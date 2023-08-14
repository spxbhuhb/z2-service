package foo.bar

import hu.simplexion.z2.commons.protobuf.*
import hu.simplexion.z2.commons.util.UUID
import hu.simplexion.z2.service.runtime.*
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport
import kotlinx.coroutines.runBlocking

interface BasicService : Service {
    suspend fun b(arg1 : Boolean, arg2 : Boolean?): Boolean?
}

val basicServiceConsumer = getService<BasicService>()

class BasicServiceProvider : BasicService, ServiceProvider {

    override suspend fun b(arg1 : Boolean, arg2 : Boolean?) = arg1

}

fun box(): String {
    runBlocking {
        defaultServiceCallTransport = DumpTransport()
        defaultServiceProviderRegistry += BasicServiceProvider()
        basicServiceConsumer.b(true, null)
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