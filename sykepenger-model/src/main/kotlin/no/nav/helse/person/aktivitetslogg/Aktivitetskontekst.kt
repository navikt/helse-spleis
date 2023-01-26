package no.nav.helse.person.aktivitetslogg

interface Aktivitetskontekst {
    fun toSpesifikkKontekst(): SpesifikkKontekst
}