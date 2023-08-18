package hu.simplexion.z2.service.runtime.registry

import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceImpl

interface ServiceImplFactory {

    operator fun plusAssign(template: ServiceImpl)

    operator fun get(serviceName: String, context: ServiceContext?): ServiceImpl?

}