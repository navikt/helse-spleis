package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingsdagVisitor
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.arbeidsgiverUtbetaling
import java.time.LocalDate
import java.time.LocalDateTime

// Understands related payment activities for an Arbeidsgiver
internal class Utbetaling
    private constructor(
        private val utbetalingstidslinje: Utbetalingstidslinje,
        private val arbeidsgiverUtbetalingslinjer: Utbetalingslinjer,
        private val personUtbetalingslinjer: Utbetalingslinjer,
        private val tidsstempel: LocalDateTime
    ) {

    internal constructor(
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje,
        sisteDato: LocalDate,
        aktivitetslogg: Aktivitetslogg
    ) : this(
        utbetalingstidslinje,
        buildArb(organisasjonsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg),
        buildPerson(fødselsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg),
        LocalDateTime.now()
    )

    companion object {
        private fun buildArb(
            organisasjonsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg
        ) = Utbetalingslinjer(
                organisasjonsnummer,
                Mottakertype.ARBEIDSGIVER,
                SpennBuilder(tidslinje, sisteDato, arbeidsgiverUtbetaling).result()).also {
            if (it.isEmpty())
                aktivitetslogg.info("Ingen utbetalingslinjer bygget")
            else
                aktivitetslogg.info("Utbetalingslinjer bygget vellykket")
        }

        private fun buildPerson(
            fødselsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg
        ): Utbetalingslinjer {
            return Utbetalingslinjer(fødselsnummer, Mottakertype.PERSON)
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
