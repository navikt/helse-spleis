package no.nav.helse.hendelser.utbetaling

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_18
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_19
import no.nav.helse.utbetalingslinjer.Utbetaling

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

    fun erRelevant(vedtaksperiodeId: String) = vedtaksperiodeId == this.vedtaksperiodeId
    fun erRelevant(utbetalingId: UUID) = utbetalingId == this.utbetalingId

    fun automatiskBehandling() = automatiskBehandling
    fun vurdering() = Utbetaling.Vurdering(
        utbetalingGodkjent,
        saksbehandler,
        saksbehandlerEpost,
        godkjenttidspunkt,
        automatiskBehandling
    )

    fun valider(): IAktivitetslogg {
        when {
            !utbetalingGodkjent && !automatiskBehandling -> {
                funksjonellFeil(RV_UT_19)
                info("Utbetaling markert som ikke godkjent av saksbehandler $saksbehandler $godkjenttidspunkt")
            }
            !utbetalingGodkjent && automatiskBehandling -> {
                funksjonellFeil(RV_UT_18)
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
