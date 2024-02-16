dependencies {
    implementation(libs.bundles.jackson)
    implementation(project(":sykepenger-etterlevelse-api"))
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
}