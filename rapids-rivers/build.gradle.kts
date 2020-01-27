val ktorVersion = "1.2.4"
val kafkaVersion = "2.3.1"
val micrometerRegistryPrometheusVersion = "1.1.5"

plugins {
    id("java")
    id("maven-publish")
    id("org.sonarqube") version "2.7"
}

dependencies {
    api(kotlin("stdlib"))
    api("ch.qos.logback:logback-classic:1.2.3")
    api("net.logstash.logback:logstash-logback-encoder:5.2")
    api("io.ktor:ktor-server-netty:$ktorVersion")

    api("org.apache.kafka:kafka-clients:$kafkaVersion")

    api("io.ktor:ktor-metrics-micrometer:$ktorVersion")
    api("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryPrometheusVersion")

    testImplementation("no.nav:kafka-embedded-env:2.3.0")
    testImplementation("org.awaitility:awaitility:4.0.1")
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("http://packages.confluent.io/maven/")
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

version = properties["rapidVersion"] ?: "local-build"
val githubUser: String by project
val githubPassword: String by project

publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/navikt/helse-spleis")
            credentials {
                username = githubUser
                password = githubPassword
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {

            pom {
                name.set("rapids-rivers")
                description.set("Rapids and Rivers")
                url.set("https://github.com/navikt/helse-spleis")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/navikt/helse-spleis.git")
                    developerConnection.set("scm:git:https://github.com/navikt/helse-spleis.git")
                    url.set("https://github.com/navikt/helse-spleis")
                }
            }
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}
