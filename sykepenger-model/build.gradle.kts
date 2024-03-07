val jsonassertVersion = "1.5.0"
val spekematVersion = "2024.03.07-12.28-78550ee8"

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
    // <midlertidig begrunnelse="sammenligner json mellom JsonBuilder og ny PersonDto. scope skal tilbake til testImplementation">
    implementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    // </midlertidig>
    testImplementation("com.github.navikt.spekemat:fabrikk:$spekematVersion")
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}
