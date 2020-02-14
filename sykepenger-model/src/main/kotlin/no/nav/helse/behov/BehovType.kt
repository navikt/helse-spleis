package no.nav.helse.behov

import no.nav.helse.person.Personkontekst
import no.nav.helse.person.Vedtaksperiodekontekst
import no.nav.helse.utbetalingstidslinje.Utbetalingslinje
import java.time.LocalDate
import java.time.YearMonth

sealed class BehovType(private val context: Personkontekst) {
    class GjennomgåTidslinje internal constructor(context: Personkontekst) : BehovType(context)

    class Sykepengehistorikk internal constructor(
        context: Vedtaksperiodekontekst,
        private val utgangspunktForBeregningAvYtelse: LocalDate
    ) : BehovType(context)

    class Foreldrepenger internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)

    class Inntektsberegning internal constructor(
        context: Vedtaksperiodekontekst,
        private val beregningStart: YearMonth,
        private val beregningSlutt: YearMonth
    ) : BehovType(context)

    class EgenAnsatt internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)

    class Opptjening internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)

    class Godkjenning internal constructor(context: Vedtaksperiodekontekst) : BehovType(context)

    class Utbetaling internal constructor(
        context: Vedtaksperiodekontekst,
        private val utbetalingsreferanse: String,
        private val utbetalingslinjer: List<Utbetalingslinje>,
        private val maksdato: LocalDate,
        private val saksbehandler: String
    ) : BehovType(context)
}
