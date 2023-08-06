package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoDecoder
import hu.simplexion.z2.commons.protobuf.ProtoEncoder
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.util.UUID

data class BasicServiceContext(
    val uuid: UUID<BasicServiceContext> = UUID(),
    val data : Any? = null
) : ServiceContext {
    companion object : ProtoDecoder<BasicServiceContext>, ProtoEncoder<BasicServiceContext> {

        override fun decodeProto(message: ProtoMessage?): BasicServiceContext {
            if (message == null) return BasicServiceContext()
            return BasicServiceContext(message.uuid(1))
        }

        override fun encodeProto(value: BasicServiceContext): ByteArray {
            return ProtoMessageBuilder().uuid(1, value.uuid).pack()
        }
    }
}