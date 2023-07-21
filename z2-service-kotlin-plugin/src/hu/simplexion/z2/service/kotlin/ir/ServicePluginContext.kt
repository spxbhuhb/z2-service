/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ServicePluginContext(
    val irContext: IrPluginContext,
) {

    val serviceClass = SERVICE_CLASS.runtimeClass()
    val serviceProviderType = SERVICE_PROVIDER_CLASS.runtimeClass().defaultType
    val serviceConsumerType = SERVICE_CONSUMER_CLASS.runtimeClass().defaultType

    val serviceCallTransportClass = SERVICE_CALL_TRANSPORT_CLASS.runtimeClass()
    val callFunction = serviceCallTransportClass.functionByName(CALL_FUNCTION)

    val defaultServiceCallTransport = checkNotNull(
        GLOBALS_CLASS.runtimeClass(TRANSPORT_PACKAGE).getPropertyGetter(DEFAULT_SERVICE_CALL_TRANSPORT)
    ) { "Missing $GLOBALS_CLASS, is the plugin added to gradle?" }

    val protoMessageBuilderClass = PROTO_MESSAGE_BUILDER.runtimeClass(PROTO_PACKAGE)
    val protoMessageBuilderConstructor = protoMessageBuilderClass.constructors.first()
    val protoBuilderPack = protoMessageBuilderClass.functionByName(PROTO_BUILDER_PACK)
    val protoBuilderBoolean = protoMessageBuilderClass.functionByName(PROTO_BUILDER_BOOLEAN)
    val protoBuilderInt = protoMessageBuilderClass.functionByName(PROTO_BUILDER_INT)
    val protoBuilderLong = protoMessageBuilderClass.functionByName(PROTO_BUILDER_LONG)
    val protoBuilderString = protoMessageBuilderClass.functionByName(PROTO_BUILDER_STRING)
    val protoBuilderByteArray = protoMessageBuilderClass.functionByName(PROTO_BUILDER_BYTEARRAY)
    val protoBuilderUuid = protoMessageBuilderClass.functionByName(PROTO_BUILDER_UUID)

    val uuidType = UUID.runtimeClass(UTIL_PACKAGE).defaultType

    val protoOneBoolean = PROTO_ONE_BOOLEAN.runtimeClass(PROTO_PACKAGE)
    val protoOneInt = PROTO_ONE_INT.runtimeClass(PROTO_PACKAGE)
    val protoOneLong = PROTO_ONE_LONG.runtimeClass(PROTO_PACKAGE)
    val protoOneString = PROTO_ONE_STRING.runtimeClass(PROTO_PACKAGE)
    val protoOneByteArray = PROTO_ONE_BYTEARRAY.runtimeClass(PROTO_PACKAGE)
    val protoOneUuid = PROTO_ONE_UUID.runtimeClass(PROTO_PACKAGE)

    val typeSystem = IrTypeSystemContextImpl(irContext.irBuiltIns)

    fun String.runtimeClass(pkg: String? = null) =
        checkNotNull(irContext.referenceClass(ClassId(FqName(pkg ?: RUNTIME_PACKAGE), Name.identifier(this)))) {
            "Missing runtime class. Maybe the gradle dependency on \"hu.simplexion.z2:z2-service-runtime\" is missing."
        }

}