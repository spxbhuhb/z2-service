/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

import hu.simplexion.z2.service.kotlin.ir.util.ConsumerCache
import hu.simplexion.z2.service.kotlin.ir.util.ProtoCache
import hu.simplexion.z2.service.kotlin.ir.util.ServiceFunctionCache
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructors
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

    val protoMessageBuilderClass = PROTO_MESSAGE_BUILDER_CLASS.runtimeClass(PROTO_PACKAGE)
    val protoMessageBuilderConstructor = protoMessageBuilderClass.constructors.first()
    val protoBuilderPack = protoMessageBuilderClass.functionByName(PROTO_BUILDER_PACK)

    val protoBuilderBoolean = protoMessageBuilderClass.functionByName(PROTO_BUILDER_BOOLEAN)
    val protoBuilderInt = protoMessageBuilderClass.functionByName(PROTO_BUILDER_INT)
    val protoBuilderLong = protoMessageBuilderClass.functionByName(PROTO_BUILDER_LONG)
    val protoBuilderString = protoMessageBuilderClass.functionByName(PROTO_BUILDER_STRING)
    val protoBuilderByteArray = protoMessageBuilderClass.functionByName(PROTO_BUILDER_BYTEARRAY)
    val protoBuilderUuid = protoMessageBuilderClass.functionByName(PROTO_BUILDER_UUID)
    val protoBuilderInstance = protoMessageBuilderClass.functionByName(PROTO_BUILDER_INSTANCE)

    val protoBuilderBooleanList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_BOOLEAN_LIST)
    val protoBuilderIntList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_INT_LIST)
    val protoBuilderLongList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_LONG_LIST)
    val protoBuilderStringList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_STRING_LIST)
    val protoBuilderByteArrayList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_BYTEARRAY_LIST)
    val protoBuilderUuidList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_UUID_LIST)
    val protoBuilderInstanceList = protoMessageBuilderClass.functionByName(PROTO_BUILDER_INSTANCE_LIST)

    val protoMessageClass = PROTO_MESSAGE_CLASS.runtimeClass(PROTO_PACKAGE)

    val protoMessageBoolean = protoMessageClass.functionByName(PROTO_MESSAGE_BOOLEAN)
    val protoMessageInt = protoMessageClass.functionByName(PROTO_MESSAGE_INT)
    val protoMessageLong = protoMessageClass.functionByName(PROTO_MESSAGE_LONG)
    val protoMessageString = protoMessageClass.functionByName(PROTO_MESSAGE_STRING)
    val protoMessageByteArray = protoMessageClass.functionByName(PROTO_MESSAGE_BYTEARRAY)
    val protoMessageUuid = protoMessageClass.functionByName(PROTO_MESSAGE_UUID)
    val protoMessageInstance = protoMessageClass.functionByName(PROTO_MESSAGE_INSTANCE)

    val protoMessageBooleanList = protoMessageClass.functionByName(PROTO_MESSAGE_BOOLEAN_LIST)
    val protoMessageIntList = protoMessageClass.functionByName(PROTO_MESSAGE_INT_LIST)
    val protoMessageLongList = protoMessageClass.functionByName(PROTO_MESSAGE_LONG_LIST)
    val protoMessageStringList = protoMessageClass.functionByName(PROTO_MESSAGE_STRING_LIST)
    val protoMessageByteArrayList = protoMessageClass.functionByName(PROTO_MESSAGE_BYTEARRAY_LIST)
    val protoMessageUuidList = protoMessageClass.functionByName(PROTO_MESSAGE_UUID_LIST)
    val protoMessageInstanceList = protoMessageClass.functionByName(PROTO_MESSAGE_INSTANCE_LIST)

    val uuidType = UUID.runtimeClass(UTIL_PACKAGE)

    val protoOneUnit = PROTO_ONE_UNIT.runtimeClass(PROTO_PACKAGE)
    val protoOneBoolean = PROTO_ONE_BOOLEAN.runtimeClass(PROTO_PACKAGE)
    val protoOneInt = PROTO_ONE_INT.runtimeClass(PROTO_PACKAGE)
    val protoOneLong = PROTO_ONE_LONG.runtimeClass(PROTO_PACKAGE)
    val protoOneString = PROTO_ONE_STRING.runtimeClass(PROTO_PACKAGE)
    val protoOneByteArray = PROTO_ONE_BYTEARRAY.runtimeClass(PROTO_PACKAGE)
    val protoOneUuid = PROTO_ONE_UUID.runtimeClass(PROTO_PACKAGE)

    val protoOneInstance = PROTO_ONE_INSTANCE.runtimeClass(PROTO_PACKAGE)
    val protoOneInstanceConstructor = protoOneInstance.constructors.first()

    val protoOneBooleanList = PROTO_ONE_BOOLEAN_LIST.runtimeClass(PROTO_PACKAGE)
    val protoOneIntList = PROTO_ONE_INT_LIST.runtimeClass(PROTO_PACKAGE)
    val protoOneLongList = PROTO_ONE_LONG_LIST.runtimeClass(PROTO_PACKAGE)
    val protoOneStringList = PROTO_ONE_STRING_LIST.runtimeClass(PROTO_PACKAGE)
    val protoOneByteArrayList = PROTO_ONE_BYTEARRAY_LIST.runtimeClass(PROTO_PACKAGE)
    val protoOneUuidList = PROTO_ONE_UUID_LIST.runtimeClass(PROTO_PACKAGE)

    val protoOneInstanceList = PROTO_ONE_INSTANCE_LIST.runtimeClass(PROTO_PACKAGE)
    val protoOneInstanceListConstructor = protoOneInstanceList.constructors.first()

    val protoEncoderClass = PROTO_ENCODER_CLASS.runtimeClass(PROTO_PACKAGE).owner
    val protoDecoderClass = PROTO_DECODER_CLASS.runtimeClass(PROTO_PACKAGE).owner

    val serviceContextType = SERVICE_CONTEXT_CLASS.runtimeClass().defaultType

    val notImplementedErrorClass = NOT_IMPLEMENTED_ERROR.runtimeClass(KOTLIN)
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