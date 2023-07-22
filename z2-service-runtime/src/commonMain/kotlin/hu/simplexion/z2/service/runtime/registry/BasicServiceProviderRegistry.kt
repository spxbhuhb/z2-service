package hu.simplexion.z2.service.runtime.registry

import hu.simplexion.z2.service.runtime.ServiceProvider

class BasicServiceProviderRegistry : ServiceProviderRegistry {

    val providers = mutableMapOf<String, ServiceProvider>()

    override fun plusAssign(provider: ServiceProvider) {
        providers[provider.serviceName] = provider
    }

    override fun get(serviceName: String): ServiceProvider? =
        providers[serviceName]

}