package no.nav.helse.behov

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Vedtaksperiodekontekst
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

fun List<BehovType>.partisjoner(): List<Map<String, Any>> = BehovType.partisjoner(this)

sealed class BehovType(private val context: Vedtaksperiodekontekst) {
    internal companion object {
        fun partisjoner(liste: List<BehovType>): List<Map<String, Any>> {
            return liste.groupBy { it.context }.map { kombiner(it.key.toMap().toMutableMap(), it.value) }
        }

        private fun kombiner(initiell: MutableMap<String, Any>, liste: List<BehovType>): Map<String, Any> {
            initiell["@behov"] = liste.map { it.navn }
            return liste.fold(initiell) { result, behov ->
                result.apply {
                    putAll(behov.toMap().also { behovMap ->
                        behovMap.keys
                            .firstOrNull { key -> key in result.keys && result[key] != behovMap[key] }
                            ?.also { key -> error("$key finnes med verdi ${result[key]}. Prøvde å sette til ${behovMap[key]}") }

                    })
                }
            }
        }
    }

    private val navn = this::class.simpleName!!

    internal open fun toMap() = emptyMap<String, Any>()

    internal fun loggTilAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        //aktivitetslogg.behov(melding = navn)
    }

    class GjennomgåTidslinje internal constructor(context: Vedtaksperiodekontekst) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
        )
    }
    class Sykepengehistorikk internal constructor(context: Vedtaksperiodekontekst, private val utgangspunktForBeregningAvYtelse: LocalDate) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, utgangspunktForBeregningAvYtelse: LocalDate) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId), utgangspunktForBeregningAvYtelse
        )

        override fun toMap() = mapOf(
            "utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse
        )
    }
    class Foreldrepenger internal constructor(context: Vedtaksperiodekontekst) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
        )
    }
    class Inntektsberegning internal constructor(
        context: Vedtaksperiodekontekst,
        private val beregningStart: YearMonth,
        private val beregningSlutt: YearMonth
    ) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, beregningStart: YearMonth, beregningSlutt: YearMonth) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId), beregningStart, beregningSlutt
        )

        override fun toMap() = mapOf(
            "beregningStart" to beregningStart,
            "beregningSlutt" to beregningSlutt
        )
    }
    class EgenAnsatt internal constructor(context: Vedtaksperiodekontekst) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
        )
    }
    class Opptjening internal constructor(context: Vedtaksperiodekontekst) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
        )
    }
    class Godkjenning internal constructor(context: Vedtaksperiodekontekst) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId)
        )
    }
    class Utbetaling internal constructor(
        context: Vedtaksperiodekontekst,
        private val utbetalingsreferanse: String,
        private val utbetalingslinjer: List<Utbetalingslinje>,
        private val maksdato: LocalDate,
        private val saksbehandler: String
    ) : BehovType(context) {
        constructor(aktørId: String, fødselsnummer: String, organisasjonsnummer: String, vedtaksperiodeId: UUID, utbetalingsreferanse: String, utbetalingslinjer: List<Utbetalingslinje>, maksdato: LocalDate, saksbehandler: String) : this(
            Vedtaksperiodekontekst(aktørId, fødselsnummer, organisasjonsnummer, vedtaksperiodeId), utbetalingsreferanse, utbetalingslinjer, maksdato, saksbehandler
        )

        override fun toMap() = mapOf(
            "utbetalingsreferanse" to utbetalingsreferanse,
            "utbetalingslinjer" to utbetalingslinjer.map {
                mapOf(
                    "fom" to it.fom,
                    "tom" to it.tom,
                    "dagsats" to it.dagsats
                )
            },
            "maksdato" to maksdato,
            "saksbehandler" to saksbehandler
        )
    }
}
