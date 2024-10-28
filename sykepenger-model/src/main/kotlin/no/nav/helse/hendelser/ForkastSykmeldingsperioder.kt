package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.person.Sykmeldingsperioder

class ForkastSykmeldingsperioder(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val periode: Periode
): PersonHendelse() {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = organisasjonsnummer
    )

    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = Avsender.SAKSBEHANDLER,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = false
        )
    }

    internal fun forkast(sykdomsperioder: Sykmeldingsperioder) {
        sykdomsperioder.fjern(periode)
    }
}