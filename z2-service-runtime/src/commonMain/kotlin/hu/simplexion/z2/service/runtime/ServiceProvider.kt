package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder

interface ServiceProvider {

    val serviceName: String
        get() = checkNotNull(this::class.simpleName).substringBeforeLast("Provider")

    val serviceContext: ServiceContext?
        get() = null

    suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        context: ServiceContext,
        response: ProtoMessageBuilder
    ) {
        throw IllegalStateException("ServiceProvider.dispatch should be overridden, is the compiler plugin missing?")
    }

}