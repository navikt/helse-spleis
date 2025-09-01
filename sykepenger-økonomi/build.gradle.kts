dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-økonomi-dto"))

    implementation(project(":sykepenger-primitiver"))
    testFixturesImplementation(project(":sykepenger-primitiver"))
}
