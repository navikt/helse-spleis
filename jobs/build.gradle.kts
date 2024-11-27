val mainClass = "no.nav.helse.spleis.jobs.AppKt"
val tbdLibsVersion = "2024.11.25-10.59-6f263a10"
val jacksonVersion = "2.18.1"

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation("com.github.navikt.tbd-libs:kafka:$tbdLibsVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))
    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
}

tasks {
    val copyJars =
        create("copy-jars") {
            doLast {
                configurations.runtimeClasspath.get().forEach {
                    val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                    if (!file.exists()) {
                        it.copyTo(file)
                    }
                }
            }
        }
    get("build").finalizedBy(copyJars)

    withType<Jar> {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }
    }
}
