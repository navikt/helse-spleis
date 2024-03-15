val jsonassertVersion = "1.5.0"
val spekematVersion = "2024.03.07-12.28-78550ee8"

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-model-dto"))

    api(project(":sykepenger-primitiver"))
    api(project(":sykepenger-utbetaling"))
    api(project(":sykepenger-aktivitetslogg"))
    api(project(":sykepenger-etterlevelse"))

    testImplementation(kotlin("reflect"))
    testImplementation(testFixtures(project(":sykepenger-utbetaling")))
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("com.github.navikt.spekemat:fabrikk:$spekematVersion")

    testImplementation(project(":sykepenger-serde"))
}

tasks {
    withType<Test> {
        systemProperty("junit.jupiter.execution.parallel.enabled", "true")
        systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
        systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
        systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "8")
    }
}
