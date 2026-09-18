import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    alias(libs.plugins.docker.remote.api)
}

val mainClass = "no.nav.helse.opprydding.AppKt"

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
    implementation(project(":sykepenger-utbetaling"))
    implementation(libs.tbd.naisful.postgres)

    testImplementation(project(":sykepenger-mediators")) // for å få  tilgang på db/migrations-filene
    testImplementation(libs.tbd.rapids.and.rivers.test)
    testImplementation(libs.tbd.postgres.testdatabaser)
    testImplementation(libs.bundles.flyway)
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
tasks.register("remove_spleis_opprydding_db_container", DockerRemoveContainer::class) {
    targetContainerId("spleis-opprydding-dev")
    dependsOn(":sykepenger-opprydding-dev:test")
    setProperty("force", true)
    onError {
        if (!this.message!!.contains("No such container"))
            throw this
    }
}
