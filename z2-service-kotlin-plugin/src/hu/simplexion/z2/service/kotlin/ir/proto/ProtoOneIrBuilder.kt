package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

class ProtoOneIrBuilder(
    override val pluginContext: ServicePluginContext,
) : IrBuilder {

    val protoCache = pluginContext.protoCache

    fun getDecoder(type: IrType): IrExpression? {
        primitive(type)?.let { return it }
        primitiveList(type)?.let { return it }
        instance(type)?.let { return it }
        instanceList(type)?.let { return it }
        return null
    }

    fun primitive(type: IrType): IrExpression? {
        protoCache.primitive(type)?.let { return irGetObject(if (type.isNullable()) it.oneOrNull else it.one) }
        if (type == irBuiltIns.unitType) return irGetObject(protoCache.protoOneUnit)
        return null
    }

    fun primitiveList(type: IrType): IrExpression? =
        protoCache.list(type)?.let { irGetObject(if (type.isNullable()) it.oneListOrNull else it.oneList) }

    fun instance(type: IrType): IrExpression? {
        val encoder = pluginContext.protoCache[type]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) protoCache.protoInstance.oneOrNull.defaultType else protoCache.protoInstance.one.defaultType,
            if (type.isNullable()) protoCache.protoOneInstanceOrNullConstructor else protoCache.protoOneInstanceConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(encoder))
        }
    }

    fun instanceList(type: IrType): IrExpression? {
        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) protoCache.protoInstance.oneListOrNull.defaultType else protoCache.protoInstance.oneList.defaultType,
            if (type.isNullable()) protoCache.protoOneInstanceListOrNullConstructor else protoCache.protoOneInstanceListConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(encoder))
        }
    }
}