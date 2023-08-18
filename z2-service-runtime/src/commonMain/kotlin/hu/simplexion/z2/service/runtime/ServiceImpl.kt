package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder

interface ServiceImpl : Service {

    /**
     * Context of a service call. Set by `dispatch` when the call goes through it.
     */
    val serviceContext: ServiceContext?
        get() { throw IllegalStateException("ServiceContext should be overridden manually or by the compiler plugin, is the plugin missing?") }

    fun newInstance(serviceContext: ServiceContext?) : ServiceImpl {
        throw IllegalStateException("newInstance should be overridden by the compiler plugin, is tha plugin missing?")
    }

    /**
     * Called by service transports to execute a service call. Actual code of this function is generated
     * by the plugin.
     */
    suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        response: ProtoMessageBuilder
    ) {
        placeholder()
    }

}