import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    id("com.bmuschko.docker-remote-api") version "9.3.3"
}

val micrometerRegistryPrometheusVersion = "1.11.4"
val ktorVersion = "2.3.4"
val wireMockVersion = "2.35.0"
val awaitilityVersion = "4.2.0"
val mockVersion = "1.13.4"
val kGraphQLVersion = "0.19.0"
val jsonassertVersion = "1.5.1"

val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation(project(":sykepenger-mediators")) // for å få tilgang på db/migrations-filene
    testImplementation(libs.flyway)
    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("com.github.tomakehurst:wiremock-jre8:$wireMockVersion") {
        exclude(group = "junit")
    }
    testImplementation("io.mockk:mockk:$mockVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")

    testImplementation("com.apurebase:kgraphql:$kGraphQLVersion")
    testImplementation("com.apurebase:kgraphql-ktor:$kGraphQLVersion")
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
        finalizedBy(":sykepenger-api:remove_spleis_api_db_container")
    }
}

tasks.create("remove_spleis_api_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-api")
    dependsOn(":sykepenger-api:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
