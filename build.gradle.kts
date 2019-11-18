import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `build-scan`
    kotlin("jvm") version "1.3.50"
}

allprojects {
    group = "no.nav.helse"
    version = properties["version"] ?: "local-build"
}

repositories {
    mavenCentral()
}

val githubUser: String by project
val githubPassword: String by project


subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/navikt/inntektsmelding-kontrakt")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }

    dependencies {
        implementation(kotlin("stdlib"))

    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "12"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    tasks.withType<Wrapper> {
        gradleVersion = "5.6.4"
    }
}
dependencies {
    implementation(kotlin("stdlib"))
}