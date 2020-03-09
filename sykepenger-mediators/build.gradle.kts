val ktorVersion = "1.2.4"
val flywayVersion = "6.0.0-beta"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.0"
val wireMockVersion = "2.23.2"
val mockkVersion = "1.9.3"
val micrometerRegistryPrometheusVersion = "1.1.5"
val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation("no.nav.helse:rapids-and-rivers:1.56d085f")
    implementation(project(":sykepenger-model"))

    implementation("io.ktor:ktor-jackson:$ktorVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    implementation("io.ktor:ktor-auth-jwt:$ktorVersion") {
        exclude(group = "junit")
    }

    testImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2019.10.15-02-33-local-build")
    testImplementation("no.nav.syfo.kafka:sykepengesoknad:0b2a259676f7a78da70d65838851b05925d6de6f")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.1")

    testImplementation("org.awaitility:awaitility:3.1.6")
    testImplementation("no.nav:kafka-embedded-env:2.3.0")

    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.github.tomakehurst:wiremock:$wireMockVersion") {
        exclude(group = "junit")
    }
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    maven("https://kotlin.bintray.com/ktor")
    maven("http://packages.confluent.io/maven/")

    maven {
        url = uri("https://maven.pkg.github.com/navikt/rapids-and-rivers")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("app")

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
