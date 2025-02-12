package no.nav.helse.etterlevelse

enum class KontekstType {
    Fødselsnummer,
    Organisasjonsnummer,
    Vedtaksperiode,
    Behandling
}

data class Subsumsjonskontekst(
    val type: KontekstType,
    val verdi: String
)
