package hu.simplexion.z2.service.runtime

interface ServiceConsumer {

    val serviceName : String
        get() = checkNotNull(this::class.qualifiedName).substringBeforeLast("Consumer")

}