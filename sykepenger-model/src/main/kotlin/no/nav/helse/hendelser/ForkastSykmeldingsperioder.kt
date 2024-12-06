package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Sykmeldingsperioder

class ForkastSykmeldingsperioder(
    meldingsreferanseId: UUID,
    organisasjonsnummer: String,
    private val periode: Periode
) : Hendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
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
