package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.MESSAGE_CALL_DECODER_INDEX
import hu.simplexion.z2.service.kotlin.ir.MESSAGE_CALL_FIELD_NUMBER_INDEX
import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.getArgument

class ProtoMessageIrBuilder(
    override val pluginContext: ServicePluginContext,
    val dispatchReceiver : IrExpression
) : IrBuilder {

    var current: IrExpression? = null

    fun get(valueParameter: IrValueParameter): IrExpression? {
        primitive(valueParameter)?.let { return current }
        primitiveList(valueParameter)?.let { return current }
        instance(valueParameter)?.let { return current }
        instanceList(valueParameter)?.let { return current }
        return null
    }

    fun primitive(valueParameter: IrValueParameter): Boolean? {

        val builderFun = when (valueParameter.type) {
            irBuiltIns.booleanType -> pluginContext.protoMessageBoolean
            irBuiltIns.intType -> pluginContext.protoMessageInt
            irBuiltIns.longType -> pluginContext.protoMessageLong
            irBuiltIns.stringType -> pluginContext.protoMessageString
            irBuiltIns.byteArray -> pluginContext.protoMessageByteArray
            pluginContext.uuidType -> pluginContext.protoMessageUuid
            else -> null
        } ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            it.putValueArgument(MESSAGE_CALL_FIELD_NUMBER_INDEX, irConst(valueParameter.index + 1))
        }

        return true
    }

    fun primitiveList(valueParameter: IrValueParameter): Boolean? {
        if (valueParameter.type != irBuiltIns.listClass) return null

        val builderFun = when (valueParameter.type.getArgument(0)) {
            irBuiltIns.booleanType -> pluginContext.protoMessageBooleanList
            irBuiltIns.intType -> pluginContext.protoMessageIntList
            irBuiltIns.longType -> pluginContext.protoMessageLongList
            irBuiltIns.stringType -> pluginContext.protoMessageStringList
            irBuiltIns.byteArray -> pluginContext.protoMessageByteArrayList
            pluginContext.uuidType -> pluginContext.protoMessageUuidList
            else -> null
        } ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            it.putValueArgument(MESSAGE_CALL_FIELD_NUMBER_INDEX, irConst(valueParameter.index + 1))
        }

        return true
    }

    fun instance(valueParameter: IrValueParameter): Boolean? {
        val encoder = pluginContext.protoCache[valueParameter.type]?.symbol ?: return null

        decode(valueParameter, encoder, pluginContext.protoMessageInstance)

        return true
    }

    fun instanceList(valueParameter: IrValueParameter): Boolean? {
        if (valueParameter.type != irBuiltIns.listClass) return null

        val encoder = pluginContext.protoCache[valueParameter.type.getArgument(0) as IrType]?.symbol ?: return null

        decode(valueParameter, encoder, pluginContext.protoMessageList)

        return true
    }

    fun decode(valueParameter: IrValueParameter, encoder: IrClassSymbol, buildFun: IrFunctionSymbol) {
        current = irCall(
            buildFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            it.putValueArgument(MESSAGE_CALL_FIELD_NUMBER_INDEX, irConst(valueParameter.index + 1))
            it.putValueArgument(MESSAGE_CALL_DECODER_INDEX, irGetObject(encoder))
        }
    }
}