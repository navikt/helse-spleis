package no.nav.helse.hendelser

import no.nav.helse.hendelser.Avsender.SYSTEM
import java.time.LocalDateTime
import java.util.UUID

class Utbetalingsgodkjenning(
    meldingsreferanseId: UUID,
    organisasjonsnummer: String,
    override val utbetalingId: UUID,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    utbetalingGodkjent: Boolean,
    godkjenttidspunkt: LocalDateTime,
    automatiskBehandling: Boolean,
) : Behandlingsavgjørelse {
    override val behandlingsporing =
        Behandlingsporing.Arbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
        )
    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = if (automatiskBehandling) SYSTEM else Avsender.SAKSBEHANDLER,
            innsendt = godkjenttidspunkt,
            registrert = LocalDateTime.now(),
            automatiskBehandling = automatiskBehandling,
        )

    override fun relevantVedtaksperiode(id: UUID) = id.toString() == this.vedtaksperiodeId

    override fun saksbehandler() = Saksbehandler(saksbehandler, saksbehandlerEpost)

    override val godkjent = utbetalingGodkjent
    override val avgjørelsestidspunkt = metadata.innsendt
    override val automatisert = metadata.automatiskBehandling
}
