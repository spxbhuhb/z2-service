package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.service.runtime.transport.LocalServiceCallTransport
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport

val defaultServiceCallTransport : ServiceCallTransport = LocalServiceCallTransport()

val defaultServiceProviderRegistry = mutableMapOf<String, ServiceProvider>()

const val RESPONSE_CODE_SUCCESS = 200
const val RESPONSE_CODE_NOT_FOUND = 404
const val RESPONSE_CODE_SERVER_ERROR = 500

internal const val CALL_ID_FIELD_NUMBER = 1
internal const val CALL_SERVICE_NAME_FIELD_NUMBER = 2
internal const val CALL_SERVICE_FUN_NAME_FIELD_NUMBER = 3
internal const val CALL_PAYLOAD_FIELD_NUMBER = 4

internal const val RESPONSE_CALL_ID_FIELD_NUMBER = 1
internal const val RESPONSE_CODE_FIELD_NUMBER = 2
internal const val RESPONSE_ERROR_MESSAGE_FIELD_NUMBER = 3
internal const val RESPONSE_PAYLOAD_FIELD_NUMBER = 4