plugins {
    kotlin("jvm") version "1.5.21"
}

val flywayVersion = "7.12.0"
val hikariVersion = "5.0.0"
val jacksonVersion = "2.12.4"
val junitJupiterVersion = "5.7.2"
val kotliqueryVersion = "1.3.1"
val vaultJdbcVersion = "1.3.7"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.21")
        implementation("ch.qos.logback:logback-classic:1.2.5")
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

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

        testImplementation("com.opentable.components:otj-pg-embedded:0.13.4")
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "16"
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = "16"
        }

        withType<Wrapper> {
            gradleVersion = "7.1.1"
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
