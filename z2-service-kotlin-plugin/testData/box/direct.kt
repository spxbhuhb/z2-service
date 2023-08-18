package foo.bar

import hu.simplexion.z2.commons.protobuf.*
import hu.simplexion.z2.commons.util.UUID
import hu.simplexion.z2.service.runtime.*
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport
import kotlinx.coroutines.runBlocking

interface BasicService : Service {
    suspend fun a(arg1: Int): Int
}

val basicServiceConsumer = getService<BasicService>()

class BasicServiceProvider : BasicService, ServiceProvider {

    override suspend fun a(arg1: Int): Int {
        return arg1 + 1
    }

}

fun box(): String {
    runBlocking {
        defaultServiceCallTransport = DumpTransport()
        defaultServiceProviderRegistry += BasicServiceProvider()

        val b1 = getService<BasicService>()
        val b2 = BasicServiceProvider()

        if (b1.a(12) != 13) return@runBlocking "Fail: through transport"
        if (b2.a(12) != 13) return@runBlocking "Fail: direct"
    }
    return "OK"
}

class DumpTransport : ServiceCallTransport {
    override suspend fun <T> call(serviceName: String, funName: String, payload: ByteArray, decoder: ProtoDecoder<T>): T {
        println("==== REQUEST ====")
        println(serviceName)
        println(funName)
        println(payload.dumpProto())

        val service = requireNotNull(defaultServiceProviderRegistry[serviceName])

        val responseBuilder = ProtoMessageBuilder()

        service.dispatch(funName, ProtoMessage(payload), BasicServiceContext(), responseBuilder)

        val responsePayload = responseBuilder.pack()
        println("==== RESPONSE ====")
        println(responsePayload.dumpProto())
        println(decoder::class.qualifiedName)

        return decoder.decodeProto(ProtoMessage(responseBuilder.pack()))
    }
}