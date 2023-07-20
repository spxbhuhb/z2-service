package hu.simplexion.z2.service.runtime.transport

import hu.simplexion.z2.commons.protobuf.ProtoDecoder
import hu.simplexion.z2.commons.protobuf.ProtoEncoder
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.util.UUID

class RequestEnvelope(
    val callId : UUID<RequestEnvelope>,
    val serviceName : String,
    val funName : String,
    val payload : ByteArray
) : ProtoEncoder<RequestEnvelope>, ProtoDecoder<RequestEnvelope>{

    override fun decodeProto(message: ProtoMessage?): RequestEnvelope {
        requireNotNull(message)
        return RequestEnvelope(
            message.uuid(1),
            message.string(2),
            message.string(3),
            message.byteArray(4)
        )
    }

    override fun encodeProto(value: RequestEnvelope): ByteArray =
        ProtoMessageBuilder()
            .uuid(1, value.callId)
            .string(2, value.serviceName)
            .string(3, value.funName)
            .byteArray(4, value.payload)
            .pack()

}