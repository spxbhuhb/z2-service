package hu.simplexion.z2.service.runtime.manual

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceProvider

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        context: ServiceContext,
        response : ProtoMessageBuilder
    ) {
        when (funName) {
            "testFun" -> response.string(1, testFun(payload.int(1), payload.string(2), context))
            else -> throw IllegalStateException("unknown function: $funName")
        }
    }

    suspend fun testFun(arg1: Int, arg2: String, serviceContext : ServiceContext?): String {
        return "i:$arg1 s:$arg2 $serviceContext"
    }

    override suspend fun testFun(arg1: Int, arg2: String) =
        testFun(arg1, arg2, null)

}