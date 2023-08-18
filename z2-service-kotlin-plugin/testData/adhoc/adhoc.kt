package foo.bar

import hu.simplexion.z2.service.runtime.*
import kotlinx.coroutines.runBlocking

interface BasicService : Service {
    suspend fun a(arg1: Int): Int
}

class BasicServiceImpl : BasicService, ServiceImpl {

    override suspend fun a(arg1: Int): Int {
        return arg1 + 1
    }

}

fun box(): String {
    runBlocking {
        if (BasicServiceImpl().a(12) != 13) return@runBlocking "Fail"
    }
    return "OK"
}