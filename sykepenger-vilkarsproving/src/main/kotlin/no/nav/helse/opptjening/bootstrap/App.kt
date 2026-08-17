package no.nav.helse.opptjening.bootstrap

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    launchApplication(System.getenv())
}

fun launchApplication(env: Map<String, String>) {
    RapidApplication
        .create(env)
        .apply {
            VilkårsprøvingModule(this)
        }.start()
}
