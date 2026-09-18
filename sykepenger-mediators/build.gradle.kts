val mainClass = "no.nav.helse.AppKt"

dependencies {
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))

    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.bundles.flyway)
    implementation(libs.tbd.naisful.postgres)

    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(libs.tbd.rapids.and.rivers.test)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.spill.av.im.matching)
    testImplementation(libs.syfokafka)
    testImplementation(libs.mockk)
    testImplementation(libs.jsonassert)
}

val copyJars = tasks.register("copy-jars") {
    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.get("build").finalizedBy(copyJars)

tasks.withType<Jar> {
    archiveBaseName.set("app")

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }
}
