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

interface ClickService : Service {

    suspend fun click() : Int = service()

}

object ClickServiceConsumer : ClickService, ServiceConsumer

class ClickServiceProvider : ClickService, ServiceProvider {

    var clicked = 23

    override suspend fun click(): Int {
        return clicked++
    }

}

fun box(): String {
    var response : Int = 0
    runBlocking {
        val provider = ClickServiceProvider()
        defaultServiceProviderRegistry[provider.serviceName] = provider

        response = ClickServiceConsumer.click()
    }
    return if (response == 23) "OK" else "Fail (response=$response)"
}