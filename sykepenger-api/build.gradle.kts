val micrometerRegistryPrometheusVersion = "1.9.4"
val ktorVersion = "2.1.1"
val wireMockVersion = "2.31.0"
val awaitilityVersion = "4.1.1"
val mockVersion = "1.12.4"

val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-primitiver"))
    implementation(project(":sykepenger-etterlevelse"))
    implementation(project(":sykepenger-utbetaling"))
    implementation(project(":sykepenger-aktivitetslogg"))

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    // Midledertidig løsnings, pga problemer med KGraphQL og ktor 2.0 se: https://github.com/aPureBase/KGraphQL/issues/185
    // Etter evt fix fra apurebase Legg tilbake importerne:
    // implementation("com.apurebase:kgraphql:$kGraphQLVersion")
    // implementation("com.apurebase:kgraphql-ktor:$kGraphQLVersion")
    implementation("com.github.untoldwind.KGraphQL:kgraphql:0.17.14-fork-8")
    implementation("com.github.untoldwind.KGraphQL:kgraphql-ktor:0.17.14-fork-8")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation(libs.flyway)
    testImplementation(libs.testcontainers) {
        exclude("com.fasterxml.jackson.core")
    }
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("com.github.tomakehurst:wiremock-jre8:$wireMockVersion") {
        exclude(group = "junit")
    }
    testImplementation("io.mockk:mockk:$mockVersion")

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
