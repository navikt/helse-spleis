val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))
    implementation(project(":sykepenger-api-rest"))
    implementation(project(":sykepenger-api-dto"))
    implementation(libs.tbd.naisful.app)
    implementation(libs.tbd.naisful.postgres)
    implementation(libs.tbd.azure.token.client.default)
    implementation(libs.tbd.retry)
    implementation(libs.tbd.speed.client)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.bundles.database)
    implementation(libs.cloudsql)

    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.auth.jwt) {
        exclude(group = "junit")
    }

    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(project(":sykepenger-mediators")) // for å få tilgang på db/migrations-filene
    testImplementation(libs.bundles.flyway)

    testImplementation(libs.tbd.naisful.test.app)
    testImplementation(libs.awaitility)
    testImplementation(libs.mockk)
    testImplementation(libs.jsonassert)

    testImplementation(libs.tbd.mock.http.client)
    testImplementation(libs.tbd.signed.jwt.issuer.test)

    testImplementation(libs.spekemat.fabrikk)

    // for å kunne gjenopprette personer fra json
    testImplementation(project(":sykepenger-serde"))
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
