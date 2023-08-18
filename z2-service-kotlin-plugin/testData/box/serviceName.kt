package foo.bar

import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.protobuf.ProtoOneString
import hu.simplexion.z2.service.runtime.Service
import hu.simplexion.z2.service.runtime.defaultServiceCallTransport
import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceImpl
import hu.simplexion.z2.service.runtime.getService
import kotlinx.coroutines.runBlocking
import hu.simplexion.z2.service.runtime.defaultServiceImplFactory

interface TestService : Service {

    suspend fun testFun(arg1: Int, arg2: String): String

}

val test = getService<TestService>()
val test2 = getService<TestService>().also { it.serviceName = "manual" }

class TestServiceImpl : TestService, ServiceImpl {

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

}

class TestServiceImpl2 : TestService, ServiceImpl {

    override var serviceName = "manual"

    override suspend fun testFun(arg1: Int, arg2: String) =
        "i:$arg1 s:$arg2 $serviceContext"

}

fun box(): String {
    var name = test.serviceName
    if (name != "foo.bar.TestService") return "Fail: Test.serviceName=$name"

    name = TestServiceImpl().serviceName
    if (name != "foo.bar.TestService") return "Fail TestServiceImpl().serviceName=$name"

    name = test2.serviceName
    if (name != "manual") return "Fail: Test2.serviceName=$name"

    name = TestServiceImpl2().serviceName
    if (name != "manual") return "Fail: TestServiceImpl2().serviceName=$name"

    return "OK"
}