val jsonassertVersion = "1.5.0"

dependencies {
    api(project(":sykepenger-primitiver"))
    api(project(":sykepenger-utbetaling"))
    api(project(":sykepenger-aktivitetslogg"))
    api(project(":sykepenger-etterlevelse"))
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }
    testImplementation(testFixtures(project(":sykepenger-utbetaling")))
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
