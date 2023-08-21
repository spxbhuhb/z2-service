
tasks.register("clean") {
    group = "build"
    dependsOn(gradle.includedBuilds.map { it.task(":clean") })
}

tasks.register("build") {
    group = "build"
    dependsOn(gradle.includedBuilds.map { it.task(":build") })
}

tasks.register("publishToMavenLocal") {
    group = "publishing"
    dependsOn(gradle.includedBuilds.map { it.task(":publishToMavenLocal") })
}

tasks.register("publish") {
    group = "publishing"
    dependsOn(gradle.includedBuilds.map { it.task(":publishMavenPublicationToMavenRepository") })
}