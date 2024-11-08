import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    id("com.bmuschko.docker-remote-api") version "9.3.3"
}

val tbdLibsVersion = "2024.11.06-07.11-d5ab9e24"
val spekematVersion = "2024.03.07-12.49-d2ad6319"
val ktorVersion = "3.0.1"
val awaitilityVersion = "4.2.2"
val mockVersion = "1.13.13"
val jsonassertVersion = "1.5.3"

val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))
    implementation(project(":sykepenger-api-graphql"))
    implementation(project(":sykepenger-api-dto"))
    implementation("com.github.navikt.tbd-libs:naisful-app:$tbdLibsVersion")
    implementation("com.github.navikt.tbd-libs:azure-token-client-default:$tbdLibsVersion")
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.bundles.database)
    implementation(libs.cloudsql)

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation(project(":sykepenger-mediators")) // for å få tilgang på db/migrations-filene
    testImplementation(libs.bundles.flyway)
    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }

    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:$tbdLibsVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.wiremock:wiremock:3.3.1") {
        exclude(group = "junit")
    }
    testImplementation("io.mockk:mockk:$mockVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")

    testImplementation("com.github.navikt.tbd-libs:mock-http-client:$tbdLibsVersion")
    testImplementation("com.github.navikt.tbd-libs:postgres-testdatabaser:$tbdLibsVersion")

    testImplementation("com.github.navikt.spekemat:fabrikk:$spekematVersion")

    // for å kunne gjenopprette personer fra json
    testImplementation(project(":sykepenger-serde"))
}

tasks {
    val copyJars = create("copy-jars") {
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

tasks.create("remove_spleis_api_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-api")
    dependsOn(":sykepenger-api:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
