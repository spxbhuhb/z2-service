package hu.simplexion.z2.service.runtime


interface Service {

    fun <T> service() : T = throw NotImplementedError()

}