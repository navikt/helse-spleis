package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.ArbeidstakerHendelse
import java.time.LocalDateTime

class Utbetalingsgodkjenning(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val utbetalingGodkjent: Boolean,
    private val godkjenttidspunkt: LocalDateTime
) : ArbeidstakerHendelse() {

    internal fun vedtaksperiodeId() = vedtaksperiodeId
    internal fun saksbehandler() = saksbehandler
    internal fun godkjenttidspunkt() = godkjenttidspunkt

    internal fun valider(): Aktivitetslogg {
        if (!utbetalingGodkjent) error("Utbetaling markert som ikke godkjent")
        return aktivitetslogg
    }

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
    override fun melding(klassName: String) = "Utbetalingsgodkjenning"
}
