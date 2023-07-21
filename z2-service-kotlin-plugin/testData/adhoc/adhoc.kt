package foo.bar

import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.protobuf.ProtoOneString
import hu.simplexion.z2.service.runtime.Service
import hu.simplexion.z2.service.runtime.ServiceConsumer
import hu.simplexion.z2.service.runtime.defaultServiceCallTransport
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceProvider
import kotlinx.coroutines.runBlocking
import hu.simplexion.z2.service.runtime.defaultServiceProviderRegistry

interface TestService : Service {

    suspend fun testFun(arg1: Int, arg2: String): String = service()

}

object TestServiceConsumer : TestService, ServiceConsumer

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

}

//object TestServiceConsumer : TestService, ServiceConsumer {
//
//    override suspend fun testFun(arg1: Int, arg2: String): String =
//        defaultServiceCallTransport
//            .call(
//                serviceName,
//                "testFun",
//                ProtoMessageBuilder()
//                    .int(1, arg1)
//                    .string(2, arg2)
//                    .pack(),
//                ProtoOneString
//            )
//
//}
//
//class TestServiceProvider : TestService, ServiceProvider {
//
//    override suspend fun dispatch(
//        funName: String,
//        payload: ProtoMessage,
//        context: ServiceContext,
//        response : ProtoMessageBuilder
//    ) {
//        when (funName) {
//            "testFun" -> response.string(1, testFun(payload.int(1), payload.string(2), context))
//        }
//    }
//
//    suspend fun testFun(arg1: Int, arg2: String, serviceContext : ServiceContext?): String {
//        return "i:$arg1 s:$arg2 $serviceContext"
//    }
//
//    override suspend fun testFun(arg1: Int, arg2: String) =
//        testFun(arg1, arg2, null)
//
//}

fun box(): String {
    var response : String = ""
    runBlocking {
        val provider = TestServiceProvider()
        defaultServiceProviderRegistry[provider.serviceName] = provider

        response = TestServiceConsumer.testFun(1, "hello")
    }
    return if (response.startsWith("i:1 s:hello BasicServiceContext(")) "OK" else "Fail (response=$response)"
}