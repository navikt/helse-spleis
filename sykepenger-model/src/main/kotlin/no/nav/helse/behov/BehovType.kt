package no.nav.helse.behov

import no.nav.helse.person.Personkontekst
import no.nav.helse.person.Vedtaksperiodekontekst
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import java.time.LocalDate
import java.time.YearMonth

sealed class BehovType(private val context: Personkontekst) {
    val fødselsnummer = context.fødselsnummer
    val navn: String = this::class.simpleName!!
    protected open fun toMapInternal() = emptyMap<String, Any>()
    fun toMap() = toMapInternal() + context.toMap()

    class GjennomgåTidslinje constructor(context: Personkontekst) : BehovType(context)

    class Sykepengehistorikk internal constructor(
        context: Vedtaksperiodekontekst,
        private val utgangspunktForBeregningAvYtelse: LocalDate
    ) : BehovType(context) {
        override fun toMapInternal() = mapOf(
            "utgangspunktForBeregningAvYtelse" to utgangspunktForBeregningAvYtelse
        )
    }

    class Foreldrepenger internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)

    class Inntektsberegning internal constructor(
        context: Vedtaksperiodekontekst,
        private val beregningStart: YearMonth,
        private val beregningSlutt: YearMonth
    ) : BehovType(context) {
        override fun toMapInternal() = mapOf(
            "beregningStart" to beregningStart,
            "beregningSlutt" to beregningSlutt
        )
    }

    class EgenAnsatt internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)
    class Opptjening internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)
    class Godkjenning constructor(context: Vedtaksperiodekontekst) : BehovType(context)

    class Utbetaling internal constructor(
        context: Vedtaksperiodekontekst,
        private val utbetalingsreferanse: String,
        private val utbetalingslinjer: List<Utbetalingslinje>,
        private val maksdato: LocalDate,
        private val saksbehandler: String
    ) : BehovType(context) {
        override fun toMapInternal() = mapOf(
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
