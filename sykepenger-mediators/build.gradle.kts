val ktorVersion = "1.2.4"
val jacksonVersion = "2.9.8"
val kafkaVersion = "2.3.0"
val flywayVersion = "6.0.0-beta"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.0"
val junitJupiterVersion = "5.5.2"
val wireMockVersion = "2.23.2"
val mockkVersion = "1.9.3"
val micrometerRegistryPrometheusVersion = "1.1.5"
val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("net.logstash.logback:logstash-logback-encoder:5.2")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("io.ktor:ktor-jackson:$ktorVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    implementation("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    implementation("no.nav.helse:sykdomstidslinje:1.f9e93b5")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2019.10.15-02-33-local-build")
    testImplementation("no.nav.syfo.kafka:sykepengesoknad:191a7a91c115dab0038a7063be52dfae34f76c3a")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.1")

    testImplementation("org.awaitility:awaitility:3.1.6")
    testImplementation("no.nav:kafka-embedded-env:2.2.3")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.github.tomakehurst:wiremock:$wireMockVersion") {
        exclude(group = "junit")
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    maven("https://kotlin.bintray.com/ktor")
    maven("http://packages.confluent.io/maven/")
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

tasks.named<Jar>("jar") {
    baseName = "app"

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
