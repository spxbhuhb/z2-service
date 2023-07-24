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

object Test : TestService, ServiceConsumer

object Test2 :  TestService, ServiceConsumer {
    override val serviceName = "manual"
}

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

}

class TestServiceProvider2 : TestService, ServiceProvider {
    override val serviceName = "manual"

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

}

fun box(): String {
    var name = Test.serviceName
    if (name != "foo.bar.TestService") return "Fail: Test.serviceName=$name"

    name = TestServiceProvider().serviceName
    if (name != "foo.bar.TestService") return "Fail TestServiceProvider().serviceName=$name"

    name = Test2.serviceName
    if (name != "manual") return "Fail: Test2.serviceName=$name"

    name = TestServiceProvider2().serviceName
    if (name != "manual") return "Fail: TestServiceProvider2().serviceName=$name"

    return "OK"
}