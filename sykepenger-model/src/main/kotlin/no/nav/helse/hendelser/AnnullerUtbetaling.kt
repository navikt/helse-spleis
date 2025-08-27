package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.utbetalingslinjer.Utbetaling

class AnnullerUtbetaling(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    override val utbetalingId: UUID,
    val saksbehandlerIdent: String,
    saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime,
    internal val årsaker: List<String>,
    internal val begrunnelse: String
) : Hendelse, AnnullerUtbetalingHendelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = erAutomatisk()
    )

    override val vurdering: Utbetaling.Vurdering = Utbetaling.Vurdering(true, saksbehandlerIdent, saksbehandlerEpost, opprettet, false)

    private fun erAutomatisk() = this.saksbehandlerIdent == "Automatisk behandlet"
}
