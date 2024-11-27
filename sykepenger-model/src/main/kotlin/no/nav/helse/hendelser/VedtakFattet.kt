package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYSTEM

class VedtakFattet(
    meldingsreferanseId: UUID,
    organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    override val utbetalingId: UUID,
    private val saksbehandlerIdent: String,
    private val saksbehandlerEpost: String,
    vedtakFattetTidspunkt: LocalDateTime,
    override val automatisert: Boolean,
) : Behandlingsavgjørelse {
    override val behandlingsporing =
        Behandlingsporing.Arbeidsgiver(organisasjonsnummer = organisasjonsnummer)
    override val metadata =
        HendelseMetadata(
            meldingsreferanseId = meldingsreferanseId,
            avsender = if (automatisert) SYSTEM else SAKSBEHANDLER,
            innsendt = vedtakFattetTidspunkt,
            registrert = LocalDateTime.now(),
            automatiskBehandling = automatisert,
        )

    override val avgjørelsestidspunkt = metadata.innsendt
    override val godkjent = true

    override fun saksbehandler() = Saksbehandler(saksbehandlerIdent, saksbehandlerEpost)

    override fun relevantVedtaksperiode(id: UUID) = vedtaksperiodeId == id
}
