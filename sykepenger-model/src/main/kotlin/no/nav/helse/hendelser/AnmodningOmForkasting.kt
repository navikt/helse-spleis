package no.nav.helse.hendelser

import java.util.UUID

class AnmodningOmForkasting(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID
): ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {
    internal fun erRelevant(other: UUID) = other == vedtaksperiodeId
}