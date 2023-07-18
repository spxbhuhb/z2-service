# Z2 Service

[![Maven Central](https://img.shields.io/maven-central/v/hu.simplexion.z2/z2-service)](https://mvnrepository.com/artifact/hu.simplexion.z2/z2-service)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
![Kotlin](https://img.shields.io/github/languages/top/spxbhuhb/z2-service)

Client-server communication with the absolute minimum amount of boilerplate. Part of [Z2](https://github.com/spxbhuhb/z2)

Status: **planning, no coding has been done yet**

The library has a runtime part and a Kotlin compiler plugin that transforms the code.

## Overview

When using services we work with:

* service definitions
* service consumers
* service providers
* service transports

### Service Definitions

Service definitions describe the communication between the client and the server. They are pretty straightforward, you define
and interface that extends the `Service` interface and define the functions the service provides.

All functions must have the `= service()` default implementation set.

```kotlin
interface HelloService : Service {
    
    suspend fun hello(myName : String) : String = service()

}
```

### Service Consumers

When the client wants to use the service it defines a service consumer. This is also pretty easy.
The compiler plugin generates all the code for client side, you can simply call the service.
This simple definition uses the default service transport (more about that later).

```kotlin
object Hello : HelloService, ServiceConsumer

fun main() {
    localLaunch {
        document.body !!.innerText = Hello.hello("World")
    }
}
```

### Service Providers

On the server side you need a service provider that does whatever this service should do. You also have to register
this provider, so the clients will be able to reach it (details in the transports section).

```kotlin
class HelloServiceProvider : HelloService, ServiceProvider {

    override suspend fun hello(myName: String): String {
        return "Hello $myName"
    }

}
```

### Service Transports

Transports move the service request and response between the server and the client. The library uses Protocol Buffers
as transport format.

#### Client Side

The `defaultServiceTransport` global variable contains the transport.

In browsers this is automatically set to `DefaultWebSocketServiceTransport` which connects to the server from where
the web page is opened with the path `/z2/services`.

#### Server Side

With Ktor you can use the `Routing.defaultWebSocketServiceDispatcher` to install a service provider on `/z2/services`.

With other servers you can write your own service provider based on `defaultWebSocketServiceDispatcher`, it's pretty easy.

You also have to add your providers to the list of know service providers. Here is a full example:

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

    serviceProviderRegistry += HelloServiceProvider()

    routing {
        defaultWebSocketServiceDispatcher("/z2/services")
    }
}
```

## Gradle Setup

Gradle plugin repository (settings.gradle.kts, temporary until Gradle registers the plugin):

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
    }
}
```

Gradle plugin dependency (build.gradle.kts):

```kotlin
plugin {
    id("hu.simplexion.z2.service") version "--not released yet--"
}
```

Runtime dependency (build.gradle.kts):

```kotlin
val commonMain by getting {
    dependencies {
        implementation("hu.simplexion.z2:z2-service-runtime:--not released yet--")
    }
}
```

## A Kind of Magic

So, how does this work? Actually, it is pretty simple. The compiler plugin does two things:

* on the client side it creates an override for the service functions
* on the server side it adds a `dispatch` function and creates an override for the service functions

### Client Side Transform

When you extend a service interface (one that extends `Service`) the plugin creates 
something like this:

```kotlin
object Hello : HelloService, ServiceConsumer {

    override suspend fun hello(myName: String): String =
        defaultServiceTransport
            .serviceCall(this::class.qualifiedName, "hello")
            .string(3, myName)
            .execute()
            .string(3)

}
```

### Server Side Transform

```kotlin
class HelloServiceProvider : HelloService, ServiceProvider {

    override suspend fun dispatch(records : List<ProtoRecord>) : LenProtoRecord {
        when (records[1].string()) {
            "hello" -> hello(records[2].string()).toLenRecord()
        }
    }
    
    override suspend fun hello(myName: String): String {
        throw NotImplementedError()
    }

}
```