package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.getPropertyGetter

class ProtoOneIrBuilder(
    override val pluginContext: ServicePluginContext,
) : IrBuilder {

    val protoCache = pluginContext.protoCache
    val protoEnum = pluginContext.protoEnum

    fun getDecoder(type: IrType): IrExpression? {
        primitive(type)?.let { return it }
        primitiveList(type)?.let { return it }
        enum(type)?.let { return it }
        enumList(type)?.let { return it }
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
        val decoder = pluginContext.protoCache[type]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) protoCache.protoInstance.oneOrNull.defaultType else protoCache.protoInstance.one.defaultType,
            if (type.isNullable()) protoCache.protoOneInstanceOrNullConstructor else protoCache.protoOneInstanceConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(decoder))
        }
    }

    fun instanceList(type: IrType): IrExpression? {
        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val decoder = pluginContext.protoCache[itemType]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) protoCache.protoInstance.oneListOrNull.defaultType else protoCache.protoInstance.oneList.defaultType,
            if (type.isNullable()) protoCache.protoOneInstanceListOrNullConstructor else protoCache.protoOneInstanceListConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(decoder))
        }
    }

    fun enum(type : IrType) : IrExpression? {
        if (!type.isSubtypeOfClass(protoEnum.enumClass)) return null

        val entriesGetter = (type.classifierOrNull?.owner as? IrClass)?.getPropertyGetter("entries") ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) protoEnum.enumOneOrNull.defaultType else protoEnum.enumOne.defaultType,
            if (type.isNullable()) protoEnum.enumOneOrNullConstructor else protoEnum.enumOneConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irCall(entriesGetter))
        }
    }

    fun enumList(type : IrType) : IrExpression? {
        val itemType = type.enumListType(protoEnum) ?: return null

        val entriesGetter = (itemType.classifierOrNull?.owner as? IrClass)?.getPropertyGetter("entries") ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) protoEnum.enumOneListOrNull.defaultType else protoEnum.enumOneList.defaultType,
            if (type.isNullable()) protoEnum.enumOneListOrNullConstructor else protoEnum.enumOneListConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irCall(entriesGetter))
        }
    }
}