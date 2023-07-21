package hu.simplexion.z2.service.runtime


interface Service {

    fun <T> service() : T = throw NotImplementedError("Service is not implemented and/or the z2-service plugin is missing.")

}