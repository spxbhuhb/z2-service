/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.SERVICE_CONTEXT_ARG_NAME
import hu.simplexion.z2.service.kotlin.ir.SERVICE_CONTEXT_PROPERTY
import hu.simplexion.z2.service.kotlin.ir.SERVICE_NAME_PROPERTY
import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addTypeParameter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.name.Name

class ServiceProviderClassTransform(
    override val pluginContext: ServicePluginContext
) : IrElementTransformerVoidWithContext(), ServiceBuilder {

    override lateinit var transformedClass: IrClass

    override lateinit var serviceNameGetter: IrSimpleFunctionSymbol
    lateinit var serviceContextGetter: IrSimpleFunctionSymbol

    override val serviceFunctions = mutableListOf<IrSimpleFunctionSymbol>()

    val contextLessFunctions = mutableListOf<IrSimpleFunction>()

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

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val function = declaration.asServiceFun() ?: return declaration

        if (function.isFakeOverride) replaceFakeOverrideWithThrow(function)

        val contextLess = contextLessDeclaration(function)

        function.overriddenSymbols = emptyList()
        function.addValueParameter(Name.identifier(SERVICE_CONTEXT_ARG_NAME), pluginContext.serviceContextType)

        addContextLessBody(contextLess, function)

        function.accept(ServiceContextTransform(pluginContext, function, serviceContextGetter), null)

        return function
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
            addDispatchReceiver {
                type = transformedClass.defaultType
            }

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

    }
}
