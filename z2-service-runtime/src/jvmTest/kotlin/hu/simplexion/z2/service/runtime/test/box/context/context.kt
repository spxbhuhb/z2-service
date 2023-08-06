package hu.simplexion.z2.service.runtime.test.box.context

import hu.simplexion.z2.service.runtime.*
import kotlinx.coroutines.runBlocking

interface TestService : Service {

    suspend fun testFun(arg1: Int, arg2: String): String

    suspend fun testFun(): String

}

fun <T> ServiceContext?.ensure(builder: () -> T): T {
    return builder()
}

val testServiceConsumer = getService<TestService>()

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

    override suspend fun testFun() =
        serviceContext.ensure { "hello" }
}

fun box(): String {
    defaultServiceProviderRegistry += TestServiceProvider()

    var response = runBlocking { testServiceConsumer.testFun(1, "hello") }

    if (!response.startsWith("i:1 s:hello BasicServiceContext(")) return "Fail (response=$response)"

    response = runBlocking { testServiceConsumer.testFun() }

    return if (response == "hello") "OK" else "Fail"
}