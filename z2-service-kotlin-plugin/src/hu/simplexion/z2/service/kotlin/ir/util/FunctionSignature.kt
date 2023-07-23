package hu.simplexion.z2.service.kotlin.ir.util

import hu.simplexion.z2.service.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl

// TODO cache signatures
/**
 * Generate function signatures. This lets us handle functions with the
 * same name but different parameters.
 *
 * Not using the Kotlin compiler signatures as I don't know their exact
 * mechanism and having our own signature generator makes it possible
 * to do optimizations.
 */
class FunctionSignature(
    override val pluginContext: ServicePluginContext,
    val function: IrSimpleFunction
) : IrBuilder {

    fun signature(): String {
        val parts = mutableListOf<String>()
        parts += "${function.name.identifier}${SIGNATURE_DELIMITER}"
        function.valueParameters.mapTo(parts) { it.type.signature() }
        return parts.joinToString("")
    }

    fun IrType.signature(): String {
        primitive(this)?.let { return it }
        primitiveList(this)?.let { return it }
        instance(this)?.let { return it }
        instanceList(this)?.let { return it }
        return SIGNATURE_UNKNOWN
    }

    fun primitive(type: IrType): String? =
        when (type) {
            irBuiltIns.booleanType -> SIGNATURE_BOOLEAN
            irBuiltIns.intType -> SIGNATURE_INT
            irBuiltIns.longType -> SIGNATURE_LONG
            irBuiltIns.stringType -> SIGNATURE_STRING
            else -> null
        }
            ?: type.ifUuid { SIGNATURE_UUID }
            ?: type.ifByteArray { SIGNATURE_BYTEARRAY }

    fun primitiveList(type: IrType): String? {
        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        return when (itemType) {
            irBuiltIns.intType -> SIGNATURE_INT_LIST
            irBuiltIns.longType -> SIGNATURE_LONG_LIST
            irBuiltIns.stringType -> SIGNATURE_STRING_LIST
            else -> null
        }
            ?: itemType.ifBoolean { SIGNATURE_BOOLEAN_LIST }
            ?: itemType.ifUuid { SIGNATURE_UUID_LIST }
            ?: itemType.ifByteArray { SIGNATURE_BYTEARRAY_LIST }

    }

    fun instance(type: IrType): String? {
        if (pluginContext.protoCache[type]?.symbol==null) return null
        return "${SIGNATURE_INSTANCE}${type.classFqName?.asString()}${SIGNATURE_DELIMITER}"
    }

    fun instanceList(type: IrType): String? {
        if (!type.isList) return null

        // FIXME hackish list item type retrieval
        val itemType = (type as IrSimpleTypeImpl).arguments.first() as IrType

        return "${SIGNATURE_LIST}${itemType.signature()}"
    }
}