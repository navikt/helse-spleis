dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))

    implementation(libs.bundles.jackson)
    implementation(project(":sykepenger-etterlevelse-api"))
    testFixturesImplementation(libs.junit.jupiter.api)

    // delt testdatabase-oppsett (én gjenbrukt Testcontainers-database per modul, migrert med Flyway)
    testFixturesImplementation(libs.bundles.database)
    testFixturesImplementation(libs.bundles.flyway)
    testFixturesImplementation(libs.testcontainers.postgres)
}