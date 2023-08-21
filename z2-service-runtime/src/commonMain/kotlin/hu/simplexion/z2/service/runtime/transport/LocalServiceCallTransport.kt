package hu.simplexion.z2.service.runtime.transport

import hu.simplexion.z2.commons.protobuf.ProtoDecoder
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.service.runtime.BasicServiceContext
import hu.simplexion.z2.service.runtime.defaultServiceImplFactory

/**
 * Get the service from the implementation factory, execute the function with it and
 * returns with the result.
 */
class LocalServiceCallTransport : ServiceCallTransport {

    override suspend fun <T> call(serviceName: String, funName: String, payload: ByteArray, decoder: ProtoDecoder<T>): T {

        val service = requireNotNull(defaultServiceImplFactory[serviceName, null])

        val responseBuilder = ProtoMessageBuilder()

        service.dispatch(funName, ProtoMessage(payload), responseBuilder)

        return decoder.decodeProto(ProtoMessage(responseBuilder.pack()))
    }

}