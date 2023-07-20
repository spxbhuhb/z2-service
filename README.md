# Z2 Service

[![Maven Central](https://img.shields.io/maven-central/v/hu.simplexion.z2/z2-service)](https://mvnrepository.com/artifact/hu.simplexion.z2/z2-service)
[![GitHub License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
![Kotlin](https://img.shields.io/github/languages/top/spxbhuhb/z2-service)

Client-server communication with the absolute minimum of boilerplate. Part of [Z2](https://github.com/spxbhuhb/z2).

Status: **initial development**

The library has a runtime part and a Kotlin compiler plugin that transforms the code.

## Overview

When using services we work with:

* service definitions
* service consumers
* service providers
* service transports

### Service Definitions

Service definitions describe the communication between the client and the server. They are pretty straightforward, you
create and interface that extends `Service` and define the functions the service provides.

All functions must have the `= service()` default implementation assigned.

```kotlin
interface HelloService : Service {
    
    suspend fun hello(myName : String) : String = service()

}
```

### Service Consumers

When the client wants to use the service it defines a service consumer. This is also pretty easy.
The compiler plugin generates all the code for the client side, you can simply call the functions.
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
this provider, so the server knows that it's there (details in the transports section).

```kotlin
class HelloServiceProvider : HelloService, ServiceProvider {

    override suspend fun hello(myName: String): String {
        return "Hello $myName!"
    }

}
```

#### Service Context

Most cases you need authorization on the server side. Services provide you a so-called `serviceContext`.
This context may contain the identity of the user, along with other information.

You can use `serviceContext` only inside the service functions. All other uses throw an exception.

```kotlin
class HelloServiceProvider : HelloService, ServiceProvider {

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

### Service Transports

Transports move the call arguments and the return values between the client and the server.
The library uses Protocol Buffers as transport format.

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

    defaultServiceProviderRegistry += HelloServiceProvider()

    routing {
        defaultWebSocketServiceDispatcher("/z2/services")
    }
}
```

## Supported Data Types

Services support these data types as function parameters and return values:

### Simple Types

* `Boolean`
* `Int`
* `Long`
* `String`
* `ByteArray`
* `UUID` (from Z2 Commons)

### Composite Types

[Z2 Schematic](https://github.com/spxbhuhb/z2-schematic) classes can be used with services out-of-the-box.

Any other classes that support Protocol Buffer decoding/encoding:

* the companion object of the class implements the `ProtoEncoder<T>` and `ProtoDecoder<T>` interface

### Collections

* `List` of any simple or composite type
* `MutableList` of any simple or composite type

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

So, how does this work? Actually, it is pretty simple.

You can't see code like the ones below as it is added during compilation. There are a few manually written
examples between the [tests](z2-service-runtime/src/jvmTest/kotlin/hu/simplexion/z2/service/runtime).

### Client Side Transform

When a class implements the `ServiceConsumer` interface, the plugin creates overrides for the service functions
like this:

```kotlin
package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.commons.protobuf.ProtoOneString

object TestServiceConsumer : TestService, ServiceConsumer {

    override suspend fun testFun(arg1: Int, arg2: String): String =
        defaultServiceCallTransport
            .call(
                serviceName,
                "testFun",
                ProtoMessageBuilder() // this is the payload to send to the service
                    .int(1, arg1)
                    .string(2, arg2)
                    .pack(),
                ProtoOneString // this is a decoder that will decode the response
            )

}
```

### Server Side Transform

The server side transform is a bit trickier, mostly because we need information for authorization.

When a class implements the `ServiceProvider` interface, the plugin:

* for each original service function (the ones you write)
  * removes the `override` modifier
  * adds a `serviceContext` argument
  * replaces all `ServiceProvider.serviceContext` property accesses to access the parameter added above 
  * creates a new function with the same name and arguments but without the `serviceContext` parameter
  * adds the `override` modifier to this new function
* adds a `dispatch` function that handles dispatch of the incoming calls

```kotlin
package hu.simplexion.z2.service.runtime

import hu.simplexion.z2.commons.protobuf.ProtoMessage
import hu.simplexion.z2.commons.protobuf.ProtoMessageBuilder
import hu.simplexion.z2.service.runtime.ServiceContext
import hu.simplexion.z2.service.runtime.ServiceProvider

class TestServiceProvider : TestService, ServiceProvider {

    override suspend fun dispatch(
        funName: String,
        payload: ProtoMessage,
        context: ServiceContext,
        response : ProtoMessageBuilder
    ) {
        when (funName) {
            "testFun" -> response.string(1, testFun(payload.int(1), payload.string(2), context))
            else -> throw IllegalStateException("unknown function: $funName")
        }
    }

    suspend fun testFun(arg1: Int, arg2: String, serviceContext : ServiceContext?): String {
        return "i:$arg1 s:$arg2 $serviceContext"
    }

    override suspend fun testFun(arg1: Int, arg2: String) =
        testFun(arg1, arg2, null)

}
```

### Considerations

There are other possible solutions for passing the context.

ThreadLocal would be able to pass the information. However, I felt that it would be really dangerous. The
main purpose of the context is authorization. I don't know how to make it sure that TreadLocal is properly
cleared all the time. Also, it would make elevation much-much harder (if we decide to implement it).

Adding a parameter to the function would also work. That parameter would be meaningless on the
client side and introduce visual clutter/boilerplate. That's exactly what I wanted to avoid.

Other possibility would be to use different definitions for the consumer and the provider. While that
would work, there would be no easy way to make sure that the definitions are aligned. Also, code 
analysis tools wouldn't realize that the two sides belong to each other.

When looking from application programming point of view, the current solution is the most comfortable
one, therefore that's the one implemented.

### Wire Formats

Both request and response use an envelope to encapsulate the metadata needed for routing.

#### Request

```protobuf
message ServiceCallRequestEnvelope {
    string id = 1;
    string serviceName = 2;
    string funName = 3;
    bytes payload = 4;
}
```

The payload contains the arguments of the call. For example:

```protobuf
message HelloRequestPayload {
    string myName = 1;
    string from = 2;
}
```

#### Response 

```protobuf
message ResponseEnvelope {
    string id = 1;
    int32 responseCode = 2;
    bytes payload = 3;
}
```

When there is no return value, the response payload is omitted.

When the return value is a primitive type, the field number is always `1`:

```protobuf
message HelloResponsePayload {
    string value = 1;
}
```

When the return value is a complex type, the field numbers are assigned by the encoder of the type:

```protobuf
message ComplexResponsePayload {
    string field1 = 1;
    int field2 = 2;
    OtherComplexType field3 = 3;
}
```

## License

> Copyright (c) 2020-2023 Simplexion Kft, Hungary and contributors
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
