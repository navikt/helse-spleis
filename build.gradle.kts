import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

val flywayVersion = "6.5.0"
val hikariVersion = "3.4.5"
val jacksonVersion = "2.12.0"
val junitJupiterVersion = "5.6.2"
val kotliqueryVersion = "1.3.1"
val vaultJdbcVersion = "1.3.1"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.0")
        implementation("ch.qos.logback:logback-classic:1.2.3")
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

        testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    }

    tasks {
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "14"
        }

        named<KotlinCompile>("compileTestKotlin") {
            kotlinOptions.jvmTarget = "14"
        }

        withType<Wrapper> {
            gradleVersion = "6.7"
        }
    }
}

repositories {
    jcenter()
}

val githubUser: String by project
val githubPassword: String by project

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        jcenter()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/inntektsmelding-kontrakt")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }

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
