val jsonassertVersion = "1.5.0"
val jacksonVersion = "2.14.0"

dependencies {
    implementation(project(":sykepenger-primitiver"))
    implementation(project(":sykepenger-utbetaling"))
    testImplementation(testFixtures(project(":sykepenger-utbetaling")))
    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    implementation(project(":sykepenger-aktivitetslogg"))
    implementation(project(":sykepenger-etterlevelse"))
    implementation(project(":sykepenger-inntekt"))
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}
