package no.nav.helse.dsl

import no.nav.helse.etterlevelse.Subsumsjon
import no.nav.helse.etterlevelse.Subsumsjonslogg

class SubsumsjonsListLog : Subsumsjonslogg {
    private val oppsamlet = mutableListOf<Subsumsjon>()
    val subsumsjoner get() = oppsamlet.toList()

    override fun logg(subsumsjon: Subsumsjon) {
        oppsamlet.add(subsumsjon)
    }
}