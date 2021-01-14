package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*

class Utbetalingsgodkjenning(
    meldingsreferanseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val utbetalingId: UUID,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    private val utbetalingGodkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime,
    private val automatiskBehandling: Boolean,
    private val makstidOppnådd: Boolean
) : ArbeidstakerHendelse(meldingsreferanseId) {

    internal fun erRelevant(vedtaksperiodeId: String) = vedtaksperiodeId == this.vedtaksperiodeId
    internal fun erRelevant(utbetalingId: UUID) = utbetalingId == this.utbetalingId

    internal fun vurdering() = Utbetaling.Vurdering(
        utbetalingGodkjent,
        saksbehandler,
        saksbehandlerEpost,
        godkjenttidspunkt,
        automatiskBehandling
    )

    internal fun valider(): Aktivitetslogg {
        when {
            !utbetalingGodkjent && automatiskBehandling && makstidOppnådd ->
                error("Gir opp fordi saksbehandleroppgaven har nådd makstid")
            !utbetalingGodkjent && !automatiskBehandling ->
                error("Utbetaling markert som ikke godkjent av saksbehandler $saksbehandler $godkjenttidspunkt")
            !utbetalingGodkjent && automatiskBehandling ->
                error("Utbetaling markert som ikke godkjent automatisk $godkjenttidspunkt")
            utbetalingGodkjent && !automatiskBehandling ->
                info("Utbetaling markert som godkjent av saksbehandler $saksbehandler $godkjenttidspunkt")
            else ->
                info("Utbetaling markert som godkjent automatisk $godkjenttidspunkt")
        }
        return aktivitetslogg
    }

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
    override fun melding(klassName: String) = "Utbetalingsgodkjenning"
}
