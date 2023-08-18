package foo.bar

import hu.simplexion.z2.service.runtime.defaultServiceImplFactory
import hu.simplexion.z2.commons.protobuf.*
import hu.simplexion.z2.commons.util.UUID
import hu.simplexion.z2.service.runtime.*
import hu.simplexion.z2.service.runtime.BasicServiceContext
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface TestService : Service {

    suspend fun testFun(arg1: Int, arg2: String): String

}

val testServiceConsumer = getService<TestService>()

class TestServiceImpl : TestService, ServiceImpl {

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

}

fun box(): String {
    var response : String = ""
    runBlocking {
        defaultServiceImplFactory += TestServiceImpl()
        defaultServiceCallTransport = DumpTransport()
        response = testServiceConsumer.testFun(1, "hello")
    }
    return if (response.startsWith("i:1 s:hello BasicServiceContext(")) "OK" else "Fail (response=$response)"
}

class DumpTransport : ServiceCallTransport {
    override suspend fun <T> call(serviceName: String, funName: String, payload: ByteArray, decoder: ProtoDecoder<T>): T {
        println("==== REQUEST ====")
        println(serviceName)
        println(funName)
        println(payload.dumpProto())

        val service = requireNotNull(defaultServiceImplFactory[serviceName, BasicServiceContext()])

        val responseBuilder = ProtoMessageBuilder()

        service.dispatch(funName, ProtoMessage(payload), responseBuilder)

        val responsePayload = responseBuilder.pack()
        println("==== RESPONSE ====")
        println(responsePayload.dumpProto())
        println(decoder::class.qualifiedName)

        return decoder.decodeProto(ProtoMessage(responseBuilder.pack()))
    }
}