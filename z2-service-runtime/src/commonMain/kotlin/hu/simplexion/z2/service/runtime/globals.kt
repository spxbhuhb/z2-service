package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.service.runtime.transport.LocalServiceCallTransport
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport

var defaultServiceCallTransport : ServiceCallTransport = LocalServiceCallTransport()

val defaultServiceProviderRegistry = mutableMapOf<String, ServiceProvider>()