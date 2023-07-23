/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.*
import hu.simplexion.z2.service.kotlin.ir.util.FunctionSignature
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter


class ServiceConsumerClassTransform(
    override val pluginContext: ServicePluginContext,
) : IrElementTransformerVoidWithContext(), ServiceBuilder {

    override lateinit var transformedClass: IrClass

    override lateinit var serviceNameGetter: IrSimpleFunctionSymbol

    override val serviceFunctions = mutableListOf<IrSimpleFunctionSymbol>()

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (::transformedClass.isInitialized) return declaration

        transformedClass = declaration
        serviceNameGetter = checkNotNull(declaration.getPropertyGetter(SERVICE_NAME_PROPERTY))
        collectServiceFunctions()

        return super.visitClassNew(declaration)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        val function = declaration.asServiceFun() ?: return declaration

        function.origin = IrDeclarationOrigin.DEFINED
        function.isFakeOverride =false

        function.addDispatchReceiver { // replace the interface in the dispatcher with the class
            type = transformedClass.defaultType
        }

        declaration.body = DeclarationIrBuilder(irContext, function.symbol).irBlockBody {
            +irReturn(
                irCall(
                    pluginContext.callFunction,
                    dispatchReceiver = getServiceTransport()
                ).also {
                    it.type = function.returnType
                    it.putTypeArgument(CALL_TYPE_INDEX, function.returnType)
                    it.putValueArgument(CALL_SERVICE_NAME_INDEX, getServiceName(function))
                    it.putValueArgument(CALL_FUN_NAME_INDEX, irConst(FunctionSignature(pluginContext, function).signature()))
                    it.putValueArgument(CALL_PAYLOAD_INDEX, buildPayload(function))
                    it.putValueArgument(CALL_DECODER_INDEX, ProtoDecoderIrBuilder(pluginContext).getDecoder(function.returnType))
                }
            )
        }

        return super.visitFunctionNew(function)
    }


    fun getServiceTransport(): IrCallImpl =
        irCall(
            pluginContext.defaultServiceCallTransport,
            IrStatementOrigin.GET_PROPERTY
        )

    fun buildPayload(function: IrSimpleFunction): IrExpression {
        val protoBuilder = ProtoMessageBuilderIrBuilder(pluginContext)

        for (valueParameter in function.valueParameters) {
            protoBuilder.next(valueParameter)
            check(protoBuilder.valid) { "unsupported type: ${valueParameter.symbol} function: ${function.symbol}" }
        }

        return irCall(
            pluginContext.protoBuilderPack,
            dispatchReceiver = protoBuilder.current
        )
    }

}
