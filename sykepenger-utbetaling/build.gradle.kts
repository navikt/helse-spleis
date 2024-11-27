val commonsCodecVersion = "1.15"

dependencies {
    api("commons-codec:commons-codec:$commonsCodecVersion")

    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-utbetaling-dto"))

    implementation(project(":sykepenger-etterlevelse-api"))
    implementation(project(":sykepenger-primitiver"))
    implementation(project(":sykepenger-aktivitetslogg"))
    testFixturesImplementation(project(":sykepenger-aktivitetslogg"))
    testFixturesImplementation(project(":sykepenger-primitiver"))
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }
}
