package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingVisitor
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

    internal fun arbeidsgiverUtbetalingslinjer() = arbeidsgiverUtbetalingslinjer

    internal fun personUtbetalingslinjer() = personUtbetalingslinjer

    companion object {

        private fun buildArb(
            organisasjonsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg
        ) = Utbetalingslinjer(
                organisasjonsnummer,
                Mottakertype.SPREF,
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
            return Utbetalingslinjer(fødselsnummer, Mottakertype.SP)
        }
    }

    internal fun accept(visitor: UtbetalingVisitor) {
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


