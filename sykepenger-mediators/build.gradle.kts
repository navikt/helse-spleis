val mainClass = "no.nav.helse.AppKt"

val innteksmeldingKontraktVersion = "2020.04.06-ab8f786"
val syfokafkaVersion = "2022.10.28-10.24-aa0eced7"
val mockkVersion = "1.12.4"
val jsonSchemaValidatorVersion = "1.0.70"
val jsonassertVersion = "1.5.0"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-utbetaling"))
    implementation(project(":sykepenger-etterlevelse"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.flyway)
    implementation(project(":sykepenger-primitiver"))
    implementation(project(":sykepenger-aktivitetslogg"))

    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }
    testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
    testImplementation("com.github.navikt:inntektsmelding-kontrakt:$innteksmeldingKontraktVersion")
    testImplementation("com.github.navikt:sykepengesoknad-kafka:$syfokafkaVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
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
