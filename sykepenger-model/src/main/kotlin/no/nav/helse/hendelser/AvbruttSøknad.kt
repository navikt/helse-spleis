package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Sykmeldingsperioder

class AvbruttSøknad(
    private val periode: Periode,
    meldingsreferanseId: UUID,
    orgnummer: String,
    fødselsnummer: String,
    aktørId: String,
) : ArbeidstakerHendelse(
    fødselsnummer = fødselsnummer,
    aktørId = aktørId,
    organisasjonsnummer = orgnummer
) {
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = Avsender.SYKMELDT,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = false
        )
    }

    internal fun avbryt(sykmeldingsperioder: Sykmeldingsperioder) {
        sykmeldingsperioder.fjern(periode)
    }
}