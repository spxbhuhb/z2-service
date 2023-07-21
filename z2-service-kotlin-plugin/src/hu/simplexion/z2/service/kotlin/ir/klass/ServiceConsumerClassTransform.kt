/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.*
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
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
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


class ServiceConsumerClassTransform(
    override val pluginContext: ServicePluginContext,
) : IrElementTransformerVoidWithContext(), IrBuilder {

    lateinit var transformedClass: IrClass
    lateinit var serviceNameGetter: IrSimpleFunctionSymbol

    val serviceTypes = mutableListOf<IrType>()

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (::transformedClass.isInitialized) return declaration

        transformedClass = declaration
        serviceNameGetter = checkNotNull(declaration.getPropertyGetter(SERVICE_NAME_PROPERTY))

        for (superType in declaration.superTypes) {
            if (superType.isSubtypeOfClass(pluginContext.serviceClass)) {
                serviceTypes += superType
            }
        }

        return super.visitClassNew(declaration)
    }

    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        if (!isServiceFun(declaration)) return declaration

        declaration.origin = IrDeclarationOrigin.DEFINED

        declaration.body = DeclarationIrBuilder(irContext, declaration.symbol).irBlockBody {
            +irReturn(
                irCall(
                    pluginContext.callFunction,
                    dispatchReceiver = getServiceTransport()
                ).also {
                    it.putTypeArgument(CALL_TYPE_INDEX, declaration.returnType)
                    it.putValueArgument(CALL_SERVICE_NAME_INDEX, getServiceName())
                    it.putValueArgument(CALL_FUN_NAME_INDEX, irConst(declaration.name.identifier))
                    it.putValueArgument(CALL_PAYLOAD_INDEX, buildPayload(declaration))
                    it.putValueArgument(CALL_DECODER_INDEX, getDecoder(declaration.returnType))
                }
            )
        }

        return super.visitFunctionNew(declaration)
    }

    @OptIn(ExperimentalContracts::class)
    fun isServiceFun(declaration: IrFunction): Boolean {
        contract {
            returns(true) implies (declaration is IrSimpleFunction)
        }
        if (declaration !is IrSimpleFunction) return false
        if (!declaration.isFakeOverride) return false
        for (overriddenSymbol in declaration.overriddenSymbols) {
            val parent = overriddenSymbol.owner.parent
            if (parent !is IrClass) continue
            if (parent.defaultType in serviceTypes) return true
        }
        return false
    }

    fun getServiceTransport(): IrCallImpl = irCall(pluginContext.defaultServiceCallTransport)

    fun getServiceName(): IrCallImpl = irCall(
        serviceNameGetter,
        dispatchReceiver = irGet(checkNotNull(transformedClass.thisReceiver))
    )

    fun buildPayload(function: IrSimpleFunction): IrExpression {
        var current: IrExpression = IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            pluginContext.protoMessageBuilderClass.defaultType,
            pluginContext.protoMessageBuilderConstructor,
            0, 0, 0
        )

        for (valueParameter in function.valueParameters) {
            val builderFun = when (valueParameter.type) {
                irBuiltIns.booleanType -> pluginContext.protoBuilderBoolean
                irBuiltIns.intType -> pluginContext.protoBuilderInt
                irBuiltIns.longType -> pluginContext.protoBuilderLong
                irBuiltIns.stringType -> pluginContext.protoBuilderString
                irBuiltIns.byteArray -> pluginContext.protoBuilderByteArray
                pluginContext.uuidType -> pluginContext.protoBuilderUuid
                else -> throw IllegalArgumentException("invalid parameter type: ${valueParameter.type}")
            }

            current = irCall(
                builderFun,
                dispatchReceiver = current
            ).also {
                it.putValueArgument(BUILDER_CALL_FIELD_NUMBER_INDEX, irConst(valueParameter.index))
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
            else -> throw IllegalArgumentException("unsupported return type: $type")
        }

}
