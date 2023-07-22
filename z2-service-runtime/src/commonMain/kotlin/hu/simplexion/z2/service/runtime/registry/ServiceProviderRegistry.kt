package hu.simplexion.z2.service.runtime.registry

import hu.simplexion.z2.service.runtime.ServiceProvider

interface ServiceProviderRegistry {

    operator fun plusAssign(provider: ServiceProvider)

    operator fun get(serviceName: String): ServiceProvider?

}