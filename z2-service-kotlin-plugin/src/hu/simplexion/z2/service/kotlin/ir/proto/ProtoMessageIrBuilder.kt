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
    val dispatchReceiver: () -> IrExpression
) : IrBuilder {

    var current: IrExpression? = null

    var fieldNumber = 1

    val protoCache = pluginContext.protoCache

    fun get(valueParameter: IrValueParameter): IrExpression? {
        primitive(valueParameter)?.let { return current }
        primitiveList(valueParameter)?.let { return current }
        instance(valueParameter)?.let { return current }
        instanceList(valueParameter)?.let { return current }
        return null
    }

    fun primitive(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        val nullable = type.isNullable()
        val builtInEntry = protoCache.primitive(type) ?: return null

        current = irCall(
            if (nullable) builtInEntry.decodeOrNull else builtInEntry.decode,
            dispatchReceiver = dispatchReceiver()
        ).also {
            it.putValueArgument(0, irConst(fieldNumber ++))
            if (type.isNullable()) it.putValueArgument(1, irConst(fieldNumber ++))
        }

        return true
    }

    fun primitiveList(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        val builtInEntry = protoCache.list(type) ?: return null
        val nullable = type.isNullable()

        current = irCall(
            if (nullable) builtInEntry.decodeListOrNull else builtInEntry.decodeList,
            dispatchReceiver = dispatchReceiver()
        ).also {
            it.putValueArgument(0, irConst(fieldNumber ++))
            if (nullable) it.putValueArgument(1, irConst(fieldNumber ++))
        }

        return true
    }

    fun instance(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type
        val encoder = pluginContext.protoCache[valueParameter.type]?.symbol ?: return null
        val buildFun = if (type.isNullable()) protoCache.protoInstance.decodeOrNull else protoCache.protoInstance.decode

        decode(valueParameter, encoder, buildFun)

        return true
    }

    fun instanceList(valueParameter: IrValueParameter): Boolean? {
        val type = valueParameter.type

        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null
        val buildFun = if (type.isNullable()) protoCache.protoInstance.decodeListOrNull else protoCache.protoInstance.decodeList

        decode(valueParameter, encoder, buildFun)

        return true
    }

    fun decode(valueParameter: IrValueParameter, encoder: IrClassSymbol, buildFun: IrFunctionSymbol) {
        current = irCall(
            buildFun,
            dispatchReceiver = dispatchReceiver()
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (valueParameter.type.isNullable()) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index, irGetObject(encoder))
        }
    }
}