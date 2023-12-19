package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.Avsender.SAKSBEHANDLER
import no.nav.helse.hendelser.Avsender.SYSTEM

class Utbetalingsgodkjenning(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val utbetalingId: UUID,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    utbetalingGodkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime,
    automatiskBehandling: Boolean
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer), Utbetalingsavgjørelse {
    override fun relevantVedtaksperiode(id: UUID) = id.toString() == this.vedtaksperiodeId
    override fun relevantUtbetaling(id: UUID) = id == utbetalingId
    override fun saksbehandler() = Saksbehandler(saksbehandler, saksbehandlerEpost)
    override val godkjent = utbetalingGodkjent
    override val avgjørelsestidspunkt = godkjenttidspunkt
    override val automatisert = automatiskBehandling
    override fun innsendt() = godkjenttidspunkt
    override fun avsender() = if (automatisert) SYSTEM else SAKSBEHANDLER

}
