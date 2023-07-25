/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.*
import hu.simplexion.z2.service.kotlin.ir.util.FunctionSignature
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irImplicitCoercionToUnit
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

class ServiceProviderClassTransform(
    override val pluginContext: ServicePluginContext
) : IrElementTransformerVoidWithContext(), ServiceBuilder {

    override lateinit var transformedClass: IrClass

    override lateinit var serviceNameGetter: IrSimpleFunctionSymbol
    lateinit var serviceContextGetter: IrSimpleFunctionSymbol

    override val serviceFunctions = mutableListOf<IrSimpleFunctionSymbol>()

    override val serviceNames = mutableListOf<String>()

    val contextFunctions = mutableListOf<ServiceFunctionEntry>()

    val contextLessFunctions = mutableListOf<IrSimpleFunction>()

    class ServiceFunctionEntry(
        val signature : String,
        val function : IrSimpleFunction
    )

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (::transformedClass.isInitialized) return declaration

        transformedClass = declaration
        serviceNameGetter = checkNotNull(declaration.getPropertyGetter(SERVICE_NAME_PROPERTY))
        serviceContextGetter = checkNotNull(declaration.getPropertyGetter(SERVICE_CONTEXT_PROPERTY))

        collectServiceFunctions()

        super.visitClassNew(declaration)

        transformedClass.declarations += contextLessFunctions

        generateDispatch()

        return declaration
    }

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        if (declaration.name.identifier != SERVICE_NAME_PROPERTY) return declaration
        return declaration.accept(ServiceNamePropertyTransform(pluginContext, this), null) as IrStatement
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val function = declaration.asServiceFun() ?: return declaration

        if (function.isFakeOverride) replaceFakeOverrideWithThrow(function)

        val contextLess = contextLessDeclaration(function)

        function.overriddenSymbols = emptyList()
        function.addValueParameter(Name.identifier(SERVICE_CONTEXT_ARG_NAME), pluginContext.serviceContextType)

        addContextLessBody(contextLess, function)

        function.accept(ServiceContextTransform(pluginContext, function, serviceContextGetter), null)

        contextFunctions += ServiceFunctionEntry(
            FunctionSignature(pluginContext, contextLess).signature(),
            function
        )

        return function
    }

    fun IrSimpleFunction.setDispatchReceiver() {
        val thisReceiver = transformedClass.thisReceiver!!
        dispatchReceiverParameter = thisReceiver.copyTo(this, type = thisReceiver.type)
    }

    fun replaceFakeOverrideWithThrow(function: IrSimpleFunction) {
        function.isFakeOverride = false
        function.origin = IrDeclarationOrigin.DEFINED
        function.body = DeclarationIrBuilder(irContext, function.symbol).irBlockBody {
            +irThrow(
                IrConstructorCallImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    pluginContext.notImplementedErrorClass.defaultType,
                    pluginContext.notImplementedErrorClass.constructors.first { it.owner.isPrimary },
                    0, 0, 0
                )
            )
        }
    }

    fun contextLessDeclaration(original: IrSimpleFunction): IrSimpleFunction {
        val function = irFactory.buildFun {
            startOffset = original.startOffset
            endOffset = original.endOffset
            name = original.name
            returnType = original.returnType
            modality = original.modality
            visibility = original.visibility
            isSuspend = original.isSuspend
            isFakeOverride = false
            isInline = false
            origin = original.origin
        }.apply {

            parent = transformedClass
            setDispatchReceiver()
            overriddenSymbols = original.overriddenSymbols

            for (typeParameter in original.typeParameters) {
                addTypeParameter {
                    updateFrom(typeParameter)
                    superTypes += typeParameter.superTypes // FIXME use type parameter mapper somehow... check addTypeParameter source
                }
            }

            for (valueParameter in original.valueParameters) {
                addValueParameter {
                    name = valueParameter.name
                    updateFrom(valueParameter)
                }
            }
        }

        contextLessFunctions += function

        return function
    }

    fun addContextLessBody(function: IrSimpleFunction, original: IrSimpleFunction) {
        function.body = DeclarationIrBuilder(irContext, function.symbol).irBlockBody {
            +irReturn(
                irCall(
                    original.symbol,
                    dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                ).apply {
                    for (valueParameter in function.valueParameters) {
                        putValueArgument(valueParameter.index, irGet(valueParameter))
                    }
                    putValueArgument(function.valueParameters.size, irNull())
                }
            )
        }
    }

    fun generateDispatch() {
        val dispatch = checkNotNull(transformedClass.getSimpleFunction(DISPATCH_NAME)).owner
        if (!dispatch.isFakeOverride) return

        dispatch.isFakeOverride = false
        dispatch.origin = IrDeclarationOrigin.DEFINED

        dispatch.addDispatchReceiver {// replace the interface in the dispatcher with the class
            type = transformedClass.defaultType
        }

        dispatch.body = DeclarationIrBuilder(irContext, dispatch.symbol).irBlockBody {
            +irBlock(
                origin = IrStatementOrigin.WHEN
            ) {
                val funName = irTemporary(irGet(dispatch.valueParameters[DISPATCH_FUN_NAME_INDEX]))
                +irWhen(
                    irBuiltIns.unitType,
                    contextFunctions.map { dispatchBranch(dispatch, it, funName) }
                )
            }
        }
    }

    fun IrBlockBodyBuilder.dispatchBranch(dispatch: IrSimpleFunction, serviceFunction: ServiceFunctionEntry, funName: IrVariable): IrBranch =
        irBranch(
            irEquals(
                irGet(funName),
                irConst(serviceFunction.signature),
                IrStatementOrigin.EQEQ
            ),
            if ( serviceFunction.function.returnType == irBuiltIns.unitType) {
                callServiceFunction(dispatch, serviceFunction.function)
            } else {
                irImplicitCoercionToUnit(
                    requireNotNull(
                        ProtoMessageBuilderIrBuilder(
                            pluginContext,
                            irGet(dispatch.valueParameters[DISPATCH_RESPONSE_INDEX])
                        ).next(
                            serviceFunction.function.returnType,
                            1
                        ) { callServiceFunction(dispatch, serviceFunction.function) }
                    ) { "unsupported type return type: ${serviceFunction.function.symbol}" }
                )
            }
        )

    fun IrBlockBodyBuilder.callServiceFunction(dispatch: IrSimpleFunction, serviceFunction: IrSimpleFunction): IrExpression =
        irCall(
            serviceFunction.symbol,
            dispatchReceiver = irGet(dispatch.dispatchReceiverParameter!!)
        ).also {
            val valueParameters = serviceFunction.valueParameters

            for (index in 0 until valueParameters.size - 1) { // last parameter is the context
                val valueParameter = valueParameters[index]

                it.putValueArgument(
                    index,
                    requireNotNull(
                        ProtoMessageIrBuilder(
                            pluginContext,
                            irGet(dispatch.valueParameters[DISPATCH_PAYLOAD_INDEX])
                        ).get(valueParameter)
                    ) { "unsupported type argument type: ${valueParameter.symbol}" }
                )
            }

            it.putValueArgument(serviceFunction.valueParameters.size - 1, irGet(dispatch.valueParameters[DISPATCH_CONTEXT_INDEX]))
        }

}
