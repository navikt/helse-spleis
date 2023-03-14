package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Arbeidsgiver

class InntektsmeldingReplayUtført(
    meldingsreferanseId: UUID,
    fødselsnummer: String,
    aktørId: String,
    organisasjonsnummer: String,
    private val vedtaksperiode: UUID
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {

    internal fun erRelevant(other: UUID) = other == vedtaksperiode
    override fun venter(arbeidsgivere: List<Arbeidsgiver>) {}
}