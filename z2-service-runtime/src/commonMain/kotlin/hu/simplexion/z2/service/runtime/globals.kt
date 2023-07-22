package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.service.runtime.registry.BasicServiceProviderRegistry
import hu.simplexion.z2.service.runtime.registry.ServiceProviderRegistry
import hu.simplexion.z2.service.runtime.transport.LocalServiceCallTransport
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport

var defaultServiceCallTransport : ServiceCallTransport = LocalServiceCallTransport()

val defaultServiceProviderRegistry : ServiceProviderRegistry = BasicServiceProviderRegistry()