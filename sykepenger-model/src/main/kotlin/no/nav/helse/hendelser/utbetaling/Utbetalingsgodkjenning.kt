package no.nav.helse.hendelser.utbetaling

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*

class Utbetalingsgodkjenning(
    meldingsreferanseId: UUID,
    aktørId: String,
    fødselsnummer: String,
    organisasjonsnummer: String,
    private val utbetalingId: UUID,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    private val utbetalingGodkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime,
    private val automatiskBehandling: Boolean
) : ArbeidstakerHendelse(meldingsreferanseId, fødselsnummer, aktørId, organisasjonsnummer) {

    internal fun erRelevant(vedtaksperiodeId: String) = vedtaksperiodeId == this.vedtaksperiodeId
    internal fun erRelevant(utbetalingId: UUID) = utbetalingId == this.utbetalingId

    internal fun vurdering() = Utbetaling.Vurdering(
        utbetalingGodkjent,
        saksbehandler,
        saksbehandlerEpost,
        godkjenttidspunkt,
        automatiskBehandling
    )

    internal fun valider(): IAktivitetslogg {
        when {
            !utbetalingGodkjent && !automatiskBehandling -> {
                error("Utbetaling markert som ikke godkjent av saksbehandler")
                info("Utbetaling markert som ikke godkjent av saksbehandler $saksbehandler $godkjenttidspunkt")
            }
            !utbetalingGodkjent && automatiskBehandling -> {
                error("Utbetaling markert som ikke godkjent automatisk")
                info("Utbetaling markert som ikke godkjent automatisk $godkjenttidspunkt")
            }
            utbetalingGodkjent && !automatiskBehandling ->
                info("Utbetaling markert som godkjent av saksbehandler $saksbehandler $godkjenttidspunkt")
            else ->
                info("Utbetaling markert som godkjent automatisk $godkjenttidspunkt")
        }
        return this
    }
}
