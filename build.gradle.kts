import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.2.2"
val jacksonVersion = "2.9.8"
val kafkaVersion = "2.3.0"
val flywayVersion = "6.0.0-beta"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.0"
val junitJupiterVersion = "5.4.0"
val mockkVersion = "1.9.3"
val micrometerRegistryPrometheusVersion = "1.1.5"
val mainClass = "no.nav.helse.AppKt"

plugins {
    `build-scan`
    kotlin("jvm") version "1.3.40"
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    compile(kotlin("stdlib"))
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("net.logstash.logback:logstash-logback-encoder:5.2")
    compile("io.ktor:ktor-server-netty:$ktorVersion")

    compile("io.ktor:ktor-jackson:$ktorVersion")

    compile("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    compile("org.apache.kafka:kafka-streams:$kafkaVersion")

    compile("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    compile("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    compile("org.flywaydb:flyway-core:$flywayVersion")
    compile("com.zaxxer:HikariCP:$hikariVersion")
    compile("no.nav:vault-jdbc:$vaultJdbcVersion")
    compile("com.github.seratch:kotliquery:$kotliqueryVersion")

    testCompile("com.opentable.components:otj-pg-embedded:0.13.1")

    testCompile("org.awaitility:awaitility:3.1.6")
    testCompile("no.nav:kafka-embedded-env:2.2.3")
    testCompile("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")

    testCompile("io.mockk:mockk:$mockkVersion")

    testCompile("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

repositories {
    mavenCentral()
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
        attributes["Class-Path"] = configurations["compile"].joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations["compile"].forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.4.1"
}
