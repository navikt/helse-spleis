dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))

    implementation(libs.bundles.jackson)
    implementation(project(":sykepenger-etterlevelse-api"))
    testFixturesImplementation(libs.junit.jupiter.api)
}