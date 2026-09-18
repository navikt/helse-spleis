val mainClass = "no.nav.helse.opprydding.AppKt"

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
    implementation(project(":sykepenger-utbetaling"))
    implementation(libs.tbd.naisful.postgres)

    testImplementation(project(":sykepenger-mediators")) // for å få  tilgang på db/migrations-filene
    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(libs.tbd.rapids.and.rivers.test)
    testImplementation(libs.bundles.flyway)
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
            val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}
