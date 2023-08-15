/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

import hu.simplexion.z2.service.kotlin.ir.proto.ProtoCache
import hu.simplexion.z2.service.kotlin.ir.util.ConsumerCache
import hu.simplexion.z2.service.kotlin.ir.util.ServiceFunctionCache
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ServicePluginContext(
    val irContext: IrPluginContext
) {

    val serviceClass = SERVICE_CLASS.runtimeClass()
    val serviceType = serviceClass.defaultType
    val serviceName = serviceClass.owner.properties.first { it.name.identifier == SERVICE_NAME_PROPERTY }

    val serviceProviderType = SERVICE_PROVIDER_CLASS.runtimeClass().defaultType

    val serviceCallTransportClass = SERVICE_CALL_TRANSPORT_CLASS.runtimeClass(TRANSPORT_PACKAGE)
    val callFunction = serviceCallTransportClass.functionByName(CALL_FUNCTION)

    val defaultServiceCallTransport = checkNotNull(
        irContext
            .referenceProperties(CallableId(FqName(RUNTIME_PACKAGE),Name.identifier(DEFAULT_SERVICE_CALL_TRANSPORT)))
            .first().owner.getter?.symbol
    ) { "Missing $GLOBALS_CLASS, is the plugin added to gradle?" }

    val getService = checkNotNull(
        irContext
            .referenceFunctions(CallableId(FqName(RUNTIME_PACKAGE),Name.identifier(GET_SERVICE)))
            .first()
    ) { "Missing $GLOBALS_CLASS, is the plugin added to gradle?" }

    val uuidType = UUID.runtimeClass(UTIL_PACKAGE)

    val serviceContextType = SERVICE_CONTEXT_CLASS.runtimeClass().defaultType

    val listClass = LIST.runtimeClass(KOTLIN_COLLECTIONS)

    val serviceFunctionCache = ServiceFunctionCache()
    val protoCache = ProtoCache(this)
    val consumerCache = ConsumerCache(this)

    fun String.runtimeClass(pkg: String = RUNTIME_PACKAGE) =
        checkNotNull(irContext.referenceClass(ClassId(FqName(pkg), Name.identifier(this)))) {
            "Missing ${pkg}.$this class. Maybe the gradle dependency on \"hu.simplexion.z2:z2-service-runtime\" is missing."
        }

    @Suppress("UNUSED_PARAMETER")
    fun debug(label : String, message : () -> Any?) {
//        val paddedLabel = "[$label]".padEnd(20)
//        Files.write(Paths.get("/Users/tiz/Desktop/plugin.txt"), "$paddedLabel  ${message()}\n".encodeToByteArray(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
    }
}