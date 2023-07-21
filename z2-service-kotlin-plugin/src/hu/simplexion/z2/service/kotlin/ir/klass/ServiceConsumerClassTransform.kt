/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.*
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.addDispatchReceiver
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
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
                    it.putValueArgument(CALL_FUN_NAME_INDEX, irConst(function.name.identifier))
                    it.putValueArgument(CALL_PAYLOAD_INDEX, buildPayload(function))
                    it.putValueArgument(CALL_DECODER_INDEX, getDecoder(function.returnType))
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
        var current: IrExpression = IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            pluginContext.protoMessageBuilderClass.defaultType,
            pluginContext.protoMessageBuilderConstructor,
            0, 0, 0
        )

        for (valueParameter in function.valueParameters) {

            current = irCall(
                requireNotNull(valueParameter.type.protoBuilderFun())  { "unsupported type: ${valueParameter.symbol} function: ${function.symbol}" },
                dispatchReceiver = current
            ).also {
                it.putValueArgument(BUILDER_CALL_FIELD_NUMBER_INDEX, irConst(valueParameter.index + 1))
                it.putValueArgument(BUILDER_CALL_VALUE_INDEX, irGet(valueParameter))
            }
        }

        return irCall(
            pluginContext.protoBuilderPack,
            dispatchReceiver = current
        )
    }

    fun IrBlockBodyBuilder.getDecoder(type: IrType): IrExpression =
        when (type) {
            irBuiltIns.booleanType -> irGetObject(pluginContext.protoOneBoolean)
            irBuiltIns.intType -> irGetObject(pluginContext.protoOneInt)
            irBuiltIns.longType -> irGetObject(pluginContext.protoOneLong)
            irBuiltIns.stringType -> irGetObject(pluginContext.protoOneString)
            irBuiltIns.byteArray -> irGetObject(pluginContext.protoOneByteArray)
            pluginContext.uuidType -> irGetObject(pluginContext.protoOneUuid)
            else -> throw IllegalArgumentException("unsupported return type: ${type.classFqName}")
        }

}
