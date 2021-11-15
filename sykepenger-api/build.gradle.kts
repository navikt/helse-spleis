val micrometerRegistryPrometheusVersion = "1.7.5"
val ktorVersion = "1.5.0"
val wireMockVersion = "2.31.0"

val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))

    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")


    implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("org.awaitility:awaitility:4.1.1")
    testImplementation("com.github.tomakehurst:wiremock-jre8:$wireMockVersion") {
        exclude(group = "junit")
    }
    testImplementation("io.mockk:mockk:1.12.0")

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
