import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    id("com.bmuschko.docker-remote-api") version "9.4.0"
}

val mainClass = "no.nav.helse.opprydding.AppKt"
val tbdLibsVersion = "2024.11.25-10.59-6f263a10"

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
    implementation(project(":sykepenger-utbetaling"))
    implementation("com.github.navikt.tbd-libs:naisful-postgres:$tbdLibsVersion")

    testImplementation(project(":sykepenger-mediators")) // for å få  tilgang på db/migrations-filene
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
    testImplementation(libs.bundles.flyway)
    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
    finalizedBy(":sykepenger-opprydding-dev:remove_spleis_opprydding_db_container")

}

tasks.create("remove_spleis_opprydding_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-opprydding-dev")
    dependsOn(":sykepenger-opprydding-dev:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
