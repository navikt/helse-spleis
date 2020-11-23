package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime
import java.util.*

class UtbetalingOverført(
    meldingsreferanseId: UUID,
    internal val vedtaksperiodeId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val orgnummer: String,
    private val fagsystemId: String,
    private val utbetalingId: String,
    internal val avstemmingsnøkkel: Long,
    internal val overføringstidspunkt: LocalDateTime
) : ArbeidstakerHendelse(meldingsreferanseId) {
    private var erHåndtert = false

    override fun aktørId() = aktørId
    override fun fødselsnummer() = fødselsnummer
    override fun organisasjonsnummer() = orgnummer

    internal fun håndtert(utbetaling: Utbetaling) = erHåndtert.also {
        erHåndtert = true
    }

    internal fun erRelevant(fagsystemId: String) = fagsystemId == this.fagsystemId
    internal fun erRelevant(fagsystemId: String, utbetalingId: UUID) =
        erRelevant(fagsystemId) && this.utbetalingId == utbetalingId.toString()
}
