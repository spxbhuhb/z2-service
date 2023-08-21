# Z2 Service

[![Maven Central](https://img.shields.io/maven-central/v/hu.simplexion.z2/z2-service-runtime)](https://mvnrepository.com/artifact/hu.simplexion.z2/z2-service-runtime)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
![Kotlin](https://img.shields.io/github/languages/top/spxbhuhb/z2-service)

Client-server communication for Kotlin Multiplatform with the absolute minimum of boilerplate. Part of [Z2](https://github.com/spxbhuhb/z2).

The library has a runtime part and a Kotlin compiler plugin that transforms the code.

| Platform | Status                                                                                              |
|----------|-----------------------------------------------------------------------------------------------------|
| JVM      | **experimental**                                                                                    |
| JS       | **experimental**                                                                                    |
| Android  | **unverified** probably works                                                                       |
| iOS      | **not yet** `fourRandomInt` should be implemented in commons, no idea if IR works on iOS or not.    |
| Native   | **not yet** `fourRandomInt` should be implemented in commons, no idea if IR works on Native or not. |

## Getting started

* [Overview](#Overview)
* [Gradle Setup](#gradle-setup)
* [Example Project](https://github.com/spxbhuhb/z2-service-example)

If you refresh the plugin version you have to run `gradle clean` to force code refresh.

Compressed example:

```kotlin
// commonMain - client and server
interface HelloService : Service {
    suspend fun hello(myName : String) : String
}

// jsMain or jvmMain - client
val hello = getService<HelloService>()

// jvmMain - server
class HelloServiceImpl : HelloService, ServiceImpl {
    override suspend fun hello(myName: String): String {
      return "Hello $myName!"
    }
}

// testing - this one for jvmMain
fun main() {
    defaultServiceImplFactory += HelloServiceImpl()
    runBlocking {
        println(Hello.hello("World"))
    }
}
```

## Overview

When using services we work with:

* service definitions (the API)
* service consumers
* service implementations
* service transports

### Service Definitions

Service definitions describe the communication between the client and the server. They are pretty straightforward:
create an interface that extends `Service` and define the functions the service provides.

```kotlin
interface HelloService : Service {
    
    suspend fun hello(myName : String) : String

}
```

### Service Consumers

Service consumers let the clients use the service. Use the `getService` function to 
get a consumer instance.

The compiler plugin generates all the code for the client side, you simply call the functions.

Definition: 
```kotlin
val hello = getService<HelloService>()
```

Call example (this uses the default service transport, more about that later):

```kotlin
fun main() {
    runBlocking {
        println(hello.hello("World"))
    }
}
```

### Service Implementations

On the server side create a service implementation that does whatever this service should do:

```kotlin
class HelloServiceImpl : HelloService, ServiceImpl {

    override suspend fun hello(myName: String): String {
        return "Hello $myName!"
    }

}
```

Register the service implementation during application startup, so the server knows that they are available.

Use [defaultServiceImplFactory](./z2-service-runtime/src/commonMain/kotlin/hu/simplexion/z2/service/runtime/globals.kt)
or implement your own way to store the services. These factories are used by the transports to find the service.

```kotlin
defaultServiceImplFactory += HelloServiceImpl()
```

It is very important that a new service implementation instance is created for each service call. This might seem 
a bit of an overkill, but it makes the handling of the [service context](#Service-Context) very straightforward.

**IMPORTANT** This **DOES NOT WORK**. As each call gets a new instance `clicked` will be 0 all the time.

```kotlin
class ClickServiceImpl : ClickService, ServiceImpl {

    val clicked = AtomicInteger(0)

    override suspend fun click(): Int {
      return clicked.incrementAndGet()
    }
}
```

Replace the code above with this, this works:

```kotlin
val clicked = AtomicInteger(0)

class ClickServiceImpl : ClickService, ServiceImpl {
    override suspend fun click(): Int {
        return clicked.incrementAndGet()
    }
}
```

#### Service Context

Most cases you need authorization and session data on the server side. Services provide a `serviceContext`.
This context may contain the identity of the user, along with other information.

Each service implementation call gets the service context which is reachable in the `serviceContext` property:

```kotlin
class HelloServiceImpl : HelloService, ServiceImpl {

    override suspend fun hello(myName: String): String {
        if (serviceContext.isAnonymous) {
            return "Sorry, I can talk only with clients I know."
        } else {
            return "Hello $myName! Your user id is: ${serviceContext.owner}."
        }            
    }
    
    override suspend fun login(email : String, password : String) : String {
        if (authenticate(email, password)) serviceContext.owner = myId
    }

    override suspend fun logout() : String {
        serviceContext.owner = null
    }

}
```

Type of `serviceContext` is `ServiceContext`:

```kotlin
interface ServiceContext {
    val uuid: UUID<ServiceContext>
    var data : MutableMap<Any,Any?>
}
```

Your transport may use your choice of class for the actual service context. There is a `BasicServiceContext` which is the
barest implementation:

```kotlin
data class BasicServiceContext(
    override val uuid: UUID<ServiceContext> = UUID(),
    override var data : MutableMap<Any,Any?> = mutableMapOf()
) : ServiceContext
```

### Service Transports

Transports move the call arguments and the return values between the client and the server. The library uses Protocol Buffers as 
transport format, but it does not really care about how the packets reach the other side.

There is a very basic transport implementation for Ktor in the `z2-service-ktor` module. This module has to be added to the project
as a dependency to use them:

```kotlin
implementation("hu.simplexion.z2:z2-service-ktor:${z2_service_version}")
```

#### Client Side

The [defaultServiceCallTransport](z2-service-runtime/src/commonMain/kotlin/hu/simplexion/z2/service/runtime/globals.kt)
global variable contains the transport. Set this during application startup as the example shows below.

[BasicWebSocketServiceTransport](z2-service-ktor/src/commonMain/kotlin/hu/simplexion/z2/service/ktor/client/BasicWebSocketServiceTransport.kt)
from `z2-service-ktor` provides a basic web socket transport implementation for clients.

```kotlin
defaultServiceCallTransport = BasicWebSocketServiceTransport(
    window.location.hostname,
    window.location.port.toInt(),
    "/z2/services"
).also {
    it.start()
}
```

#### Server Side

[Routing.basicWebSocketServiceDispatcher](z2-service-ktor/src/jvmMain/kotlin/hu/simplexion/z2/service/ktor/server/basic.kt)
from `z2-service-ktor` provides a basic web socket dispatcher implementation for Ktor.

With other servers you can write your own service provider based on `basicWebSocketServiceDispatcher`.

Full example of basic server setup:

```kotlin
fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(20)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    defaultServiceImplFactory += HelloServiceImpl()

    routing {
        basicWebsocketServiceCallTransport("/z2/services")
    }
}
```

## Supported Data Types

Services support the following data types as function parameters and return values.

### Nulls

`null`s are supported as parameter and/or return values, except nullable items in lists (the lists
themselves can be null).

While I can imagine proper use cases for nullable list items it would require considerable
effort to support them. I might add it when the need arises, but it's not planned for now.

### Simple Types

* `Unit`
* `Boolean`
* `Int`
* `Long`
* `String`
* `ByteArray`
* `Duration` (from `kotlin.time`)
* `Instant` (from `kotlinx.datetime`)
* `LocalDate` (from `kotlinx.datetime`)
* `LocalDateTime` (from `kotlinx.datetime`)
* `UUID` (from Z2 Commons)

### Enum Classes

All enum classes work out-of-the box, no modifications needed.

### Composite Types

[Z2 Schematic](https://github.com/spxbhuhb/z2-schematic) classes work out-of-the-box.

Any other classes that support Protocol Buffer decoding/encoding:

* the companion object of the class implements the `ProtoEncoder<T>` and `ProtoDecoder<T>` interface

### Collections

* `List` of any simple or composite type

## Gradle Setup

Gradle plugin dependency (build.gradle.kts):

```kotlin
plugin {
    id("hu.simplexion.z2.service") version "<z2-service-version>"
}
```

Runtime dependency (build.gradle.kts):

```kotlin
val commonMain by getting {
    dependencies {
        implementation("hu.simplexion.z2:z2-service-runtime:${z2_service_version}")
    }
}
```

For Ktor transport and dispatcher: 

```kotlin
implementation("hu.simplexion.z2:z2-service-ktor:${z2_service_version}")
```

## A Kind of Magic

So, how does this work? Actually, it is pretty simple.

You can't see code like the ones below as it is added during compilation. There are a few manually written
examples between the [tests](z2-service-runtime/src/jvmTest/kotlin/hu/simplexion/z2/service/runtime).

### Client Side Transform

For each interface that extends `Service` the compiler plugin generates a class. If your interface is
called `Hello`, the class name will be `Hello$Consumer`.

```kotlin
package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.protobuf.ProtoOneString

class Hello$Consumer : Hello {

    override suspend fun testFun(arg1: Int, arg2: String): String =
        defaultServiceCallTransport
            .call(
                serviceName,
                "testFun;IS", // signature of the function, so same name may be used with different parameters
                ProtoMessageBuilder() // this is the payload to send to the service
                    .int(1, arg1)
                    .string(2, arg2)
                    .pack(),
                ProtoOneString // this is a decoder that will decode the response
            )

}
```

When you call `getService`, the plugin replaces the parameter with a new instance of the generated consumer class:

```kotlin
val hello = getService<Hello>(Hello$Consumer())
```

### Server Side Transform

The server side transform is a bit trickier, mostly because we need information for authorization.

When a class implements `ServiceImpl`, the plugin:

* adds a `dispatch` function that handles dispatch of the incoming calls
* adds a `newInstance` function that creates a new instance of the implementation
* adds a new constructor with the service context as a parameter
* replaces the code of the original constructor, so it calls the new one with `null` as service context

```kotlin
package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceImpl

class TestServiceImpl : TestService, ServiceImpl {

    override suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        response : ProtoMessageBuilder
    ) {
        when (funName) {
            "testFun" -> response.string(1, testFun(payload.int(1), payload.string(2)))
            else -> throw IllegalStateException("unknown function: $funName")
        }
    }

    suspend fun testFun(arg1: Int, arg2: String): String {
        return "i:$arg1 s:$arg2 $serviceContext"
    }

}
```

## Plugin Development

The repo contains a composite gradle build. When cloning with IntelliJ the IDE
asks about importing the Gradle project. As of now this imports 3 of the 4 projects,
`z2-service-kotlin-plugin` has to be linked manually but clicking on `settings.gradle.kts`
and selecting the "Link Gradle Project" item.

| Project                    | Content                            |
|----------------------------|------------------------------------|
| `z2-service-runtime`       | runtime part of the library        |
| `z2-service-kotlin-plugin` | source code of the compiler plugin |
| `z2-service-gradle-plugin` | source code of the gradle plugin   |
| `z2-service-ktor`          | Ktor specific implementations      |

* The compiler plugin uses the standard [Kotlin compiler test infrastructure](https://github.com/JetBrains/kotlin/blob/bebb9b13924660160226a85cde59b6f30c72feec/compiler/test-infrastructure/ReadMe.md).
* To run the compiler plugin tests you have to run the `z2-service-runtime:shadowJar` beforehand.

## License

> Copyright (c) 2023 Simplexion Kft, Hungary and contributors
>
> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this work except in compliance with the License.
> You may obtain a copy of the License at
>
>    http://www.apache.org/licenses/LICENSE-2.0
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
