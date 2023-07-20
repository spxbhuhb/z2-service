package hu.simplexion.z2.service.runtime.transport

import hu.simplexion.z2.commons.protobuf.ProtoDecoder
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.service.runtime.BasicServiceContext
import hu.simplexion.z2.service.runtime.defaultServiceProviderRegistry

/**
 * Get the service from the local provider registry, execute the function with it and
 * returns with the result.
 */
class LocalServiceCallTransport : ServiceCallTransport {

    override suspend fun <T> call(serviceName: String, funName: String, payload: ByteArray, decoder: ProtoDecoder<T>): T {

        val service = requireNotNull(defaultServiceProviderRegistry[serviceName])

        val responseBuilder = ProtoMessageBuilder()

        service.dispatch(funName, ProtoMessage(payload), BasicServiceContext(), responseBuilder)

        return decoder.decodeProto(ProtoMessage(responseBuilder.pack()))
    }

}