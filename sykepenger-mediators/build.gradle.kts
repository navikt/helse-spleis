val flywayVersion = "6.3.1"
val hikariVersion = "3.4.2"
val vaultJdbcVersion = "1.3.1"
val kotliqueryVersion = "1.3.1"
val wireMockVersion = "2.23.2"
val mockkVersion = "1.9.3"
val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:1.809f148")
    implementation(project(":sykepenger-model"))

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")

    testImplementation("no.nav.sykepenger.kontrakter:inntektsmelding-kontrakt:2019.10.15-02-33-local-build")
    testImplementation("no.nav.syfo.kafka:felles:2020.04.16-12.30-92d9c5cc")
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("io.mockk:mockk:$mockkVersion")
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    maven("https://kotlin.bintray.com/ktor")

    maven {
        url = uri("https://maven.pkg.github.com/navikt/rapids-and-rivers")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
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
