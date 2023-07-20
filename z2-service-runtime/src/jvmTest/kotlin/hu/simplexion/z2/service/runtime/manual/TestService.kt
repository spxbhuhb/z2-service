package hu.simplexion.z2.service.runtime.manual

import hu.simplexion.z2.service.runtime.Service

interface TestService : Service {

    suspend fun testFun(arg1 : Int, arg2 : String) : String = service()

}