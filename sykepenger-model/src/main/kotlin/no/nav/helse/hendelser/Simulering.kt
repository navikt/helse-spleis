package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Fagområde

class Simulering(
    meldingsreferanseId: UUID,
    val vedtaksperiodeId: String,
    aktørId: String,
    fødselsnummer: String,
    orgnummer: String,
    override val fagsystemId: String,
    fagområde: String,
    override val simuleringOK: Boolean,
    override val melding: String,
    override val simuleringsResultat: SimuleringResultatDto?,
    override val utbetalingId: UUID
) : Hendelse, SimuleringHendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = orgnummer
    )
    override val metadata = LocalDateTime.now().let { nå ->
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SYSTEM,
            innsendt = nå,
            registrert = nå,
            automatiskBehandling = true
        )
    }

    override val fagområde = Fagområde.from(fagområde)
}
