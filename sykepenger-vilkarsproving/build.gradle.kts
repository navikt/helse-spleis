plugins {
    kotlin("jvm") version "2.3.0"
}

val tbdLibsVersion = "20260626.0942"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.bundles.database)
    implementation(libs.bundles.flyway)
    implementation(project(":sykepenger-primitiver"))
    implementation("com.github.navikt.tbd-libs:naisful-postgres:$tbdLibsVersion")

    testImplementation(kotlin("test"))
    testImplementation(testFixtures(project(":sykepenger-primitiver")))
    testImplementation("com.github.navikt.tbd-libs:rapids-and-rivers-test:$tbdLibsVersion")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}
