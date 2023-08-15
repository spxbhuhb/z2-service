package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isSubtypeOfClass
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

class ProtoMessageBuilderIrBuilder(
    override val pluginContext: ServicePluginContext,
    start: IrExpression? = null
) : IrBuilder {

    var valid = true

    var fieldNumber = 1

    val protoCache = pluginContext.protoCache
    val protoEnum = pluginContext.protoEnum

    var current: IrExpression = start ?: IrConstructorCallImpl(
        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
        protoCache.protoMessageBuilderClass.defaultType,
        protoCache.protoMessageBuilderConstructor,
        0, 0, 0
    )

    fun next(valueParameter: IrValueParameter) {
        next(valueParameter.type) { irGet(valueParameter) }
    }

    fun next(type: IrType, value: () -> IrExpression): IrExpression? {

        primitive(type, value)?.let { return current }
        primitiveList(type, value)?.let { return current }
        enum(type, value)?.let { return current }
        enumList(type, value)?.let { return current }
        instance(type, value)?.let { return current }
        instanceList(type, value)?.let { return current }

        valid = false
        return null
    }

    fun primitive(type: IrType, value: () -> IrExpression): Boolean? {
        val builtInEntry = protoCache.primitive(type) ?: return null
        val nullable = type.isNullable()

        current = irCall(
            if (nullable) builtInEntry.encodeOrNull else builtInEntry.encode,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (nullable) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index, value())
        }

        return true
    }

    fun primitiveList(type: IrType, value: () -> IrExpression): Boolean? {

        val builtInEntry = protoCache.list(type) ?: return null
        val nullable = type.isNullable()

        current = irCall(
            if (nullable) builtInEntry.encodeListOrNull else builtInEntry.encodeList,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (nullable) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index, value())
        }

        return true
    }

    fun instance(type: IrType, value: () -> IrExpression): Boolean? {

        val encoder = pluginContext.protoCache[type]?.symbol ?: return null
        val buildFun = if (type.isNullable()) protoCache.protoInstance.encodeOrNull else protoCache.protoInstance.encode

        encode(type, value, encoder, buildFun)

        return true
    }

    fun instanceList(type: IrType, value: () -> IrExpression): Boolean? {
        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null
        val buildFun = if (type.isNullable()) protoCache.protoInstance.encodeListOrNull else protoCache.protoInstance.encodeList

        encode(type, value, encoder, buildFun)

        return true
    }

    fun encode(type: IrType, value: () -> IrExpression, encoder: IrClassSymbol, buildFun: IrFunctionSymbol) {
        current = irCall(
            buildFun,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (type.isNullable()) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index ++, irGetObject(encoder))
            it.putValueArgument(index, value())
        }
    }

    fun enum(type : IrType, value: () -> IrExpression) : Boolean? {
        if (!type.isSubtypeOfClass(protoEnum.enumClass)) return null

        val nullable = type.isNullable()

        current = irCall(
            if (nullable) protoCache.protoInt.encodeOrNull else protoCache.protoInt.encode,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (nullable) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index,
                irCall(
                    protoEnum.enumOrNullToOrdinal,
                ).also { c ->
                    c.putValueArgument(0, value())
                }
            )
        }

        return true
    }

    fun enumList(type : IrType, value: () -> IrExpression) : Boolean? {
        type.enumListType(protoEnum) ?: return null

        val nullable = type.isNullable()

        current = irCall(
            if (nullable) protoCache.protoInt.encodeListOrNull else protoCache.protoInt.encodeList,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (nullable) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index,
                irCall(
                    if (nullable) protoEnum.enumListOrNullToOrdinals else protoEnum.enumListToOrdinals
                ).also { c ->
                    c.putValueArgument(0, value())
                }
            )
        }

        return true
    }
}