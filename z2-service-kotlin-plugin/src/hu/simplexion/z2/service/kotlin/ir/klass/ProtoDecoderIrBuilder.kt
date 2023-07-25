package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
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
            irBuiltIns.booleanType -> irGetObject(pluginContext.protoOneBoolean)
            irBuiltIns.intType -> irGetObject(pluginContext.protoOneInt)
            irBuiltIns.longType -> irGetObject(pluginContext.protoOneLong)
            irBuiltIns.stringType -> irGetObject(pluginContext.protoOneString)
            else -> null
        }
            ?: type.ifUuid { irGetObject(pluginContext.protoOneUuid) }
            ?: type.ifByteArray { irGetObject(pluginContext.protoOneByteArray) }

    fun primitiveList(type: IrType): IrExpression? {
        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        return when (itemType) {
            irBuiltIns.intType -> irGetObject(pluginContext.protoOneIntList)
            irBuiltIns.longType -> irGetObject(pluginContext.protoOneLongList)
            irBuiltIns.stringType -> irGetObject(pluginContext.protoOneStringList)
            else -> null
        }
            ?: itemType.ifBoolean { irGetObject(pluginContext.protoOneBooleanList) }
            ?: itemType.ifUuid { irGetObject(pluginContext.protoOneUuidList) }
            ?: itemType.ifByteArray { irGetObject(pluginContext.protoOneByteArrayList) }
    }

    fun instance(type: IrType): IrExpression? {
        val encoder = pluginContext.protoCache[type]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            pluginContext.protoOneInstance.defaultType,
            pluginContext.protoOneInstanceConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(encoder))
        }
    }

    fun instanceList(type: IrType): IrExpression? {
        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            pluginContext.protoOneInstanceList.defaultType,
            pluginContext.protoOneInstanceListConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(encoder))
        }
    }
}