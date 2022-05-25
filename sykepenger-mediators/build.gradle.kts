val mainClass = "no.nav.helse.AppKt"

val rapidsAndRiversVersion = "2022.05.19-14.18.e3dc97b518d8"
val testcontainersPostgresqlVersion = "1.17.1"
val innteksmeldingKontraktVersion = "2020.04.06-ab8f786"
val syfokafkaVersion = "2021.02.15-14.09-103a1544"
val mockkVersion = "1.12.4"

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation(project(":sykepenger-model"))
    testImplementation("org.testcontainers:postgresql:$testcontainersPostgresqlVersion") {
        exclude("com.fasterxml.jackson.core")
    }
    testImplementation("com.github.navikt:inntektsmelding-kontrakt:$innteksmeldingKontraktVersion")
    testImplementation("com.github.navikt:syfokafka:$syfokafkaVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
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
