package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder

interface ServiceProvider {

    /**
     * Name of the service. You may set this manually or let the plugin set it.
     * The plugin uses the fully qualified class name of the service interface.
     */
    val serviceName: String
        get() = throw NotImplementedError("Service is not implemented and/or the z2-service plugin is missing.")

    /**
     * Context of a service call. The plugin replaces any use of this property with the context
     * passed to `dispatch`. Check the documentation for more details.
     */
    val serviceContext: ServiceContext?
        get() = throw IllegalStateException("serviceContext should not be accessed outside service functions")

    /**
     * Called by service transports to execute a service call. Actual code of this function is generated
     * by the plugin.
     */
    suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        context: ServiceContext,
        response: ProtoMessageBuilder
    ) {
        throw IllegalStateException("ServiceProvider.dispatch should be overridden, is the compiler plugin missing?")
    }

}