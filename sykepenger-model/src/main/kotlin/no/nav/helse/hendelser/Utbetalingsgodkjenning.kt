package no.nav.helse.hendelser

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Avsender.SYSTEM

class Utbetalingsgodkjenning(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    override val utbetalingId: UUID,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    utbetalingGodkjent: Boolean,
    godkjenttidspunkt: LocalDateTime,
    automatiskBehandling: Boolean
) : Hendelse, Behandlingsavgjørelse {
    override val behandlingsporing = Behandlingsporing.Arbeidsgiver(
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
        organisasjonsnummer = organisasjonsnummer
    )
    override val metadata = HendelseMetadata(
        meldingsreferanseId = meldingsreferanseId,
        avsender = if (automatiskBehandling) SYSTEM else Avsender.SAKSBEHANDLER,
        innsendt = godkjenttidspunkt,
        registrert = LocalDateTime.now(),
        automatiskBehandling = automatiskBehandling
    )

    override fun relevantVedtaksperiode(id: UUID) = id.toString() == this.vedtaksperiodeId
    override fun saksbehandler() = Saksbehandler(saksbehandler, saksbehandlerEpost)
    override val godkjent = utbetalingGodkjent
    override val avgjørelsestidspunkt = metadata.innsendt
    override val automatisert = metadata.automatiskBehandling

}
