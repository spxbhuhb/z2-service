package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.util.UUID

data class BasicServiceContext(
    override val uuid: UUID<ServiceContext> = UUID(),
    override var data : MutableMap<Any,Any?> = mutableMapOf()
) : ServiceContext