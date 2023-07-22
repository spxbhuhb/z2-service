package hu.simplexion.z2.service.kotlin.ir.klass

import hu.simplexion.z2.service.kotlin.ir.*
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

class ProtoMessageBuilderIrBuilder(
    override val pluginContext: ServicePluginContext,
    start: IrExpression? = null
) : IrBuilder {

    var valid = true

    var current: IrExpression = start ?: IrConstructorCallImpl(
        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
        pluginContext.protoMessageBuilderClass.defaultType,
        pluginContext.protoMessageBuilderConstructor,
        0, 0, 0
    )

    fun next(valueParameter: IrValueParameter) {
        next(valueParameter.type, valueParameter.index + 1) { irGet(valueParameter) }
    }

    fun next(type: IrType, index: Int, value: () -> IrExpression): IrExpression? {

        primitive(type, index, value)?.let { return current }
        primitiveList(type, index, value)?.let { return current }
        instance(type, index, value)?.let { return current }
        instanceList(type, index, value)?.let { return current }

        valid = false
        return null
    }

    fun primitive(type: IrType, index: Int, value: () -> IrExpression): Boolean? {

        val builderFun = when (type) {
            irBuiltIns.booleanType -> pluginContext.protoBuilderBoolean
            irBuiltIns.intType -> pluginContext.protoBuilderInt
            irBuiltIns.longType -> pluginContext.protoBuilderLong
            irBuiltIns.stringType -> pluginContext.protoBuilderString
            else -> null
        }
            ?: type.ifUuid { pluginContext.protoBuilderUuid }
            ?: type.ifByteArray { pluginContext.protoBuilderByteArray }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = current
        ).also {
            it.putValueArgument(BUILDER_CALL_FIELD_NUMBER_INDEX, irConst(index))
            it.putValueArgument(BUILDER_CALL_VALUE_INDEX, value())
        }

        return true
    }

    fun primitiveList(type: IrType, index: Int, value: () -> IrExpression): Boolean? {
        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val builderFun = when (itemType) {
            irBuiltIns.intType -> pluginContext.protoBuilderIntList
            irBuiltIns.longType -> pluginContext.protoBuilderLongList
            irBuiltIns.stringType -> pluginContext.protoBuilderStringList
            else -> null
        }
            ?: itemType.ifBoolean { pluginContext.protoBuilderBooleanList }
            ?: itemType.ifUuid { pluginContext.protoBuilderUuidList }
            ?: itemType.ifByteArray { pluginContext.protoBuilderByteArrayList }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = current
        ).also {
            it.putValueArgument(BUILDER_CALL_FIELD_NUMBER_INDEX, irConst(index))
            it.putValueArgument(BUILDER_CALL_VALUE_INDEX, value())
        }

        return true
    }

    fun instance(type: IrType, index: Int, value: () -> IrExpression): Boolean? {

        val encoder = pluginContext.protoCache[type]?.symbol ?: return null

        encode(index, value, encoder, pluginContext.protoBuilderInstance)

        return true
    }

    fun instanceList(type: IrType, index: Int, value: () -> IrExpression): Boolean? {
        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null

        encode(index, value, encoder, pluginContext.protoBuilderInstanceList)

        return true
    }

    fun encode(index: Int, value: () -> IrExpression, encoder: IrClassSymbol, buildFun: IrFunctionSymbol) {
        current = irCall(
            buildFun,
            dispatchReceiver = current
        ).also {
            it.putValueArgument(BUILDER_CALL_FIELD_NUMBER_INDEX, irConst(index))
            it.putValueArgument(BUILDER_CALL_ENCODER_INDEX, irGetObject(encoder))
            it.putValueArgument(BUILDER_CALL_ENCODER_VALUE_INDEX, value())
        }
    }
}