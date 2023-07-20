package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder

interface ServiceProvider {

    val serviceName : String
        get() = checkNotNull(this::class.qualifiedName).substringBeforeLast("Provider")

    suspend fun dispatch(funName: String, payload: ProtoMessage, builder : ProtoMessageBuilder) {
        throw IllegalStateException("ServiceProvider.dispatch should be overridden, is the compiler plugin missing?")
    }

}