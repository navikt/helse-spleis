import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins { id("com.bmuschko.docker-remote-api") version "9.4.0" }

val mainClass = "no.nav.helse.AppKt"

val tbdLibsVersion = "2024.11.25-10.59-6f263a10"
val tbdSpillAvImMatchingVersion = "2024.11.06-14.54-41ea7ff3"
val syfokafkaVersion = "2024.10.21-14.49-932f4f94"
val mockkVersion = "1.13.9"
val jsonSchemaValidatorVersion = "1.0.70"
val jsonassertVersion = "1.5.0"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.bundles.flyway)
    implementation("com.github.navikt.tbd-libs:naisful-postgres:$tbdLibsVersion")

    testImplementation(libs.testcontainers) { exclude("com.fasterxml.jackson.core") }
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:$tbdLibsVersion")
    testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
    testImplementation("com.github.navikt.spill_av_im:matching:$tbdSpillAvImMatchingVersion")
    testImplementation("no.nav.helse.flex:sykepengesoknad-kafka:$syfokafkaVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
}

val copyJars =
    tasks.create("copy-jars") {
        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }

tasks.get("build").finalizedBy(copyJars)

tasks.withType<Jar> {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] =
            configurations.runtimeClasspath.get().joinToString(separator = " ") { it.name }
    }
    finalizedBy(":sykepenger-mediators:remove_spleis_mediators_db_container")
}

tasks.withType<Test> {
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
}

tasks.create("remove_spleis_mediators_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-mediators")
    dependsOn(":sykepenger-mediators:test")
    setProperty("force", true)
    onError { if (!this.message!!.contains("No such container")) throw this }
}
