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
            defaultServiceImplFactory += TestServiceImpl(null)
            response = TestServiceConsumer.testFun(1, "hello")
        }
        return if (response.startsWith("i:1 s:hello null")) "OK" else "Fail (response=$response)"
    }

    @Test
    fun basicTest() {
        assertEquals("OK", box())
    }

}

interface TestService : Service {

    suspend fun testFun(arg1 : Int, arg2 : String) : String

}

object TestServiceConsumer : TestService {

    override var serviceName = "TestService"

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

class TestServiceImpl(override val serviceContext: ServiceContext?) : TestService, ServiceImpl {

    override var serviceName = "TestService"

    override suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        response : ProtoMessageBuilder
    ) {
        when (funName) {
            "testFun" -> response.string(1, testFun(payload.int(1), payload.string(2)))
            else -> throw IllegalStateException("unknown function: $funName")
        }
    }

    override fun newInstance(serviceContext: ServiceContext?) : ServiceImpl {
        return TestServiceImpl(serviceContext)
    }

    override suspend fun testFun(arg1: Int, arg2: String): String {
        return "i:$arg1 s:$arg2 $serviceContext"
    }

}