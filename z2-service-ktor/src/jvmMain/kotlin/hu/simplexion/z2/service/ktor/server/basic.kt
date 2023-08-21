package hu.simplexion.z2.service.ktor.server

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.service.runtime.BasicServiceContext
import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.defaultServiceImplFactory
import hu.simplexion.z2.service.runtime.transport.RequestEnvelope
import hu.simplexion.z2.service.runtime.transport.ResponseEnvelope
import hu.simplexion.z2.service.runtime.transport.ServiceCallStatus
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Routing.basicWebsocketServiceCallTransport(
    path: String = "/z2/service",
    newContext: () -> ServiceContext = { BasicServiceContext() }
) {

    webSocket(path) {
        try {
            val context = newContext()

            for (frame in incoming) {
                frame as? Frame.Binary ?: continue

                // if this throws an exception there is an error in the service framework
                // better to close the connection then
                val requestEnvelope = RequestEnvelope.decodeProto(ProtoMessage(frame.data))

                val responseEnvelope = try {

                    val service = requireNotNull(defaultServiceImplFactory[requestEnvelope.serviceName, context])

                    val responseBuilder = ProtoMessageBuilder()

                    service.dispatch(
                        requestEnvelope.funName,
                        ProtoMessage(requestEnvelope.payload),
                        responseBuilder
                    )

                    ResponseEnvelope(
                        requestEnvelope.callId,
                        ServiceCallStatus.Ok,
                        responseBuilder.pack()
                    )

                } catch (ex: Exception) {
                    ex.printStackTrace() // TODO proper logging
                    ResponseEnvelope(
                        requestEnvelope.callId,
                        ServiceCallStatus.Exception,
                        ProtoMessageBuilder().string(1, ex::class.simpleName ?: "").pack()
                    )
                }

                send(Frame.Binary(true, ResponseEnvelope.encodeProto(responseEnvelope)))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}