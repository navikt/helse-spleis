plugins {
    id("no.nav.sykepenger.kotlin")
}

dependencies {
    // bruker "api" sånn at avhengigheten blir kopiert ut til konsumenter av denne modulen
    api(project(":sykepenger-primitiver-dto"))
    api(project(":sykepenger-utbetaling-dto"))
    api(project(":sykepenger-økonomi-dto"))
    api(project(":sykepenger-aktivitetslogg-dto"))
}
