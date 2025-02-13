package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

class Utbetalingpåminnelse(
    meldingsreferanseId: MeldingsreferanseId,
    organisasjonsnummer: String,
    override val utbetalingId: UUID,
    private val antallGangerPåminnet: Int,
    override val status: Utbetalingstatus,
    private val endringstidspunkt: LocalDateTime,
    påminnelsestidspunkt: LocalDateTime
) : Hendelse, UtbetalingpåminnelseHendelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        organisasjonsnummer = organisasjonsnummer
    )
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = påminnelsestidspunkt,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )
}
