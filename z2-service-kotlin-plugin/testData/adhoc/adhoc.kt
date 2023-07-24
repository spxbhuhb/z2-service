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

class AtomicInteger() {
    var value = 0
    fun incrementAndGet() : Int {
        value++
        return value
    }
}

class ClickServiceProvider : ClickService, ServiceProvider {

    var clicked = AtomicInteger()

    override suspend fun click(): Int {
        return clicked.incrementAndGet()
    }

}

fun box(): String {
    return "OK"
}