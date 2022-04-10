val mainClass = "no.nav.helse.spleis.jobs.AppKt"
val cloudSqlVersion = "1.4.4"

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers-cli:1.7927bf9")
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
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
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}
