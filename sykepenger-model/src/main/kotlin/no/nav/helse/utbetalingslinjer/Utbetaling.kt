package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

// Understands related payment activities for an Arbeidsgiver
internal class Utbetaling
    private constructor(
        private val utbetalingstidslinje: Utbetalingstidslinje,
        private val arbeidsgiverUtbetalingslinjer: Utbetalingslinjer = Utbetalingslinjer(),
        private val personUtbetalingslinjer: Utbetalingslinjer = Utbetalingslinjer(),
        private val tidsstempel: LocalDateTime
    ) {

    internal constructor(
        utbetalingstidslinje: Utbetalingstidslinje,
        sisteDato: LocalDate,
        aktivitetslogg: Aktivitetslogg
    ) : this(
        utbetalingstidslinje,
        buildArb(utbetalingstidslinje, sisteDato, aktivitetslogg),
        buildPerson(utbetalingstidslinje, sisteDato, aktivitetslogg),
        LocalDateTime.now()
    )

    companion object {
        private fun buildArb(
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg
        ) = SpennBuilder(tidslinje, sisteDato).result().also {
            if (it.isEmpty())
                aktivitetslogg.info("Ingen utbetalingslinjer bygget")
            else
                aktivitetslogg.info("Utbetalingslinjer bygget vellykket")
        }

        private fun buildPerson(
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg
        ): Utbetalingslinjer {
            return Utbetalingslinjer()
        }
    }

    fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitUtbetaling(this, tidsstempel)
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitArbeidsgiverUtbetalingslinjer(arbeidsgiverUtbetalingslinjer)
        arbeidsgiverUtbetalingslinjer.accept(visitor)
        visitor.postVisitArbeidsgiverUtbetalingslinjer(arbeidsgiverUtbetalingslinjer)
        visitor.preVisitPersonUtbetalingslinjer(personUtbetalingslinjer)
        personUtbetalingslinjer.accept(visitor)
        visitor.postVisitPersonUtbetalingslinjer(personUtbetalingslinjer)
        visitor.postVisitUtbetaling(this, tidsstempel)
    }

    internal fun utbetalingstidslinje() = utbetalingstidslinje
}

internal interface UtbetalingVisitor: UtbetalingsdagVisitor, UtbetalingslinjerVisitor {
    fun preVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {}
    fun preVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun postVisitTidslinjer(tidslinjer: MutableList<Utbetalingstidslinje>) {}
    fun preVisitArbeidsgiverUtbetalingslinjer(linjer: Utbetalingslinjer) {}
    fun postVisitArbeidsgiverUtbetalingslinjer(linjer: Utbetalingslinjer) {}
    fun preVisitPersonUtbetalingslinjer(linjer: Utbetalingslinjer) {}
    fun postVisitPersonUtbetalingslinjer(linjer: Utbetalingslinjer) {}
    fun postVisitUtbetaling(utbetaling: Utbetaling, tidsstempel: LocalDateTime) {}
}

internal interface UtbetalingslinjerVisitor {
    fun preVisitUtbetalingslinjer(linjer: Utbetalingslinjer) {}
    fun visitUtbetalingslinje(
        utbetalingslinje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        dagsats: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?
    ) {}
    fun postVisitUtbetalingslinjer(linjer: Utbetalingslinjer) {}
}
