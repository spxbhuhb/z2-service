package hu.simplexion.z2.service.runtime.manual

import hu.simplexion.z2.service.runtime.defaultServiceProviderRegistry
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceTest {

    @Test
    fun serviceTest() {
        runBlocking {
            val provider = TestServiceProvider()
            defaultServiceProviderRegistry[provider.serviceName] = provider

            val response = TestServiceConsumer.testFun(1, "hello")
            assertEquals("i:1 s:hello", response)
        }
    }

}