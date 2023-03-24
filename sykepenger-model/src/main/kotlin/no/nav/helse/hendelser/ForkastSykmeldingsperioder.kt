package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Sykmeldingsperioder

class ForkastSykmeldingsperioder(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val periode: Periode
): ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {

    internal fun forkast(sykdomsperioder: Sykmeldingsperioder) {
        sykdomsperioder.fjern(periode)
    }
}