package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Saksbehandler

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
    private val godkjenttidspunkt: LocalDateTime,
    automatiskBehandling: Boolean
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer), Behandlingsavgjørelse {
    override fun relevantVedtaksperiode(id: UUID) = id.toString() == this.vedtaksperiodeId
    override fun saksbehandler() = Saksbehandler(saksbehandler, saksbehandlerEpost)
    override val godkjent = utbetalingGodkjent
    override val avgjørelsestidspunkt = godkjenttidspunkt
    override val automatisert = automatiskBehandling
    override fun innsendt() = godkjenttidspunkt
    override fun avsender() = if (automatisert) no.nav.helse.hendelser.Avsender.SYSTEM else no.nav.helse.hendelser.Avsender.SAKSBEHANDLER

}
