plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))

    implementation(libs.bundles.jackson)
    implementation(project(":sykepenger-etterlevelse-api"))
    testFixturesImplementation(libs.junit.jupiter.api)

    // delt testdatabase-oppsett: TestDatabase fra sykepenger-libs, migrert med Flyway
    testFixturesImplementation(testFixtures(libs.sykepenger.libs.postgres))
}
