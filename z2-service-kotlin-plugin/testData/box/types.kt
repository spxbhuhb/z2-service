package hu.simplexion.z2.service.runtime.test.box

import hu.simplexion.z2.commons.protobuf.*
import hu.simplexion.z2.commons.util.UUID
import hu.simplexion.z2.service.runtime.*
import kotlinx.coroutines.runBlocking
import hu.simplexion.z2.service.runtime.transport.ServiceCallTransport

fun box(): String {
    val booleanVal = true
    val intVal = 123
    val longVal = 1234L
    val stringVal = "abc"
    val byteArrayVal = byteArrayOf(9, 8, 7)
    val uuidVal = UUID<TypesService>()
    val instanceVal = A(true, 12, "hello")

    val booleanListVal = listOf(true, false, true)
    val intListVal = listOf(1, 2, 3)
    val longListVal = listOf(1L, 2L, 3L, 4L)
    val stringListVal = listOf("a", "b", "c")
    val byteArrayListVal = listOf(byteArrayOf(1), byteArrayOf(2), byteArrayOf(3))
    val uuidListVal = listOf(UUID<TypesService>(), UUID(), UUID())

    val instanceListVal = listOf(
        B(A(true, 123, "a", mutableListOf(1, 2, 3)), "aa"),
        B(A(false, 456, "b", mutableListOf(4, 5, 6)), "bb"),
        B(A(true, 789, "c", mutableListOf(7, 8, 9)), "cc")
    )

    val errors = mutableListOf<String>()

    runBlocking {
        defaultServiceCallTransport = DumpTransport()
        defaultServiceProviderRegistry += TypesServiceProvider()

        if (TypesServiceConsumer.testFun(booleanVal) != booleanVal) errors += "booleanValue"
        if (TypesServiceConsumer.testFun(intVal) != intVal) errors += "intVal"
        if (TypesServiceConsumer.testFun(longVal) != longVal) errors += "longVal"
        if (TypesServiceConsumer.testFun(stringVal) != stringVal) errors += "stringVal"
        if (!TypesServiceConsumer.testFun(byteArrayVal).contentEquals(byteArrayVal)) errors += "byteArrayVal"
        if (TypesServiceConsumer.testFun(uuidVal) != uuidVal) errors += "uuidVal"
        if (TypesServiceConsumer.testFun(instanceVal) != instanceVal) errors += "instanceVal"

        if (TypesServiceConsumer.testBooleanList(booleanListVal) != booleanListVal) errors += "booleanListVal"
        if (TypesServiceConsumer.testIntList(intListVal) != intListVal) errors += "intListVal"
        if (TypesServiceConsumer.testLongList(longListVal) != longListVal) errors += "longListVal"
        if (TypesServiceConsumer.testStringList(stringListVal) != stringListVal) errors += "stringListVal"
        TypesServiceConsumer.testByteArrayList(byteArrayListVal).forEachIndexed { index, bytes ->
            if (!bytes.contentEquals(byteArrayListVal[index])) errors += "byteArrayListVal"
        }
        if (TypesServiceConsumer.testUuidList(uuidListVal) != uuidListVal) errors += "uuidListVal"
        if (TypesServiceConsumer.testInstanceList(instanceListVal) != instanceListVal) errors += "instanceListVal"
    }

    return if (errors.isEmpty()) "OK" else "Fail: ${errors.joinToString(", ")}"
}

interface TypesService : Service {

    suspend fun testFun() : Unit = service()

    suspend fun testFun(arg1: Boolean): Boolean = service()
    suspend fun testFun(arg1: Int): Int = service()
    suspend fun testFun(arg1: Long): Long = service()
    suspend fun testFun(arg1: String): String = service()
    suspend fun testFun(arg1: ByteArray): ByteArray = service()
    suspend fun testFun(arg1: UUID<TypesService>): UUID<TypesService> = service()
    suspend fun testFun(arg1: A): A = service()

    suspend fun testBooleanList(arg1: List<Boolean>): List<Boolean> = service()
    suspend fun testIntList(arg1: List<Int>): List<Int> = service()
    suspend fun testLongList(arg1: List<Long>): List<Long> = service()
    suspend fun testStringList(arg1: List<String>): List<String> = service()
    suspend fun testByteArrayList(arg1: List<ByteArray>): List<ByteArray> = service()
    suspend fun testUuidList(arg1: List<UUID<TypesService>>): List<UUID<TypesService>> = service()
    suspend fun testInstanceList(arg1: List<B>): List<B> = service()

}

object TypesServiceConsumer : TypesService, ServiceConsumer

class TypesServiceProvider : TypesService, ServiceProvider {

    override suspend fun testFun() = Unit

    override suspend fun testFun(arg1: Boolean) = arg1

    override suspend fun testFun(arg1: Int) = arg1

    override suspend fun testFun(arg1: Long) = arg1

    override suspend fun testFun(arg1: String) = arg1

    override suspend fun testFun(arg1: ByteArray) = arg1

    override suspend fun testFun(arg1: UUID<TypesService>) = arg1

    override suspend fun testFun(arg1: A) = arg1

    override suspend fun testBooleanList(arg1: List<Boolean>) = arg1

    override suspend fun testIntList(arg1: List<Int>) = arg1

    override suspend fun testLongList(arg1: List<Long>) = arg1

    override suspend fun testStringList(arg1: List<String>) = arg1

    override suspend fun testByteArrayList(arg1: List<ByteArray>) = arg1

    override suspend fun testUuidList(arg1: List<UUID<TypesService>>) = arg1

    override suspend fun testInstanceList(arg1: List<B>) = arg1

}

data class A(
    var b: Boolean = false,
    var i: Int = 0,
    var s: String = "",
    var l: MutableList<Int> = mutableListOf()
) {
    companion object : ProtoEncoder<A>, ProtoDecoder<A> {

        override fun decodeProto(message: ProtoMessage?): A {
            if (message == null) return A()

            println(message.dumpProto())

            return A(
                message.boolean(1),
                message.int(2),
                message.string(3),
                message.intList(4)
            )
        }

        override fun encodeProto(value: A): ByteArray =
            ProtoMessageBuilder()
                .boolean(1, value.b)
                .int(2, value.i)
                .string(3, value.s)
                .intList(4, value.l)
                .pack()
    }
}

data class B(
    var a: A = A(),
    var s: String = ""
) {
    companion object : ProtoEncoder<B>, ProtoDecoder<B> {

        override fun decodeProto(message: ProtoMessage?): B {
            if (message == null) return B()

            return B(
                message.instance(1, A),
                message.string(2)
            )
        }

        override fun encodeProto(value: B): ByteArray =
            ProtoMessageBuilder()
                .instance(1, A, value.a)
                .string(2, value.s)
                .pack()
    }
}

class DumpTransport : ServiceCallTransport {
    override suspend fun <T> call(serviceName: String, funName: String, payload: ByteArray, decoder: ProtoDecoder<T>): T {
        println("==== REQUEST ====")
        println(serviceName)
        println(funName)
        println(payload.dumpProto())

        val service = requireNotNull(defaultServiceProviderRegistry[serviceName])

        val responseBuilder = ProtoMessageBuilder()

        service.dispatch(funName, ProtoMessage(payload), BasicServiceContext(), responseBuilder)

        val responsePayload = responseBuilder.pack()
        println("==== RESPONSE ====")
        println(responsePayload.dumpProto())
        println(decoder::class.qualifiedName)

        return decoder.decodeProto(ProtoMessage(responseBuilder.pack()))
    }
}