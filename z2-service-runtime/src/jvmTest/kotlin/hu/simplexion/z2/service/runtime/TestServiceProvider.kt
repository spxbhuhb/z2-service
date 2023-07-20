package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun dispatch(funName: String, payload: ProtoMessage, builder : ProtoMessageBuilder) {
        when (funName) {
            "testFun" -> builder.string(1, testFun(payload.int(1), payload.string(2)))
            else -> throw IllegalStateException("unknown function: $funName")
        }
    }

    override suspend fun testFun(arg1: Int, arg2: String): String {
        return "i:$arg1 s:$arg2"
    }

}