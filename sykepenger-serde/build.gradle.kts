val jsonassertVersion = "1.5.0"

dependencies {
    // bruker "implementation" fremfor "api" for å unngå
    // at avhengigheten blir transitiv, altså kopiert ut, til de som bruker denne modulen.
    implementation(project(":sykepenger-model-dto"))

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.logging) {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }

    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
}