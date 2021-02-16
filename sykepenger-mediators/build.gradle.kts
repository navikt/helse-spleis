val flywayVersion = "6.5.0"
val hikariVersion = "3.4.5"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.1"
val wireMockVersion = "2.27.1"
val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:1.880e7a5")
    implementation(project(":sykepenger-model"))

    testImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2019.10.15-02-33-local-build")
    testImplementation("no.nav.syfo.kafka:felles:2021.02.15-14.09-103a1544")
    testImplementation("io.mockk:mockk:1.10.0")
}

repositories {
    maven("https://jitpack.io")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

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
    }
}
