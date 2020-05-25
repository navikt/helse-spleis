package no.nav.helse.utbetalingslinjer

import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.UtbetalingVisitor
import no.nav.helse.utbetalingslinjer.Fagområde.SykepengerRefusjon
import no.nav.helse.utbetalingstidslinje.Oldtidsutbetalinger
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.NavDag.Companion.arbeidsgiverUtbetaling
import java.time.LocalDate
import java.time.LocalDateTime

// Understands related payment activities for an Arbeidsgiver
internal class Utbetaling private constructor(
    private val utbetalingstidslinje: Utbetalingstidslinje,
    private val arbeidsgiverOppdrag: Oppdrag,
    private val personOppdrag: Oppdrag,
    private val tidsstempel: LocalDateTime
) {

    internal constructor(
        fødselsnummer: String,
        organisasjonsnummer: String,
        utbetalingstidslinje: Utbetalingstidslinje,
        sisteDato: LocalDate,
        aktivitetslogg: Aktivitetslogg,
        tidligere: Utbetaling?
    ) : this(
        utbetalingstidslinje,
        buildArb(organisasjonsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, tidligere),
        buildPerson(fødselsnummer, utbetalingstidslinje, sisteDato, aktivitetslogg, tidligere),
        LocalDateTime.now()
    )

    internal fun arbeidsgiverOppdrag() = arbeidsgiverOppdrag

    internal fun personOppdrag() = personOppdrag

    companion object {

        private fun buildArb(
            organisasjonsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg,
            tidligere: Utbetaling?
        ) = OppdragBuilder(tidslinje, organisasjonsnummer, SykepengerRefusjon, sisteDato, arbeidsgiverUtbetaling).result()
            .minus(
                tidligere?.arbeidsgiverOppdrag ?: Oppdrag(
                    organisasjonsnummer,
                    SykepengerRefusjon,
                    sisteArbeidsgiverdag = LocalDate.MIN
                )
            )
            .also { oppdrag ->
                tidligere?.arbeidsgiverOppdrag?.also { oppdrag.nettoBeløp(it) }
                if (oppdrag.isEmpty())
                    aktivitetslogg.info("Ingen utbetalingslinjer bygget")
                else
                    aktivitetslogg.info("Utbetalingslinjer bygget vellykket")
            }

        private fun buildPerson(
            fødselsnummer: String,
            tidslinje: Utbetalingstidslinje,
            sisteDato: LocalDate,
            aktivitetslogg: Aktivitetslogg,
            tidligere: Utbetaling?
        ): Oppdrag {
            return Oppdrag(fødselsnummer, Fagområde.Sykepenger, sisteArbeidsgiverdag = LocalDate.MIN)
        }
    }

    internal fun accept(visitor: UtbetalingVisitor) {
        visitor.preVisitUtbetaling(this, tidsstempel)
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        arbeidsgiverOppdrag.accept(visitor)
        visitor.postVisitArbeidsgiverOppdrag(arbeidsgiverOppdrag)
        visitor.preVisitPersonOppdrag(personOppdrag)
        personOppdrag.accept(visitor)
        visitor.postVisitPersonOppdrag(personOppdrag)
        visitor.postVisitUtbetaling(this, tidsstempel)
    }

    internal fun utbetalingstidslinje() = utbetalingstidslinje

    internal fun kansellerUtbetaling() = Utbetaling(
        utbetalingstidslinje,
        arbeidsgiverOppdrag.emptied() - arbeidsgiverOppdrag,
        personOppdrag.emptied() - personOppdrag,
        LocalDateTime.now()
    )

    internal fun append(organisasjonsnummer: String, oldtid: Oldtidsutbetalinger) {
        oldtid.add(organisasjonsnummer, utbetalingstidslinje)
    }
}


