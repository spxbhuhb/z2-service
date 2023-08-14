/*
 * Copyright © 2022-2023, Simplexion, Hungary and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package hu.simplexion.z2.service.kotlin.ir

const val RUNTIME_PACKAGE = "hu.simplexion.z2.service.runtime"

const val SERVICE_CLASS = "Service"
const val SERVICE_NAME_PROPERTY = "serviceName"

const val CONSUMER_POSTFIX = "\$Consumer"
const val SERVICE_PROVIDER_CLASS = "ServiceProvider"
const val SERVICE_CONSUMER_CLASS = "ServiceConsumer"

const val TRANSPORT_PACKAGE = "$RUNTIME_PACKAGE.transport"
const val GLOBALS_CLASS = "GlobalsKt"
const val DEFAULT_SERVICE_CALL_TRANSPORT = "defaultServiceCallTransport"
const val GET_SERVICE = "getService"

val FUN_NAMES_TO_SKIP = listOf("service", "equals", "hashCode", "toString")
const val SERVICE_CONTEXT_PROPERTY = "serviceContext"
const val SERVICE_CONTEXT_ARG_NAME = "serviceContext"
const val SERVICE_CONTEXT_CLASS = "ServiceContext"

const val DISPATCH_NAME = "dispatch"
const val DISPATCH_FUN_NAME_INDEX = 0
const val DISPATCH_PAYLOAD_INDEX = 1
const val DISPATCH_CONTEXT_INDEX = 2
const val DISPATCH_RESPONSE_INDEX = 3

const val SERVICE_CALL_TRANSPORT_CLASS = "ServiceCallTransport"
const val CALL_FUNCTION = "call"
const val CALL_TYPE_INDEX = 0 // type argument index for CALL_FUNCTION, this is the return type of the service call
const val CALL_SERVICE_NAME_INDEX = 0
const val CALL_FUN_NAME_INDEX = 1
const val CALL_PAYLOAD_INDEX = 2
const val CALL_DECODER_INDEX = 3

const val SIGNATURE_BOOLEAN = "Z"
const val SIGNATURE_INT = "I"
const val SIGNATURE_LONG = "J"
const val SIGNATURE_STRING = "S"
const val SIGNATURE_BYTEARRAY = "[B"
const val SIGNATURE_UUID = "U"
const val SIGNATURE_INSTANCE = "L";

const val SIGNATURE_LIST = "*"
const val SIGNATURE_DELIMITER = ";"
const val SIGNATURE_UNKNOWN = "?"

const val SIGNATURE_BOOLEAN_LIST = "*Z"
const val SIGNATURE_INT_LIST = "*I"
const val SIGNATURE_LONG_LIST = "*J"
const val SIGNATURE_STRING_LIST = "*S"
const val SIGNATURE_BYTEARRAY_LIST = "*[B"
const val SIGNATURE_UUID_LIST = "*U"

const val PROTO_PACKAGE = "hu.simplexion.z2.commons.protobuf"

const val PROTO_ONE_UNIT = "ProtoOneUnit"

const val PROTO_ONE_BOOLEAN = "ProtoOneBoolean"
const val PROTO_ONE_BOOLEAN_OR_NULL = "ProtoOneBooleanOrNull"
const val PROTO_ONE_BOOLEAN_LIST = "ProtoOneBooleanList"
const val PROTO_ONE_BOOLEAN_LIST_OR_NULL = "ProtoOneBooleanListOrNull"

const val PROTO_ONE_INT = "ProtoOneInt"
const val PROTO_ONE_INT_OR_NULL = "ProtoOneIntOrNull"
const val PROTO_ONE_INT_LIST = "ProtoOneIntList"
const val PROTO_ONE_INT_LIST_OR_NULL = "ProtoOneIntListOrNull"

const val PROTO_ONE_LONG = "ProtoOneLong"
const val PROTO_ONE_LONG_OR_NULL = "ProtoOneLongOrNull"
const val PROTO_ONE_LONG_LIST = "ProtoOneLongList"
const val PROTO_ONE_LONG_LIST_OR_NULL = "ProtoOneLongListOrNull"

const val PROTO_ONE_STRING = "ProtoOneString"
const val PROTO_ONE_STRING_OR_NULL = "ProtoOneStringOrNull"
const val PROTO_ONE_STRING_LIST = "ProtoOneStringList"
const val PROTO_ONE_STRING_LIST_OR_NULL = "ProtoOneStringListOrNull"

const val PROTO_ONE_BYTEARRAY = "ProtoOneByteArray"
const val PROTO_ONE_BYTEARRAY_OR_NULL = "ProtoOneByteArrayOrNull"
const val PROTO_ONE_BYTEARRAY_LIST = "ProtoOneByteArrayList"
const val PROTO_ONE_BYTEARRAY_LIST_OR_NULL = "ProtoOneByteArrayListOrNull"

const val PROTO_ONE_UUID = "ProtoOneUuid"
const val PROTO_ONE_UUID_OR_NULL = "ProtoOneUuidOrNull"
const val PROTO_ONE_UUID_LIST = "ProtoOneUuidList"
const val PROTO_ONE_UUID_LIST_OR_NULL = "ProtoOneUuidListOrNull"

const val PROTO_ONE_INSTANCE = "ProtoOneInstance"
const val PROTO_ONE_INSTANCE_OR_NULL = "ProtoOneInstanceOrNull"
const val PROTO_ONE_INSTANCE_LIST = "ProtoOneInstanceList"
const val PROTO_ONE_INSTANCE_LIST_OR_NULL = "ProtoOneInstanceListOrNull"

const val PROTO_MESSAGE_BUILDER_CLASS = "ProtoMessageBuilder"
const val PROTO_BUILDER_PACK = "pack"

const val PROTO_BUILDER_BOOLEAN = "boolean"
const val PROTO_BUILDER_BOOLEAN_OR_NULL = "booleanOrNull"
const val PROTO_BUILDER_BOOLEAN_LIST_OR_NULL = "booleanList"
const val PROTO_BUILDER_BOOLEAN_LIST = "booleanListOrNull"

const val PROTO_BUILDER_INT = "int"
const val PROTO_BUILDER_INT_OR_NULL = "intOrNull"
const val PROTO_BUILDER_INT_LIST = "intList"
const val PROTO_BUILDER_INT_LIST_OR_NULL = "intListOrNull"

const val PROTO_BUILDER_LONG = "long"
const val PROTO_BUILDER_LONG_OR_NULL = "longOrNull"
const val PROTO_BUILDER_LONG_LIST = "longList"
const val PROTO_BUILDER_LONG_LIST_OR_NULL = "longListOrNull"

const val PROTO_BUILDER_STRING = "string"
const val PROTO_BUILDER_STRING_OR_NULL = "stringOrNull"
const val PROTO_BUILDER_STRING_LIST = "stringList"
const val PROTO_BUILDER_STRING_LIST_OR_NULL = "stringListOrNull"

const val PROTO_BUILDER_BYTEARRAY = "byteArray"
const val PROTO_BUILDER_BYTEARRAY_OR_NULL = "byteArrayOrNull"
const val PROTO_BUILDER_BYTEARRAY_LIST = "byteArrayList"
const val PROTO_BUILDER_BYTEARRAY_LIST_OR_NULL = "byteArrayListOrNull"

const val PROTO_BUILDER_UUID = "uuid"
const val PROTO_BUILDER_UUID_OR_NULL = "uuidOrNull"
const val PROTO_BUILDER_UUID_LIST = "uuidList"
const val PROTO_BUILDER_UUID_LIST_OR_NULL = "uuidListOrNull"

const val PROTO_BUILDER_INSTANCE = "instance"
const val PROTO_BUILDER_INSTANCE_OR_NULL = "instanceOrNull"
const val PROTO_BUILDER_INSTANCE_LIST = "instanceList"
const val PROTO_BUILDER_INSTANCE_LIST_OR_NULL = "instanceListOrNull"

const val PROTO_MESSAGE_CLASS = "ProtoMessage"

const val PROTO_MESSAGE_BOOLEAN = "boolean"
const val PROTO_MESSAGE_BOOLEAN_OR_NULL = "booleanOrNull"
const val PROTO_MESSAGE_BOOLEAN_LIST = "booleanList"
const val PROTO_MESSAGE_BOOLEAN_LIST_OR_NULL = "booleanListOrNull"

const val PROTO_MESSAGE_INT = "int"
const val PROTO_MESSAGE_INT_OR_NULL = "intOrNull"
const val PROTO_MESSAGE_INT_LIST = "intList"
const val PROTO_MESSAGE_INT_LIST_OR_NULL = "intListOrNull"

const val PROTO_MESSAGE_LONG = "long"
const val PROTO_MESSAGE_LONG_OR_NULL = "longOrNull"
const val PROTO_MESSAGE_LONG_LIST = "longList"
const val PROTO_MESSAGE_LONG_LIST_OR_NULL = "longListOrNull"

const val PROTO_MESSAGE_STRING = "string"
const val PROTO_MESSAGE_STRING_OR_NULL = "stringOrNull"
const val PROTO_MESSAGE_STRING_LIST = "stringList"
const val PROTO_MESSAGE_STRING_LIST_OR_NULL = "stringListOrNull"

const val PROTO_MESSAGE_BYTEARRAY = "byteArray"
const val PROTO_MESSAGE_BYTEARRAY_OR_NULL = "byteArrayOrNull"
const val PROTO_MESSAGE_BYTEARRAY_LIST = "byteArrayList"
const val PROTO_MESSAGE_BYTEARRAY_LIST_OR_NULL = "byteArrayListOrNull"

const val PROTO_MESSAGE_UUID = "uuid"
const val PROTO_MESSAGE_UUID_OR_NULL = "uuidOrNull"
const val PROTO_MESSAGE_UUID_LIST = "uuidList"
const val PROTO_MESSAGE_UUID_LIST_OR_NULL = "uuidListOrNull"

const val PROTO_MESSAGE_INSTANCE = "instance"
const val PROTO_MESSAGE_INSTANCE_OR_NULL = "instanceOrNull"
const val PROTO_MESSAGE_INSTANCE_LIST = "instanceList"
const val PROTO_MESSAGE_INSTANCE_LIST_OR_NULL = "instanceListOrNull"

const val PROTO_ENCODER_CLASS = "ProtoEncoder"
const val PROTO_DECODER_CLASS = "ProtoDecoder"

const val TIME_PACKAGE = "kotlin.time"
const val TIME_DURATION = "Duration"

const val DATETIME_PACKAGE = "kotlinx.datetime"
const val DATETIME_INSTANT = "Instant"
const val DATETIME_LOCAL_DATE = "LocalDate"
const val DATETIME_LOCAL_DATE_TIME = "LocalDateTime"

const val PROTO_DURATION = "ProtoDuration"
const val PROTO_INSTANT = "ProtoInstant"
const val PROTO_LOCAL_DATE = "ProtoLocalDate"
const val PROTO_LOCAL_DATE_TIME = "ProtoLocalDateTime"

const val UTIL_PACKAGE = "hu.simplexion.z2.commons.util"
const val UUID = "UUID"

const val COMPANION_OBJECT_NAME = "Companion"

const val KOTLIN = "kotlin"
const val KOTLIN_COLLECTIONS = "kotlin.collections"
const val LIST = "List"
const val NOT_IMPLEMENTED_ERROR = "NotImplementedError"

