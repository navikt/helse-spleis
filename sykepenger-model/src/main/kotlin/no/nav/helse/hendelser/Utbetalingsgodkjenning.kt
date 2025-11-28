package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SYSTEM

class Utbetalingsgodkjenning(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    override val utbetalingId: UUID,
    override val vedtaksperiodeId: UUID,
    override val behandlingId: UUID,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    utbetalingGodkjent: Boolean,
    godkjenttidspunkt: LocalDateTime,
    automatiskBehandling: Boolean
) : Behandlingsavgjørelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = if (automatiskBehandling) SYSTEM else Avsender.SAKSBEHANDLER,
        innsendt = godkjenttidspunkt,
        registrert = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling
    )

    override fun saksbehandler() = Saksbehandler(saksbehandler, saksbehandlerEpost)
    override val godkjent = utbetalingGodkjent
    override val avgjørelsestidspunkt = metadata.innsendt
    override val automatisert = metadata.automatiskBehandling

}
