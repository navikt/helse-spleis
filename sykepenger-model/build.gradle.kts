val jsonassertVersion = "1.5.0"
val tbdSpillAvImMatchingVersion = "2025.04.08-12.41-e519e1f8"

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-model-dto"))

    api(project(":sykepenger-primitiver"))
    api(project(":sykepenger-utbetaling"))
    api(project(":sykepenger-økonomi"))
    api(project(":sykepenger-aktivitetslogg"))
    api(project(":sykepenger-etterlevelse-api"))

    testImplementation(kotlin("reflect"))
    testImplementation(testFixtures(project(":sykepenger-økonomi")))
    testImplementation(testFixtures(project(":sykepenger-utbetaling")))
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("com.github.navikt.spill_av_im:matching:$tbdSpillAvImMatchingVersion")

    testImplementation(project(":sykepenger-serde"))

    // for å kunne lage spannerpersoner i tester med OpenInSpanner-annotation
    testImplementation(project(":sykepenger-api-dto"))
    // for å kunne lage json av spannerpersoner
    testImplementation(libs.bundles.jackson)

    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}
