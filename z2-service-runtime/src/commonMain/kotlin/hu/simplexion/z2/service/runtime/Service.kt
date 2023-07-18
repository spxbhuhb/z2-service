package hu.simplexion.z2.service.runtime


interface Service {

    fun <T> service() : T =
        throw IllegalStateException("Service.service should be removed, is the compiler plugin missing?")

}