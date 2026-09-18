import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    alias(libs.plugins.docker.remote.api)
}

val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.bundles.flyway)
    implementation(libs.tbd.naisful.postgres)

    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(libs.tbd.rapids.and.rivers.test)
    testImplementation(libs.tbd.postgres.testdatabaser)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.spill.av.im.matching)
    testImplementation(libs.syfokafka)
    testImplementation(libs.mockk)
    testImplementation(libs.jsonassert)
}

val copyJars = tasks.register("copy-jars") {
    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.get("build").finalizedBy(copyJars)

tasks.withType<Jar> {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }
    finalizedBy(":sykepenger-mediators:remove_spleis_mediators_db_container")
}

tasks.withType<Test> {
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
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
tasks.register("remove_spleis_mediators_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-mediators")
    dependsOn(":sykepenger-mediators:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
