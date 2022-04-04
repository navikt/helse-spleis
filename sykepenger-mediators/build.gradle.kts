val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:2022.04.04-22.16.0611abb2a604")
    implementation(project(":sykepenger-model"))
    testImplementation("org.testcontainers:postgresql:1.16.2") {
        exclude("com.fasterxml.jackson.core")
    }
    testImplementation("com.github.navikt:inntektsmelding-kontrakt:2020.04.06-ab8f786")
    testImplementation("com.github.navikt:syfokafka:2021.02.15-14.09-103a1544")
    testImplementation("io.mockk:mockk:1.12.0")
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
    }
}
