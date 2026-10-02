plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    // bruker "implementation" fremfor "api" for å unngå
    // at avhengigheten blir transitiv, altså kopiert ut, til de som bruker denne modulen.
    implementation(project(":sykepenger-primitiver-dto"))
    implementation(project(":sykepenger-økonomi-dto"))
}
