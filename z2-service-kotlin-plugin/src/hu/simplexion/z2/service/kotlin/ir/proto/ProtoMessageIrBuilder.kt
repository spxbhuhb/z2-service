package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNullable

class ProtoMessageIrBuilder(
    override val pluginContext: ServicePluginContext,
    val dispatchReceiver: IrExpression
) : IrBuilder {

    var current: IrExpression? = null
    var fieldNumber = 1

    fun get(valueParameter: IrValueParameter): IrExpression? {
        primitive(valueParameter)?.let { return current }
        primitiveList(valueParameter)?.let { return current }
        instance(valueParameter)?.let { return current }
        instanceList(valueParameter)?.let { return current }
        return null
    }

    fun primitive(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        val builderFun = when (type) {
            irBuiltIns.booleanType -> if (type.isNullable()) pluginContext.protoMessageBooleanOrNull else pluginContext.protoMessageBoolean
            irBuiltIns.intType -> if (type.isNullable()) pluginContext.protoMessageIntOrNull else pluginContext.protoMessageInt
            irBuiltIns.longType -> if (type.isNullable()) pluginContext.protoMessageLongOrNull else pluginContext.protoMessageLong
            irBuiltIns.stringType -> if (type.isNullable()) pluginContext.protoMessageStringOrNull else pluginContext.protoMessageString
            else -> null
        }
            ?: valueParameter.type.ifUuid { if (type.isNullable()) pluginContext.protoMessageUuidOrNull else pluginContext.protoMessageUuid }
            ?: valueParameter.type.ifByteArray { if (type.isNullable()) pluginContext.protoMessageByteArrayOrNull else pluginContext.protoMessageByteArray }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            it.putValueArgument(0, irConst(fieldNumber ++))
            if (type.isNullable()) it.putValueArgument(1, irConst(fieldNumber ++))
        }

        return true
    }

    fun primitiveList(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val builderFun = when (itemType) {
            irBuiltIns.intType -> if (type.isNullable()) pluginContext.protoMessageIntListOrNull else pluginContext.protoMessageIntList
            irBuiltIns.longType -> if (type.isNullable()) pluginContext.protoMessageLongListOrNull else pluginContext.protoMessageLongList
            irBuiltIns.stringType -> if (type.isNullable()) pluginContext.protoMessageStringListOrNull else pluginContext.protoMessageStringList
            else -> null
        }
            ?: itemType.ifBoolean { if (type.isNullable()) pluginContext.protoMessageBooleanListOrNull else pluginContext.protoMessageBooleanList }
            ?: itemType.ifUuid { if (type.isNullable()) pluginContext.protoMessageUuidListOrNull else pluginContext.protoMessageUuidList }
            ?: itemType.ifByteArray { if (type.isNullable()) pluginContext.protoMessageByteArrayListOrNull else pluginContext.protoMessageByteArrayList }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            it.putValueArgument(0, irConst(fieldNumber ++))
            if (type.isNullable()) it.putValueArgument(1, irConst(fieldNumber ++))
        }

        return true
    }

    fun instance(valueParameter: IrValueParameter): Boolean? {
        val encoder = pluginContext.protoCache[valueParameter.type]?.symbol ?: return null
        val buildFun = if (valueParameter.type.isNullable()) pluginContext.protoMessageInstanceOrNull else pluginContext.protoMessageInstance

        decode(valueParameter, encoder, buildFun)

        return true
    }

    fun instanceList(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null
        val buildFun = if (type.isNullable()) pluginContext.protoMessageInstanceListOrNull else pluginContext.protoMessageInstanceList

        decode(valueParameter, encoder, buildFun)

        return true
    }

    fun decode(valueParameter: IrValueParameter, encoder: IrClassSymbol, buildFun: IrFunctionSymbol) {
        current = irCall(
            buildFun,
            dispatchReceiver = dispatchReceiver
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (valueParameter.type.isNullable()) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index, irGetObject(encoder))
        }
    }
}