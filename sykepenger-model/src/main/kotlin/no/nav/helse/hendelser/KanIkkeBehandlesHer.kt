package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.*
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYSTEM

class KanIkkeBehandlesHer(
    meldingsreferanseId: MeldingsreferanseId,
    override val behandlingsporing: Behandlingsporing.Yrkesaktivitet,
    private val vedtaksperiodeId: UUID,
    override val utbetalingId: UUID,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    opprettet: LocalDateTime,
    override val automatisert: Boolean
) : Behandlingsavgjørelse {
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = if (automatisert) SYSTEM else SAKSBEHANDLER,
        innsendt = opprettet,
        registrert = LocalDateTime.now(),
        automatiskBehandling = automatisert
    )

    override val avgjørelsestidspunkt = opprettet
    override val godkjent = false
    override fun saksbehandler() = Saksbehandler(saksbehandlerIdent, saksbehandlerEpost)
    override fun relevantVedtaksperiode(id: UUID) = vedtaksperiodeId == id
}
