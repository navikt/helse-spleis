package no.nav.helse.hendelser

import no.nav.helse.behov.Behov
import no.nav.helse.behov.Behovstype
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.person.Inntekthistorikk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Deprecated("Sykepengehistorikk og foreldrepenger sendes som to parametre til modellen")
class Ytelser(
    hendelseId: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    internal val vedtaksperiodeId: String,
    private val utbetalingshistorikk: Utbetalingshistorikk,
    private val foreldrepermisjon: Foreldrepermisjon,
    private val rapportertdato: LocalDateTime,
    aktivitetslogger: Aktivitetslogger,
    aktivitetslogg: Aktivitetslogg
) : ArbeidstakerHendelse(hendelseId, aktivitetslogger, aktivitetslogg) {
    internal companion object {
        fun lagBehov(
            vedtaksperiodeId: UUID,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            utgangspunktForBeregningAvYtelse: LocalDate
        ): Behov {
            val params = mutableMapOf(
                "utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse
            )

            return Behov.nyttBehov(
                behov = listOf(Behovstype.Sykepengehistorikk, Behovstype.Foreldrepenger),
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                additionalParams = params
            )
        }

    }

    internal fun sykepengehistorikk() = utbetalingshistorikk

    internal fun foreldrepenger() = foreldrepermisjon

    fun valider() = utbetalingshistorikk.valider()

    internal fun addInntekter(inntekthistorikk: Inntekthistorikk) {
        sykepengehistorikk().addInntekter(this.hendelseId(), inntekthistorikk)
    }

    override fun aktørId(): String {
        return aktørId
    }

    override fun fødselsnummer(): String {
        return fødselsnummer
    }

    override fun organisasjonsnummer(): String {
        return organisasjonsnummer
    }

    internal fun kopierAktiviteterTil(aktivitetslogger: Aktivitetslogger) {
        aktivitetslogger.addAll(this.aktivitetslogger, "Ytelser")
    }
}
