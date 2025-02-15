package no.nav.helse.etterlevelse

enum class KontekstType {
    Organisasjonsnummer,
    Vedtaksperiode
}

data class Subsumsjonskontekst(
    val type: KontekstType,
    val verdi: String
)
