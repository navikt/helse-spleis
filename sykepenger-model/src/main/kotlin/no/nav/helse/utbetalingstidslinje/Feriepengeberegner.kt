package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.person.InfotrygdhistorikkVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosentdel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

internal class Feriepengeberegner(
    private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
    private val person: Person,
    private val alder: Alder
) : InfotrygdhistorikkVisitor, PersonVisitor, Iterable<LocalDate> {
    private companion object {
        private const val MAGIC_NUMBER = 48
    }

    private val dager = mutableSetOf<LocalDate>()

    init {
        utbetalingshistorikkForFeriepenger.accept(this)
        person.accept(this)
    }

    override fun visitInfotrygdhistorikkUtbetalingsperiode(orgnr: String, periode: Utbetalingsperiode, grad: Prosentdel, inntekt: Inntekt) {
        dager.addAll(periode.filterNot { it.erHelg() })
    }

    private var utbetaltUtbetaling = false
    override fun preVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        utbetaltUtbetaling = tilstand == Utbetaling.Utbetalt
    }

    override fun visitUtbetalingslinje(
        linje: Utbetalingslinje,
        fom: LocalDate,
        tom: LocalDate,
        beløp: Int?,
        aktuellDagsinntekt: Int,
        grad: Double,
        delytelseId: Int,
        refDelytelseId: Int?,
        refFagsystemId: String?,
        endringskode: Endringskode,
        datoStatusFom: LocalDate?
    ) {
        if (utbetaltUtbetaling) {
            dager.addAll((fom til tom).filterNot { it.erHelg() })
        }
    }

    override fun postVisitUtbetaling(
        utbetaling: Utbetaling,
        id: UUID,
        beregningId: UUID,
        type: Utbetaling.Utbetalingtype,
        tilstand: Utbetaling.Tilstand,
        tidsstempel: LocalDateTime,
        oppdatert: LocalDateTime,
        arbeidsgiverNettoBeløp: Int,
        personNettoBeløp: Int,
        maksdato: LocalDate,
        forbrukteSykedager: Int?,
        gjenståendeSykedager: Int?
    ) {
        utbetaltUtbetaling = false
    }

    internal fun beregn() {
        val datoer = dager
            .sorted()
            .groupBy { Year.from(it) }
            .flatMap { (_, prÅr) -> prÅr.take(MAGIC_NUMBER) }
    }

    override fun iterator() = dager
        .sorted()
        .groupBy { Year.from(it) }
        .flatMap { (_, prÅr) -> prÅr.take(MAGIC_NUMBER) }
        .iterator()
}
