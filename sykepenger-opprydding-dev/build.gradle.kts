plugins {
    id("no.nav.sykepenger.deployable")
}

sykepengerDeployable {
    mainClass = "no.nav.helse.opprydding.AppKt"
    imageName = "helse-spleis-sykepenger-opprydding-dev"
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.cloudsql)
    implementation(project(":sykepenger-utbetaling"))
    implementation(libs.tbd.naisful.postgres)

    testImplementation(project(":sykepenger-mediators")) // for å få  tilgang på db/migrations-filene
    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation(libs.tbd.rapids.and.rivers.test)
    testImplementation(libs.bundles.flyway)
}
