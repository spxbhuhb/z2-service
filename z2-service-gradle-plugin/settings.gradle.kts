rootProject.name = "z2-service-gradle-plugin"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

includeBuild("../z2-service-runtime")