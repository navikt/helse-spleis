package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.UUID

class AnnullerUtbetaling(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    val vedtaksperiodeId: UUID,
    val saksbehandlerIdent: String,
    saksbehandlerEpost: String,
    internal val opprettet: LocalDateTime,
    internal val årsaker: List<String>,
    internal val begrunnelse: String,
) : Hendelse {
    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = SAKSBEHANDLER,
            innsendt = opprettet,
            registrert = LocalDateTime.now(),
            automatiskBehandling = erAutomatisk(),
        )

    val vurdering: Utbetaling.Vurdering = Utbetaling.Vurdering(true, saksbehandlerIdent, saksbehandlerEpost, opprettet, false)

    private fun erAutomatisk() = this.saksbehandlerIdent == "Automatisk behandlet"
}
