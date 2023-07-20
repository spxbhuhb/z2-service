package hu.simplexion.z2.service.runtime.transport

import hu.simplexion.z2.commons.protobuf.ProtoDecoder

interface ServiceCallTransport {

    suspend fun <T> call(serviceName : String, funName : String, payload: ByteArray, decoder : ProtoDecoder<T>): T

}