package hu.simplexion.z2.service.runtime.transport

import hu.simplexion.z2.commons.protobuf.ProtoDecoder
import hu.simplexion.z2.commons.protobuf.ProtoEncoder
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.util.UUID

/**
 * Envelope a response payload.
 *
 * @property  [callId]   Call id from the corresponding [RequestEnvelope].
 * @property  [status]   Result status of the call.
 * @property  [payload]  Return value of the called service function.
 */
class ResponseEnvelope(
    val callId: UUID<RequestEnvelope>,
    val status: ServiceCallStatus,
    val payload: ByteArray,
) {

    companion object : ProtoEncoder<ResponseEnvelope>, ProtoDecoder<ResponseEnvelope> {

        override fun decodeProto(message: ProtoMessage?): ResponseEnvelope {
            requireNotNull(message)
            return ResponseEnvelope(
                message.uuid(1),
                requireNotNull(message.int(2)).let { mv -> ServiceCallStatus.entries.first { it.value == mv } },
                message.byteArray(3)
            )
        }

        override fun encodeProto(value: ResponseEnvelope): ByteArray =
            ProtoMessageBuilder()
                .uuid(1, value.callId)
                .int(2, value.status.value)
                .byteArray(4, value.payload)
                .pack()
    }

}