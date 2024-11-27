package no.nav.helse.hendelser

import no.nav.helse.dto.SimuleringResultatDto
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Fagområde
import java.time.LocalDateTime
import java.util.UUID

class Simulering(
    meldingsreferanseId: UUID,
    val vedtaksperiodeId: String,
    orgnummer: String,
    override val fagsystemId: String,
    fagområde: String,
    override val simuleringOK: Boolean,
    override val melding: String,
    override val simuleringsResultat: SimuleringResultatDto?,
    override val utbetalingId: UUID
) : Hendelse,
    SimuleringHendelse {
    override val behandlingsporing =
        Behandlingsporing.Arbeidsgiver(
            organisasjonsnummer = orgnummer
        )
    override val metadata =
        LocalDateTime.now().let { nå ->
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
