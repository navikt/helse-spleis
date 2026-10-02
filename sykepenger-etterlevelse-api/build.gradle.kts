plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    testImplementation(testFixtures(project(":sykepenger-primitiver")))
}
