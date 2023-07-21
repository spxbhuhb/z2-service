package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.util.UUID

data class BasicServiceContext(
    val uuid: UUID<BasicServiceContext> = UUID()
) : ServiceContext