package hu.simplexion.z2.service.runtime

interface ServiceConsumer {

    /**
     * Name of the service. You may set this manually or let the plugin set it.
     * The plugin uses the fully qualified class name of the service interface.
     */
    val serviceName: String
        get() = throw NotImplementedError("Service is not implemented and/or the z2-service plugin is missing.")

}