/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.impl

import hu.simplexion.z2.service.kotlin.ir.*
import hu.simplexion.z2.service.kotlin.ir.proto.ProtoMessageBuilderIrBuilder
import hu.simplexion.z2.service.kotlin.ir.proto.ProtoMessageIrBuilder
import hu.simplexion.z2.service.kotlin.ir.util.FunctionSignature
import hu.simplexion.z2.service.kotlin.ir.util.IrClassBaseBuilder
import hu.simplexion.z2.service.kotlin.ir.util.ServiceBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irImplicitCoercionToUnit
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.IrDeclarationBuilder
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*

class ImplClassTransform(
    override val pluginContext: ServicePluginContext
) : IrElementTransformerVoidWithContext(), ServiceBuilder, IrClassBaseBuilder {

    lateinit var transformedClass: IrClass

    lateinit var constructor: IrConstructor
    override lateinit var serviceNameGetter: IrSimpleFunctionSymbol
    lateinit var serviceContextGetter: IrSimpleFunctionSymbol

    override val overiddenServiceFunctions = mutableListOf<IrSimpleFunctionSymbol>()

    override val serviceNames = mutableListOf<String>()

    val implementedServiceFunctions = mutableListOf<ServiceFunctionEntry>()

    class ServiceFunctionEntry(
        val signature: String,
        val function: IrSimpleFunction
    )

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (::transformedClass.isInitialized) return declaration

        transformedClass = declaration
        serviceNameGetter = checkNotNull(declaration.getPropertyGetter(SERVICE_NAME_PROPERTY))
        serviceContextGetter = checkNotNull(declaration.getPropertyGetter(SERVICE_CONTEXT_PROPERTY))

        transformConstructor()
        NewInstance(pluginContext, this).build()

        collectServiceFunctions(transformedClass)

        super.visitClassNew(declaration)

        generateDispatch()

        return declaration
    }

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        when (declaration.name.identifier) {
            SERVICE_NAME_PROPERTY -> ServiceNamePropertyTransform(pluginContext, this, transformedClass, declaration).build()
            SERVICE_CONTEXT_PROPERTY -> ServiceContextPropertyTransform(pluginContext, this, declaration).build()
        }

        return declaration
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {

        val function = declaration.asServiceFun() ?: return declaration

        if (function.isFakeOverride) return declaration

        implementedServiceFunctions += ServiceFunctionEntry(
            FunctionSignature(pluginContext, function).signature(),
            function
        )

        return function
    }

    fun transformConstructor() {
        val constructors = transformedClass.constructors.toList()
        require(constructors.size == 1) { "Service implementations must have only one constructor: ${transformedClass.kotlinFqName}" }

        val newPrimary = constructor(transformedClass) // this adds an empty body
        newPrimary.addValueParameter("serviceContext".name, pluginContext.serviceContextType.makeNullable())
        constructor = newPrimary

        // replace the body of the old primary constructor - which must have no parameters -
        // with a body that calls the new primary with a null context

        val oldPrimary = constructors.first()
        require(oldPrimary.valueParameters.isEmpty()) { "Service implementation constructor must not have any parameters. ${transformedClass.kotlinFqName}" }

        oldPrimary.isPrimary = false

        oldPrimary.body = irFactory.createBlockBody(oldPrimary.startOffset, oldPrimary.endOffset).apply {
            statements += IrDelegatingConstructorCallImpl.fromSymbolOwner(
                SYNTHETIC_OFFSET,
                SYNTHETIC_OFFSET,
                transformedClass.defaultType,
                newPrimary.symbol,
                typeArgumentsCount = 0,
                valueArgumentsCount = 1
            ).also {
                it.putValueArgument(0, irNull())
            }
        }
    }

    fun generateDispatch() {
        val dispatch = checkNotNull(transformedClass.getSimpleFunction(DISPATCH_NAME)).owner
        if (! dispatch.isFakeOverride) return

        dispatch.isFakeOverride = false
        dispatch.origin = IrDeclarationOrigin.DEFINED

        dispatch.addDispatchReceiver {// replace the interface in the dispatcher with the class
            type = transformedClass.defaultType
        }

        dispatch.body = DeclarationIrBuilder(irContext, dispatch.symbol).irBlockBody {
            + irBlock(
                origin = IrStatementOrigin.WHEN
            ) {
                val funName = irTemporary(irGet(dispatch.valueParameters[DISPATCH_FUN_NAME_INDEX]))
                + irWhen(
                    irBuiltIns.unitType,
                    implementedServiceFunctions.map { dispatchBranch(dispatch, it, funName) }
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
            if (serviceFunction.function.returnType == irBuiltIns.unitType) {
                callServiceFunction(dispatch, serviceFunction.function)
            } else {
                irImplicitCoercionToUnit(
                    requireNotNull(
                        ProtoMessageBuilderIrBuilder(
                            pluginContext,
                            irGet(dispatch.valueParameters[DISPATCH_RESPONSE_INDEX])
                        ).next(
                            serviceFunction.function.returnType
                        ) { callServiceFunction(dispatch, serviceFunction.function) }
                    ) { "unsupported return type: ${serviceFunction.function.symbol}" }
                )
            }
        )

    fun IrBlockBodyBuilder.callServiceFunction(dispatch: IrSimpleFunction, serviceFunction: IrSimpleFunction): IrExpression =
        irCall(
            serviceFunction.symbol,
            dispatchReceiver = irGet(dispatch.dispatchReceiverParameter !!)
        ).also {
            val valueParameters = serviceFunction.valueParameters
            val builder = ProtoMessageIrBuilder(pluginContext) { irGet(dispatch.valueParameters[DISPATCH_PAYLOAD_INDEX]) }

            for (index in valueParameters.indices) {
                val valueParameter = valueParameters[index]

                it.putValueArgument(
                    index,
                    requireNotNull(
                        builder.get(valueParameter)
                    ) { "unsupported type argument type: ${valueParameter.symbol}" }
                )
            }
        }

    //     CONSTRUCTOR visibility:public <> () returnType:foo.bar.BasicServiceImpl [primary]
    //      BLOCK_BODY
    //        DELEGATING_CONSTRUCTOR_CALL 'public constructor <init> () [primary] declared in kotlin.Any'
    //        INSTANCE_INITIALIZER_CALL classDescriptor='CLASS CLASS name:BasicServiceImpl modality:FINAL visibility:public superTypes:[foo.bar.BasicService; hu.simplexion.z2.service.runtime.ServiceImpl]'

    //    FUN FAKE_OVERRIDE name:newInstance visibility:public modality:OPEN <> ($this:hu.simplexion.z2.service.runtime.ServiceImpl, serviceContext:hu.simplexion.z2.service.runtime.ServiceContext?) returnType:hu.simplexion.z2.service.runtime.ServiceImpl [fake_override]
    //      overridden:
    //        public open fun newInstance (serviceContext: hu.simplexion.z2.service.runtime.ServiceContext?): hu.simplexion.z2.service.runtime.ServiceImpl declared in hu.simplexion.z2.service.runtime.ServiceImpl
    //      $this: VALUE_PARAMETER name:<this> type:hu.simplexion.z2.service.runtime.ServiceImpl
    //      VALUE_PARAMETER name:serviceContext index:0 type:hu.simplexion.z2.service.runtime.ServiceContext?

    //    PROPERTY FAKE_OVERRIDE name:serviceContext visibility:public modality:OPEN [fake_override,val]
    //      overridden:
    //        public open serviceContext: hu.simplexion.z2.service.runtime.ServiceContext? [val]
    //      FUN FAKE_OVERRIDE name:<get-serviceContext> visibility:public modality:OPEN <> ($this:hu.simplexion.z2.service.runtime.ServiceImpl) returnType:hu.simplexion.z2.service.runtime.ServiceContext? [fake_override]
    //        correspondingProperty: PROPERTY FAKE_OVERRIDE name:serviceContext visibility:public modality:OPEN [fake_override,val]
    //        overridden:
    //          public open fun <get-serviceContext> (): hu.simplexion.z2.service.runtime.ServiceContext? declared in hu.simplexion.z2.service.runtime.ServiceImpl
    //        $this: VALUE_PARAMETER name:<this> type:hu.simplexion.z2.service.runtime.ServiceImpl
}
