package no.nav.helse.person

interface KontekstObserver {
    fun nyKontekst(kontekst: Aktivitetskontekst)
}
