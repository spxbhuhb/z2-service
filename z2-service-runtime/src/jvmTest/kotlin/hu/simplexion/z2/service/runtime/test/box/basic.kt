package hu.simplexion.z2.service.runtime.test.box

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.protobuf.ProtoOneString
import hu.simplexion.z2.service.runtime.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals

class BasicTest {

    fun box(): String {
        var response : String
        runBlocking {
            defaultServiceProviderRegistry += TestServiceProvider()
            response = TestServiceConsumer.testFun(1, "hello")
        }
        return if (response.startsWith("i:1 s:hello BasicServiceContext(")) "OK" else "Fail (response=$response)"
    }

    @Test
    fun basicTest() {
        assertEquals("OK", box())
    }

}

interface TestService : Service {

    suspend fun testFun(arg1 : Int, arg2 : String) : String = service()

}

object TestServiceConsumer : TestService, ServiceConsumer {

    override suspend fun testFun(arg1: Int, arg2: String): String =
        defaultServiceCallTransport
            .call(
                serviceName,
                "testFun",
                ProtoMessageBuilder()
                    .int(1, arg1)
                    .string(2, arg2)
                    .pack(),
                ProtoOneString
            )

}

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        context: ServiceContext,
        response : ProtoMessageBuilder
    ) {
        when (funName) {
            "testFun" -> response.string(1, testFun(payload.int(1), payload.string(2), context))
            else -> throw IllegalStateException("unknown function: $funName")
        }
    }

    suspend fun testFun(arg1: Int, arg2: String, serviceContext : ServiceContext?): String {
        return "i:$arg1 s:$arg2 $serviceContext"
    }

    override suspend fun testFun(arg1: Int, arg2: String) =
        testFun(arg1, arg2, null)

}