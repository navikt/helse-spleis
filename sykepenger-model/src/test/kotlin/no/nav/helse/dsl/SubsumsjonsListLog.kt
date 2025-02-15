package no.nav.helse.dsl

import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.Regelverksporing

class SubsumsjonsListLog : Regelverkslogg {
    private val oppsamlet = mutableListOf<Regelverksporing>()
    val regelverksporinger get() = oppsamlet.toList()

    override fun logg(sporing: Regelverksporing) {
        oppsamlet.add(sporing)
    }
}
