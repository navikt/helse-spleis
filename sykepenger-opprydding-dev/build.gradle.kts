val testcontainersVersion = "1.17.1"
val cloudSqlVersion = "1.4.4"
val rapidsAndRiversVersion = "2022.05.19-14.18.e3dc97b518d8"
val postgresqlVersion = "42.3.3"

val mainClass = "no.nav.helse.AppKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.navikt:rapids-and-rivers:$rapidsAndRiversVersion")
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    testImplementation(project(":sykepenger-mediators"))
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion") {
        exclude("com.fasterxml.jackson.core")
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