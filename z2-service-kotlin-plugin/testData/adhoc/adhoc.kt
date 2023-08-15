package foo.bar

import hu.simplexion.z2.commons.protobuf.*
import hu.simplexion.z2.commons.util.UUID
import hu.simplexion.z2.service.runtime.*
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport
import kotlinx.coroutines.runBlocking

enum class E {
    V1,
    V2
}

interface BasicService : Service {
    suspend fun b(arg1 : E?): E?
}

val basicServiceConsumer = getService<BasicService>()

class BasicServiceProvider : BasicService, ServiceProvider {

    override suspend fun b(arg1 : E?) = arg1

}

fun box(): String {
    runBlocking {
        defaultServiceCallTransport = DumpTransport()
        defaultServiceProviderRegistry += BasicServiceProvider()
        basicServiceConsumer.b(null)
    }
    return "OK"
}

//fun box() : String {
//    ordinalListToEnum(E.entries, listOf(0,1))
//    ordinalListOrNullToEnum(E.entries, listOf(0,1))
//    return "OK"
//}

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