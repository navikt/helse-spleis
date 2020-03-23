val ktorVersion = "1.2.4"
val micrometerRegistryPrometheusVersion = "1.3.5"
val jacksonVersion = "2.10.3"
val hikariVersion = "3.4.2"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.1"
val wireMockVersion = "2.23.2"
val flywayVersion = "6.3.1"
val mainClass = "no.nav.helse.spleis.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))

    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("net.logstash.logback:logstash-logback-encoder:5.2")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation("org.flywaydb:flyway-core:$flywayVersion")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.1")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("org.awaitility:awaitility:3.1.6")
    testImplementation("com.github.tomakehurst:wiremock:$wireMockVersion") {
        exclude(group = "junit")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
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
