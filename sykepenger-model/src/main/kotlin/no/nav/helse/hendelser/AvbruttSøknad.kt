package no.nav.helse.hendelser

import java.util.UUID
import no.nav.helse.person.Sykmeldingsperioder

class AvbruttSøknad(
    private val periode: Periode,
    meldingsreferanseId: UUID,
    orgnummer: String,
    fødselsnummer: String,
    aktørId: String,
) : ArbeidstakerHendelse(
    meldingsreferanseId = meldingsreferanseId,
    fødselsnummer = fødselsnummer,
    aktørId = aktørId,
    organisasjonsnummer = orgnummer
) {
    internal fun avbryt(sykmeldingsperioder: Sykmeldingsperioder) {
        sykmeldingsperioder.fjern(periode)
    }
}