package foo.bar

import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.protobuf.ProtoOneString
import hu.simplexion.z2.service.runtime.Service
import hu.simplexion.z2.service.runtime.ServiceConsumer
import hu.simplexion.z2.service.runtime.defaultServiceCallTransport

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

fun box() : String {
    return "OK"
}