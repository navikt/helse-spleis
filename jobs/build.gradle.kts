val mainClass = "no.nav.helse.spleis.jobs.AppKt"

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation(libs.tbd.kafka)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.cloudsql)
    implementation(libs.tbd.sql)
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
    }
}
