package hu.simplexion.z2.service.kotlin.ir.util

import hu.simplexion.z2.service.kotlin.ir.COMPANION_OBJECT_NAME
import hu.simplexion.z2.service.kotlin.ir.ServicePluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class ConsumerCache(
    val pluginContext: ServicePluginContext
) {

    val consumers = mutableMapOf<IrType, IrClass>()

    operator fun get(type: IrType) =
        consumers.getOrPut(type) { add(type) }

    fun add(type: IrType): IrClass {
        val typeFqName = checkNotNull(type.classFqName)
        val classId = ClassId(typeFqName.parent(), typeFqName.shortName()).createNestedClassId(Name.identifier(COMPANION_OBJECT_NAME))
        return checkNotNull(pluginContext.irContext.referenceClass(classId)).owner
    }

    fun add(type : IrType, consumer: IrClass) {
        consumers[type] = consumer
    }

}