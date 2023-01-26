package no.nav.helse.person

import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst

interface KontekstObserver {
    fun nyKontekst(kontekst: Aktivitetskontekst)
}
