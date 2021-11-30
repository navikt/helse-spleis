plugins {
    kotlin("jvm") version "1.6.0"
}

val flywayVersion = "8.0.2"
val hikariVersion = "5.0.0"
val jacksonVersion = "2.13.0"
val junitJupiterVersion = "5.8.1"
val kotliqueryVersion = "1.6.0"
val vaultJdbcVersion = "1.3.9"
val kGraphQLVersion = "0.17.14"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
        implementation("ch.qos.logback:logback-classic:1.2.6")
        implementation("net.logstash.logback:logstash-logback-encoder:6.6") {
            exclude("com.fasterxml.jackson.core")
            exclude("com.fasterxml.jackson.dataformat")
        }
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion") {
            exclude("org.jetbrains.kotlin:kotlin-reflect")
        }
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

        implementation("com.zaxxer:HikariCP:$hikariVersion")
        implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
        implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
        implementation("org.flywaydb:flyway-core:$flywayVersion")

        implementation("com.apurebase:kgraphql:$kGraphQLVersion")
        implementation("com.apurebase:kgraphql-ktor:$kGraphQLVersion")

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

        testImplementation("org.testcontainers:postgresql:1.16.2")
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "16"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "16"
        }

        withType<Wrapper> {
            gradleVersion = "7.3"
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks {
        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
            }
        }
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    }
}
