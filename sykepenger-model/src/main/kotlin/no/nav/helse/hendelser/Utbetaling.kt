package no.nav.helse.hendelser

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse

class Utbetaling(
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    internal val utbetalingsreferanse: String,
    private val status: String,
    internal val melding: String,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(
    aktivitetslogger,
    aktivitetslogg
) {
    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Utbetaling")
    }

    internal fun isOK() = status == "FERDIG"
}
