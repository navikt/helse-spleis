package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

class Utbetalingpåminnelse(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    override val utbetalingId: UUID,
    private val antallGangerPåminnet: Int,
    override val status: Utbetalingstatus,
    private val endringstidspunkt: LocalDateTime,
    påminnelsestidspunkt: LocalDateTime
) : Hendelse, UtbetalingpåminnelseHendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = påminnelsestidspunkt,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )
}
