package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.MESSAGE_CALL_DECODER_INDEX
import hu.simplexion.z2.service.kotlin.ir.MESSAGE_CALL_FIELD_NUMBER_INDEX
import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl

class ProtoMessageIrBuilder(
    override val pluginContext: ServicePluginContext,
    val dispatchReceiver: IrExpression
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
            else -> null
        }
            ?: valueParameter.type.ifUuid { pluginContext.protoMessageUuid }
            ?: valueParameter.type.ifByteArray { pluginContext.protoMessageByteArray }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            it.putValueArgument(MESSAGE_CALL_FIELD_NUMBER_INDEX, irConst(valueParameter.index + 1))
        }

        return true
    }

    fun primitiveList(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val builderFun = when (itemType) {
            irBuiltIns.intType -> pluginContext.protoMessageIntList
            irBuiltIns.longType -> pluginContext.protoMessageLongList
            irBuiltIns.stringType -> pluginContext.protoMessageStringList
            else -> null
        }
            ?: itemType.ifBoolean { pluginContext.protoMessageBooleanList }
            ?: itemType.ifUuid { pluginContext.protoMessageUuidList }
            ?: itemType.ifByteArray { pluginContext.protoMessageByteArrayList }
            ?: return null

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
        val type = valueParameter.type

        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null

        decode(valueParameter, encoder, pluginContext.protoMessageInstanceList)

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