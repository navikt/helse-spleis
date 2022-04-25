plugins {
    kotlin("jvm") version "1.6.21"
}

val flywayVersion = "8.5.7"
val hikariVersion = "5.0.1"
val jacksonVersion = "2.13.2"
val junitJupiterVersion = "5.8.2"
val kotliqueryVersion = "1.7.0"
val kGraphQLVersion = "0.17.14"
val postgresqlVersion = "42.3.3"
val kotlinReflectVersion = "1.6.21"
val logbackClassicVersion = "1.2.11"
val logstashVersion = "7.0.1"
val jsonSchemaValidatorVersion = "1.0.68"
val jvmTargetVersion = "17"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        implementation("org.postgresql:postgresql:$postgresqlVersion")
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinReflectVersion")
        implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
        implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion") {
            exclude("com.fasterxml.jackson.core")
            exclude("com.fasterxml.jackson.dataformat")
        }
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion") {
            exclude("org.jetbrains.kotlin:kotlin-reflect")
        }
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

        implementation("com.zaxxer:HikariCP:$hikariVersion")
        implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
        implementation("org.flywaydb:flyway-core:$flywayVersion")

        // Midledertidig løsnings, pga problemer med KGraphQL og ktor 2.0 se: https://github.com/aPureBase/KGraphQL/issues/185
        // Etter evt fix fra apurebase Legg tilbake importerne:
        // implementation("com.apurebase:kgraphql:$kGraphQLVersion")
        // implementation("com.apurebase:kgraphql-ktor:$kGraphQLVersion")
        implementation("com.github.untoldwind.KGraphQL:kgraphql:0.17.14-fork-5")
        implementation("com.github.untoldwind.KGraphQL:kgraphql-ktor:0.17.14-fork-5")

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
        testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = jvmTargetVersion
        }

        compileTestKotlin {
            kotlinOptions.jvmTarget = jvmTargetVersion
        }

        withType<Wrapper> {
            gradleVersion = "7.4.2"
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
