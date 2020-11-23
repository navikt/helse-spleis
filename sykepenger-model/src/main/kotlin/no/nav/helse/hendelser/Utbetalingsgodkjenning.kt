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
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val saksbehandlerEpost: String,
    private val utbetalingGodkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime,
    private val automatiskBehandling: Boolean
) : ArbeidstakerHendelse(meldingsreferanseId) {

    internal fun vedtaksperiodeId() = vedtaksperiodeId
    internal fun saksbehandler() = saksbehandler
    internal fun saksbehandlerEpost() = saksbehandlerEpost
    internal fun godkjenttidspunkt() = godkjenttidspunkt
    internal fun automatiskBehandling() = automatiskBehandling

    internal fun vurdering() = Utbetaling.Vurdering(
       saksbehandler,
       saksbehandlerEpost,
       godkjenttidspunkt,
       automatiskBehandling
    )

    internal fun valider(): Aktivitetslogg {
        when {
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
