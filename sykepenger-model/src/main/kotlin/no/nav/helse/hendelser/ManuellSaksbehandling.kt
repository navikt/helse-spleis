package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse

class ManuellSaksbehandling(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val saksbehandler: String,
    private val utbetalingGodkjent: Boolean,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(aktivitetslogger, aktivitetslogg) {

    internal fun saksbehandler() = saksbehandler
    internal fun utbetalingGodkjent() = utbetalingGodkjent

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = organisasjonsnummer
    override fun melding(klassName: String) = "Manuell Saksbehandling"
    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Manuell saksbehandling")
    }
}
