package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.getArgument

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
            irBuiltIns.booleanType -> irGetObject(pluginContext.protoOneBoolean)
            irBuiltIns.intType -> irGetObject(pluginContext.protoOneInt)
            irBuiltIns.longType -> irGetObject(pluginContext.protoOneLong)
            irBuiltIns.stringType -> irGetObject(pluginContext.protoOneString)
            irBuiltIns.byteArray -> irGetObject(pluginContext.protoOneByteArray)
            pluginContext.uuidType -> irGetObject(pluginContext.protoOneUuid)
            else -> null
        }

    fun primitiveList(type: IrType): IrExpression? {
        if (type != irBuiltIns.listClass) return null

        return when (type.getArgument(0)) {
            irBuiltIns.booleanType -> irGetObject(pluginContext.protoOneBooleanList)
            irBuiltIns.intType -> irGetObject(pluginContext.protoOneIntList)
            irBuiltIns.longType -> irGetObject(pluginContext.protoOneLongList)
            irBuiltIns.stringType -> irGetObject(pluginContext.protoOneStringList)
            irBuiltIns.byteArray -> irGetObject(pluginContext.protoOneByteArrayList)
            pluginContext.uuidType -> irGetObject(pluginContext.protoOneUuidList)
            else -> null
        }
    }

    fun instance(type: IrType): IrExpression? {
        val encoder = pluginContext.protoCache[type]?.symbol ?: return null
        return irGetObject(encoder)
    }

    fun instanceList(type: IrType): IrExpression? {
        if (type != irBuiltIns.listClass) return null
        val encoder = pluginContext.protoCache[type.getArgument(0) as IrType]?.symbol ?: return null

        return IrConstructorCallImpl(
            SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
            pluginContext.protoOneList.defaultType,
            pluginContext.protoOneListConstructor,
            0, 0, 1
        ).also {
            it.putValueArgument(0, irGetObject(encoder))
        }
    }
}