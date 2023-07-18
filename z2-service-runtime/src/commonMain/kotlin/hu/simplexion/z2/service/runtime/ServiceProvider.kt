package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoRecord

interface ServiceProvider {

    suspend fun dispatch(funName : String, parameters : List<ProtoRecord>) : ProtoRecord =
        throw IllegalStateException("ServiceProvider.dispatch should be overridden, is the compiler plugin missing?")

}