plugins {
    id("no.nav.sykepenger.deployable")
}

sykepengerDeployable {
    mainClass = "no.nav.helse.spleis.jobs.AppKt"
    imageName = "helse-spleis-jobs"
}

dependencies {
    implementation(libs.tbd.kafka)
    implementation(libs.bundles.jackson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":sykepenger-model"))
    implementation(project(":sykepenger-serde"))
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.cloudsql)
    implementation(libs.tbd.sql)
}

tasks.named<Test>("test") {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
