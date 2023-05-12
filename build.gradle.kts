import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer

plugins {
    kotlin("jvm") version "1.8.21"
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}

val junitJupiterVersion = "5.9.2"
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
            gradleVersion = "8.1.1"
        }
        withType<Jar> {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin="java-library")
    apply(plugin="java-test-fixtures")

    tasks {
        withType<Test> {
            maxHeapSize = "6G"
            useJUnitPlatform()
            testLogging {
                events("skipped", "failed")
            }
        }
    }
}

fun String.lagDockerOppgave(): String {
    val containerId = this
    tasks.create("remove_$containerId", DockerRemoveContainer::class) {
        targetContainerId(containerId)
        setProperty("force", true)
        //dependsOn("shut_down_$containerId")
        onError {
            if (!this.message!!.contains("No such container"))
                throw this
        }
    }
    return "remove_$containerId"
}
fun List<String>.lagDockerOppgaver() = map { it.lagDockerOppgave() }

tasks.create("nukular_docker_option") {
    val oppgaver = listOf(
        "spleis-opprydding-dev2",
        "spleis-mediators",
        "spleis-api",
        "spleis-api3",
        ).lagDockerOppgaver()
    this.dependsOn(oppgaver)
}