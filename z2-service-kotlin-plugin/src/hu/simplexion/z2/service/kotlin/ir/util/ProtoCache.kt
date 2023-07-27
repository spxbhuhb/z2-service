package hu.simplexion.z2.service.kotlin.ir.util

import hu.simplexion.z2.service.kotlin.ir.COMPANION_OBJECT_NAME
import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class ProtoCache(
    val pluginContext: ServicePluginContext
) {

    val protoCoders = mutableMapOf<IrType, IrClass?>()

    operator fun get(type: IrType) =
        protoCoders.getOrPut(type) { add(type) }

    fun add(type: IrType): IrClass? {

        val companion = type.getClass()?.companionObject() ?: tryLoadCompanion(type) ?: return null

        if (!companion.isSubclassOf(pluginContext.protoEncoderClass)) return null
        if (!companion.isSubclassOf(pluginContext.protoDecoderClass)) return null

        pluginContext.debug("service") { "protoCache add $type $companion"}
        return companion
    }

    fun add(type : IrType, companion : IrClass) {
        pluginContext.debug("service") { "protoCache add $type $companion"}
        protoCoders[type] = companion
    }

    private fun tryLoadCompanion(type: IrType): IrClass? {
        val typeFqName = type.classFqName ?: return null
        val classId = ClassId(typeFqName.parent(), typeFqName.shortName()).createNestedClassId(Name.identifier(COMPANION_OBJECT_NAME))
        return pluginContext.irContext.referenceClass(classId)?.owner
    }

}