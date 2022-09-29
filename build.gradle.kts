plugins {
    kotlin("jvm") version "1.7.0"
}

val jacksonVersion = "2.13.4"
val junitJupiterVersion = "5.9.0"
val logbackClassicVersion = "1.2.11"
val logstashVersion = "7.2"
val jvmTargetVersion = "17"

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"

    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    /*
        avhengigheter man legger til her blir lagt på -alle- prosjekter.
        med mindre alle submodulene (modellen, apiet, jobs, osv) har behov for samme avhengighet,
        bør det heller legges til de enkelte som har behov.
        Dersom det er flere som har behov så kan det være lurt å legge avhengigheten til
         dependencyResolutionManagement i settings.gradle.kts
     */
    dependencies {
        implementation("ch.qos.logback:logback-classic:$logbackClassicVersion")
        implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion") {
            exclude("com.fasterxml.jackson.core")
            exclude("com.fasterxml.jackson.dataformat")
        }
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

        testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
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
            maxHeapSize = "6G"
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
