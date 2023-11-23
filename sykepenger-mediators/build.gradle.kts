import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    id("com.bmuschko.docker-remote-api") version "9.3.3"
}

val mainClass = "no.nav.helse.AppKt"

val innteksmeldingKontraktVersion = "2023.06.20-08-54-d1c6c"
val syfokafkaVersion = "2023.11.23-14.00-f17b7ee4"
val mockkVersion = "1.12.4"
val jsonSchemaValidatorVersion = "1.0.70"
val jsonassertVersion = "1.5.0"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.flyway)

    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }
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
