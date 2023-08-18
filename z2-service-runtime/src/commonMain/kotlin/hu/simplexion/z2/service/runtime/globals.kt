package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.service.runtime.registry.BasicServiceImplFactory
import hu.simplexion.z2.service.runtime.registry.ServiceImplFactory
import hu.simplexion.z2.service.runtime.transport.LocalServiceCallTransport
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport

var defaultServiceCallTransport : ServiceCallTransport = LocalServiceCallTransport()

val defaultServiceImplFactory : ServiceImplFactory = BasicServiceImplFactory()

/**
 * Get a service consumer for the interface, specified by the type parameter.
 *
 * **You should NOT pass the [consumer] parameter! It is set by the compiler plugin.**
 *
 * ```kotlin
 * val clicks = getService<ClickApi>()
 * ```
 */
fun <T : Service> getService(consumer : T? = null) : T {
    return checkNotNull(consumer)
}