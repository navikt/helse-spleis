package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Oppdragstatus

class UtbetalingHendelse(
    meldingsreferanseId: UUID,
    orgnummer: String,
    override val fagsystemId: String,
    override val utbetalingId: UUID,
    override val status: Oppdragstatus,
    override val melding: String,
    override val avstemmingsnøkkel: Long,
    override val overføringstidspunkt: LocalDateTime,
) : Hendelse, UtbetalingmodulHendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(organisasjonsnummer = orgnummer)
    override val metadata =
        LocalDateTime.now().let { nå ->
            HendelseMetadata(
                meldingsreferanseId = meldingsreferanseId,
                avsender = SYSTEM,
                innsendt = nå,
                registrert = nå,
                automatiskBehandling = true,
            )
        }
}
