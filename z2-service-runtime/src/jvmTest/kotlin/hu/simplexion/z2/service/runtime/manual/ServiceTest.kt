package hu.simplexion.z2.service.runtime.manual

import hu.simplexion.z2.service.runtime.defaultServiceProviderRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceTest {

    fun box(): String {
        var response : String
        runBlocking {
            val provider = TestServiceProvider()
            defaultServiceProviderRegistry[provider.serviceName] = provider

            response = TestServiceConsumer.testFun(1, "hello")
        }
        return if (response.startsWith("i:1 s:hello BasicServiceContext(")) "OK" else "Fail (response=$response)"
    }

    @Test
    fun serviceTest() {
        assertEquals("OK", box())
    }

}