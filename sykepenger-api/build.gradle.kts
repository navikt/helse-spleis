import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    alias(libs.plugins.docker.remote.api)
}

val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))
    implementation(project(":sykepenger-api-rest"))
    implementation(project(":sykepenger-api-dto"))
    implementation(libs.tbd.naisful.app)
    implementation(libs.tbd.naisful.postgres)
    implementation(libs.tbd.azure.token.client.default)
    implementation(libs.tbd.retry)
    implementation(libs.tbd.speed.client)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.bundles.database)
    implementation(libs.cloudsql)

    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.auth.jwt) {
        exclude(group = "junit")
    }

    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(project(":sykepenger-mediators")) // for å få tilgang på db/migrations-filene
    testImplementation(libs.bundles.flyway)

    testImplementation(libs.tbd.naisful.test.app)
    testImplementation(libs.awaitility)
    testImplementation(libs.mockk)
    testImplementation(libs.jsonassert)

    testImplementation(libs.tbd.mock.http.client)
    testImplementation(libs.tbd.postgres.testdatabaser)
    testImplementation(libs.tbd.signed.jwt.issuer.test)

    testImplementation(libs.spekemat.fabrikk)

    // for å kunne gjenopprette personer fra json
    testImplementation(project(":sykepenger-serde"))
}

tasks {
    val copyJars = register("copy-jars") {
        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
    get("build").finalizedBy(copyJars)

    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }
        finalizedBy(":sykepenger-api:remove_spleis_api_db_container")
    }

    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}

docker {
    url = if (System.getenv("CI") == "true") {
        "unix:///var/run/docker.sock"
    } else {
        val home = System.getProperty("user.home")
        val candidates = listOf(
            "$home/.colima/default/docker.sock",       // Colima
            "/var/run/docker.sock",                    // Docker Desktop (Linux) / standard
            "$home/.docker/run/docker.sock",           // Docker Desktop (macOS)
        )
        val socket = candidates.firstOrNull { File(it).exists() }
            ?: error("No Docker socket found. Is Docker running?")
        "unix://$socket"
    }
}
tasks.register("remove_spleis_api_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-api")
    dependsOn(":sykepenger-api:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
