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

class ProtoDecoderIrBuilder(
    override val pluginContext: ServicePluginContext,
) : IrBuilder {

    fun getDecoder(type: IrType): IrExpression? {
        primitive(type)?.let { return it }
        primitiveList(type)?.let { return it }
        instance(type)?.let { return it }
        instanceList(type)?.let { return it }
        return null
    }

    fun primitive(type: IrType): IrExpression? =
        when (type) {
            irBuiltIns.unitType -> irGetObject(pluginContext.protoOneUnit)
            irBuiltIns.booleanType -> irGetObject(if (type.isNullable()) pluginContext.protoOneBooleanOrNull else pluginContext.protoOneBoolean)
            irBuiltIns.intType -> irGetObject(if (type.isNullable()) pluginContext.protoOneIntOrNull else pluginContext.protoOneInt)
            irBuiltIns.longType -> irGetObject(if (type.isNullable()) pluginContext.protoOneLongOrNull else pluginContext.protoOneLong)
            irBuiltIns.stringType -> irGetObject(if (type.isNullable()) pluginContext.protoOneStringOrNull else pluginContext.protoOneString)
            else -> null
        }
            ?: type.ifUuid { irGetObject(if (type.isNullable()) pluginContext.protoOneUuidOrNull else pluginContext.protoOneUuid) }
            ?: type.ifByteArray { irGetObject(if (type.isNullable()) pluginContext.protoOneByteArrayOrNull else pluginContext.protoOneByteArray) }

    fun primitiveList(type: IrType): IrExpression? {
        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        return when (itemType) {
            irBuiltIns.intType -> irGetObject(if (type.isNullable()) pluginContext.protoOneIntListOrNull else pluginContext.protoOneIntList)
            irBuiltIns.longType -> irGetObject(if (type.isNullable()) pluginContext.protoOneLongListOrNull else pluginContext.protoOneLongList)
            irBuiltIns.stringType -> irGetObject(if (type.isNullable()) pluginContext.protoOneStringListOrNull else pluginContext.protoOneStringList)
            else -> null
        }
            ?: itemType.ifBoolean { irGetObject(if (type.isNullable()) pluginContext.protoOneBooleanListOrNull else pluginContext.protoOneBooleanList) }
            ?: itemType.ifUuid { irGetObject(if (type.isNullable()) pluginContext.protoOneUuidListOrNull else pluginContext.protoOneUuidList) }
            ?: itemType.ifByteArray { irGetObject(if (type.isNullable()) pluginContext.protoOneByteArrayListOrNull else pluginContext.protoOneByteArrayList) }
    }

    fun instance(type: IrType): IrExpression? {
        val encoder = pluginContext.protoCache[type]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            if (type.isNullable()) pluginContext.protoOneInstanceOrNull.defaultType else pluginContext.protoOneInstance.defaultType,
            if (type.isNullable()) pluginContext.protoOneInstanceOrNullConstructor else pluginContext.protoOneInstanceConstructor,
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
            if (type.isNullable()) pluginContext.protoOneInstanceListOrNull.defaultType else pluginContext.protoOneInstanceList.defaultType,
            if (type.isNullable()) pluginContext.protoOneInstanceListOrNullConstructor else  pluginContext.protoOneInstanceListConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(encoder))
        }
    }
}