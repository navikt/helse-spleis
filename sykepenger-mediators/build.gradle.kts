import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    id("com.bmuschko.docker-remote-api") version "9.3.3"
}

val mainClass = "no.nav.helse.AppKt"

val tbdLibsVersion = "2024.02.02-20.07-81f4fa76"
val innteksmeldingKontraktVersion = "2023.06.20-08-54-d1c6c"
val syfokafkaVersion = "2023.12.05-10.16-3ffa06f7"
val mockkVersion = "1.13.9"
val jsonSchemaValidatorVersion = "1.0.70"
val jsonassertVersion = "1.5.0"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.bundles.flyway)

    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }
    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:$tbdLibsVersion")
    testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
    testImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:$innteksmeldingKontraktVersion")
    testImplementation("no.nav.helse.flex:sykepengesoknad-kafka:$syfokafkaVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
}

tasks {
    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
        finalizedBy(":sykepenger-mediators:remove_spleis_mediators_db_container")
    }

    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}

tasks.create("remove_spleis_mediators_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-mediators")
    dependsOn(":sykepenger-mediators:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
