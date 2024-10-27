package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.utbetalingslinjer.Utbetalingstatus

class Utbetalingpåminnelse(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    override val utbetalingId: UUID,
    private val antallGangerPåminnet: Int,
    override val status: Utbetalingstatus,
    private val endringstidspunkt: LocalDateTime,
    private val påminnelsestidspunkt: LocalDateTime
) : ArbeidstakerHendelse(fødselsnummer, aktørId, organisasjonsnummer), UtbetalingpåminnelseHendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SYSTEM,
        innsendt = påminnelsestidspunkt,
        registrert = LocalDateTime.now(),
        automatiskBehandling = true
    )
}
