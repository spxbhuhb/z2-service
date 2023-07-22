package hu.simplexion.z2.service.ktor.client

import hu.simplexion.z2.commons.protobuf.ProtoDecoder
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.util.UUID
import hu.simplexion.z2.commons.util.vmNowMicro
import hu.simplexion.z2.service.runtime.transport.RequestEnvelope
import hu.simplexion.z2.service.runtime.transport.ResponseEnvelope
import hu.simplexion.z2.service.runtime.transport.ServiceCallStatus
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class BasicWebSocketServiceTransport(
    val host: String,
    val port : Int,
    val path : String = "/z2/service"
) : ServiceCallTransport {

    val callTimeout = 10_000_000

    class OutgoingCall(
        val request: RequestEnvelope,
        val createdMicros: Long = vmNowMicro(),
        val responseChannel: Channel<ResponseEnvelope> = Channel(1)
    )

    val scope = CoroutineScope(Dispatchers.Default)

    val outgoingCalls = Channel<OutgoingCall>(Channel.UNLIMITED)
    val pendingCalls = mutableMapOf<UUID<RequestEnvelope>, OutgoingCall>()

    val client = HttpClient {
        install(WebSockets) {
            pingInterval = 20_000
        }
    }

    fun start() {
        scope.launch { run() }
        scope.launch { timeout() }
    }

    suspend fun run() {
        while (scope.isActive) {
            try {
                client.webSocket(HttpMethod.Get, host, port, path) {

                    launch {
                        for (call in outgoingCalls) {
                            pendingCalls[call.request.callId] = call
                            send(Frame.Binary(true, RequestEnvelope.encodeProto(call.request)))
                        }
                    }

                    for (frame in incoming) {
                        frame as? Frame.Binary ?: continue
                        try {
                            val responseEnvelope = ResponseEnvelope.decodeProto(ProtoMessage(frame.data))

                            val call = pendingCalls.remove(responseEnvelope.callId)
                                ?: continue // TODO report unknown call ids

                            call.responseChannel.trySend(responseEnvelope)
                        } catch (ex: Exception) {
                            ex.printStackTrace() // TODO logging
                        }
                    }

                }
            } catch (ex: Exception) {
                println(ex) // FIXME proper logging of client side exceptions
            } finally {
                println("closed socket")
            }
        }
    }

    suspend fun timeout() {
        while (scope.isActive) {
            val cutoff = vmNowMicro() - callTimeout

            pendingCalls
                .filter { it.value.createdMicros < cutoff }
                .toList()
                .forEach { (callId, call) ->
                    pendingCalls.remove(callId)
                    // TODO change ServiceCallStatus.Exception to Timeout
                    call.responseChannel.trySend(ResponseEnvelope(callId, ServiceCallStatus.Exception, ByteArray(0)))
                }

            delay(1_000)
        }
    }

    override suspend fun <T> call(serviceName: String, funName: String, payload: ByteArray, decoder: ProtoDecoder<T>): T =
        OutgoingCall(RequestEnvelope(UUID(), serviceName, funName, payload)).let {
            println("outgoing call: $serviceName.$funName")
            outgoingCalls.send(it)
            val responseEnvelope = it.responseChannel.receive()
            if (responseEnvelope.status != ServiceCallStatus.Ok) throw RuntimeException()
            decoder.decodeProto(ProtoMessage(responseEnvelope.payload))
        }

}