val testcontainersVersion = "1.16.3"
val cloudSqlVersion = "1.4.4"

val mainClass = "no.nav.helse.AppKt"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.navikt:rapids-and-rivers:2022.04.05-09.40.11a466d7ac70")
    implementation("com.google.cloud.sql:postgres-socket-factory:$cloudSqlVersion")
    implementation("org.postgresql:postgresql:42.3.3")

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