package hu.simplexion.z2.service.runtime.factory

import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceImpl

class BasicServiceImplFactory : ServiceImplFactory {

    val templates = mutableMapOf<String, ServiceImpl>()

    override fun plusAssign(template: ServiceImpl) {
        templates[template.serviceName] = template
    }

    override fun get(serviceName: String, context: ServiceContext?): ServiceImpl? =
        templates[serviceName]?.newInstance(context)

}