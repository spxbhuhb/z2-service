package hu.simplexion.z2.service.kotlin.ir.proto

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import hu.simplexion.z2.service.kotlin.ir.util.IrBuilder
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET

class ProtoMessageBuilderIrBuilder(
    override val pluginContext: ServicePluginContext,
    start: IrExpression? = null
) : IrBuilder {

    var valid = true

    var fieldNumber = 1

    var current: IrExpression = start ?: IrConstructorCallImpl(
        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
        pluginContext.protoMessageBuilderClass.defaultType,
        pluginContext.protoMessageBuilderConstructor,
        0, 0, 0
    )

    fun next(valueParameter: IrValueParameter) {
        next(valueParameter.type) { irGet(valueParameter) }
    }

    fun next(type: IrType, value: () -> IrExpression): IrExpression? {

        primitive(type, value)?.let { return current }
        primitiveList(type, value)?.let { return current }
        instance(type, value)?.let { return current }
        instanceList(type, value)?.let { return current }

        valid = false
        return null
    }

    fun primitive(type: IrType, value: () -> IrExpression): Boolean? {
        val classifier = type.classifierOrNull ?: return null
        val nullable = type.isNullable()

        val builderFun = when (classifier.signature) {
            IdSignatureValues._boolean -> if (nullable) pluginContext.protoBuilderBooleanOrNull else pluginContext.protoBuilderBoolean
            IdSignatureValues._int -> if (type.isNullable()) pluginContext.protoBuilderIntOrNull else pluginContext.protoBuilderInt
            IdSignatureValues._long -> if (type.isNullable()) pluginContext.protoBuilderLongOrNull else pluginContext.protoBuilderLong
            pluginContext.stringSignature -> if (type.isNullable()) pluginContext.protoBuilderStringOrNull else pluginContext.protoBuilderString
            pluginContext.uuidSignature -> if (type.isNullable()) pluginContext.protoBuilderUuidOrNull else pluginContext.protoBuilderUuid
            else -> null
        }
            ?: type.ifByteArray { if (type.isNullable()) pluginContext.protoBuilderByteArrayOrNull else pluginContext.protoBuilderByteArray }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (type.isNullable()) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index, value())
        }

        return true
    }

    fun primitiveList(type: IrType, value: () -> IrExpression): Boolean? {
        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType
        val classifier = itemType.classifierOrNull ?: return null
        check(! itemType.isNullable()) { "nullable items in lists are not supported" }

        val builderFun = when (classifier.signature) {
            IdSignatureValues._boolean -> if (type.isNullable()) pluginContext.protoBuilderBooleanListOrNull else pluginContext.protoBuilderBooleanList
            IdSignatureValues._int -> if (type.isNullable()) pluginContext.protoBuilderIntListOrNull else pluginContext.protoBuilderIntList
            IdSignatureValues._long -> if (type.isNullable()) pluginContext.protoBuilderLongListOrNull else pluginContext.protoBuilderLongList
            pluginContext.stringSignature -> if (type.isNullable()) pluginContext.protoBuilderStringListOrNull else pluginContext.protoBuilderStringList
            pluginContext.uuidSignature -> if (type.isNullable()) pluginContext.protoBuilderUuidListOrNull else pluginContext.protoBuilderUuidList
            else -> null
        }
            ?: itemType.ifByteArray { if (type.isNullable()) pluginContext.protoBuilderByteArrayListOrNull else pluginContext.protoBuilderByteArrayList }
            ?: return null

        current = irCall(
            builderFun,
            dispatchReceiver = current
        ).also {
            var index = 0
            it.putValueArgument(index ++, irConst(fieldNumber ++))
            if (type.isNullable()) it.putValueArgument(index ++, irConst(fieldNumber ++))
            it.putValueArgument(index, value())
        }

        return true
    }

    fun instance(type: IrType, value: () -> IrExpression): Boolean? {

        val encoder = pluginContext.protoCache[type]?.symbol ?: return null
        val buildFun = if (type.isNullable()) pluginContext.protoBuilderInstanceOrNull else pluginContext.protoBuilderInstance

        encode(type, value, encoder, buildFun)

        return true
    }

    fun instanceList(type: IrType, value: () -> IrExpression): Boolean? {
        if (! type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        val encoder = pluginContext.protoCache[itemType]?.symbol ?: return null
        val buildFun = if (type.isNullable()) pluginContext.protoBuilderInstanceListOrNull else pluginContext.protoBuilderInstanceList

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
}