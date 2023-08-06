package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.util.UUID

interface ServiceContext {
    val uuid: UUID<ServiceContext>
    var data : Any?
}