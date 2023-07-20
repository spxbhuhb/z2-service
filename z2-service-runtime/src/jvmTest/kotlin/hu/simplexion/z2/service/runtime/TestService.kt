package hu.simplexion.z2.service.runtime

interface TestService : Service {

    suspend fun testFun(arg1 : Int, arg2 : String) : String = service()

}