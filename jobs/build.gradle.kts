val mainClass = "no.nav.helse.spleis.jobs.AppKt"
val tbdLibsVersion = "2025.08.16-09.21-71db7cad"
val jacksonVersion = "2.18.3"

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
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.cloudsql)
    implementation(libs.tbd.sql)
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
    }
}
