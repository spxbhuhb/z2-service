package hu.simplexion.z2.service.kotlin.ir.util

import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.isSubclassOf

class ProtoCache(
    val pluginContext: ServicePluginContext
) {

    val services = mutableMapOf<IrType, IrClass?>()

    operator fun get(type: IrType) =
        services.getOrPut(type) { add(type) }

    fun add(type: IrType): IrClass? {

        val companion = type.getClass()?.companionObject() ?: return null

        if (!companion.isSubclassOf(pluginContext.protoEncoderClass)) return null
        if (!companion.isSubclassOf(pluginContext.protoDecoderClass)) return null

        return companion
    }

}