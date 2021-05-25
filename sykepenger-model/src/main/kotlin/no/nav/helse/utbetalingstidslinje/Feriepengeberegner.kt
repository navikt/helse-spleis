package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.SPLEIS_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.and
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.feriepengedager
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.orgnummerFilter
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.summer
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.tilDato
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*

private typealias UtbetaltDagSelector = (Feriepengeberegner.UtbetaltDag) -> Boolean

internal class Feriepengeberegner(
    private val alder: Alder,
    private val opptjeningsår: Year,
    private val utbetalteDager: List<UtbetaltDag>
) {
    private companion object {
        private const val MAGIC_NUMBER = 48
    }

    internal constructor(
        alder: Alder,
        opptjeningsår: Year,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        person: Person
    ) : this(alder, opptjeningsår, FinnUtbetalteDagerVisitor(utbetalingshistorikkForFeriepenger, person).utbetalteDager())

    internal fun accept(visitor: FeriepengeutbetalingVisitor) {
        visitor.preVisitFeriepengeberegner(this)
        utbetalteDager.forEach { it.accept(visitor) }
        visitor.postVisitFeriepengeberegner(this)
    }

    internal fun feriepengedatoer() = feriepengedager().tilDato()
    internal fun beregnFeriepengerForInfotrygdPerson() = beregnForFilter(INFOTRYGD_PERSON)
    internal fun beregnFeriepengerForInfotrygdPerson(orgnummer: String) = beregnForFilter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForInfotrygdPersonUtenPersonhack(orgnummer: String) =
        beregnForFilterUtenPersonhack(INFOTRYGD_PERSON and orgnummerFilter(orgnummer))

    internal fun beregnFeriepengerForInfotrygdArbeidsgiver() = beregnForFilter(INFOTRYGD_ARBEIDSGIVER)
    internal fun beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer: String) = beregnForFilter(INFOTRYGD_ARBEIDSGIVER and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForSpleis() = beregnForFilter(SPLEIS_ARBEIDSGIVER)
    internal fun beregnFeriepengerForSpleis(orgnummer: String) = beregnForFilter(SPLEIS_ARBEIDSGIVER and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForInfotrygd() = beregnForFilter(INFOTRYGD)
    internal fun beregnFeriepengerForInfotrygd(orgnummer: String) = beregnForFilter(INFOTRYGD and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForArbeidsgiver(orgnummer: String) = beregnForFilter(ARBEIDSGIVER and orgnummerFilter(orgnummer))
    internal fun beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygd(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag)
    }

    internal fun beregnFeriepengedifferansenForArbeidsgiver(orgnummer: String): Double {
        val grunnlag = feriepengedager().filter(ARBEIDSGIVER and orgnummerFilter(orgnummer)).summer()
        val grunnlagUtbetaltAvInfotrygd = grunnlagForFeriepengerUtbetaltAvInfotrygd(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag - grunnlagUtbetaltAvInfotrygd)
    }

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygd(orgnummer: String): Int {
        val itFeriepengedager = utbetalteDager.filter(INFOTRYGD).feriepengedager().flatMap { (_, dagListe) -> dagListe }
        return itFeriepengedager.filter(INFOTRYGD_ARBEIDSGIVER and orgnummerFilter(orgnummer)).summer()
    }

    private fun beregnForFilter(filter: UtbetaltDagSelector): Double {
        val grunnlag = feriepengedager().filter(filter).summer()
        return alder.beregnFeriepenger(opptjeningsår, grunnlag)
    }

    private fun beregnForFilterUtenPersonhack(filter: UtbetaltDagSelector): Double {
        val grunnlag = feriepengedagerUtenPersonhack().filter(filter).summer()
        return alder.beregnFeriepenger(opptjeningsår, grunnlag)
    }

    private fun feriepengedager(): List<UtbetaltDag> {
        val itFeriepenger = utbetalteDager.filter(INFOTRYGD).feriepengedager()
        val personreserverteDatoer = itFeriepenger.filter { (_, dager) -> dager.any(INFOTRYGD_PERSON) }.map { (dato, _) -> dato }

        return utbetalteDager
            .feriepengedager(personreserverteDatoer)
            .flatMap { (_, dagListe) -> dagListe }
    }

    private fun feriepengedagerUtenPersonhack() = utbetalteDager
        .feriepengedager()
        .flatMap { (_, dagListe) -> dagListe }

    internal sealed class UtbetaltDag(
        protected val orgnummer: String,
        protected val dato: LocalDate,
        protected val beløp: Int
    ) {
        internal companion object {
            internal fun List<UtbetaltDag>.tilDato() = map { it.dato }.distinct()
            private fun List<LocalDate>.inneholderBeggeEllerIngen(dato1: LocalDate, dato2: LocalDate) = (dato1 !in this).xor(dato2 in this)
            internal fun List<UtbetaltDag>.feriepengedager(personreserverteDatoer: List<LocalDate> = emptyList()) = this
                .sortedWith { utbetaltDag1, utbetaltDag2 ->
                    when {
                        personreserverteDatoer.inneholderBeggeEllerIngen(utbetaltDag1.dato, utbetaltDag2.dato) -> utbetaltDag1.dato.compareTo(utbetaltDag2.dato)
                        utbetaltDag1.dato in personreserverteDatoer -> -1
                        else -> 1
                    }
                }
                .groupBy { it.dato }
                .entries
                .take(MAGIC_NUMBER)
                .sortedBy { (dato, _) -> dato }

            internal fun List<UtbetaltDag>.summer() = sumBy { it.beløp }

            internal val INFOTRYGD_PERSON: UtbetaltDagSelector = { it is InfotrygdPerson }
            internal val INFOTRYGD_ARBEIDSGIVER: UtbetaltDagSelector = { it is InfotrygdArbeidsgiver }
            internal val INFOTRYGD: UtbetaltDagSelector = INFOTRYGD_PERSON or INFOTRYGD_ARBEIDSGIVER
            internal val SPLEIS_ARBEIDSGIVER: UtbetaltDagSelector = { it is SpleisArbeidsgiver }
            internal val ARBEIDSGIVER: UtbetaltDagSelector = INFOTRYGD_ARBEIDSGIVER or SPLEIS_ARBEIDSGIVER
            internal fun orgnummerFilter(orgnummer: String): UtbetaltDagSelector = { it.orgnummer == orgnummer }
            private infix fun (UtbetaltDagSelector).or(other: UtbetaltDagSelector): UtbetaltDagSelector = { this(it) || other(it) }
            internal infix fun (UtbetaltDagSelector).and(other: UtbetaltDagSelector): UtbetaltDagSelector = { this(it) && other(it) }
        }

        internal abstract fun accept(visitor: FeriepengeutbetalingVisitor)

        internal class InfotrygdPerson(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun accept(visitor: FeriepengeutbetalingVisitor) {
                visitor.visitInfotrygdPersonDag(this, orgnummer, dato, beløp)
            }
        }

        internal class InfotrygdArbeidsgiver(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun accept(visitor: FeriepengeutbetalingVisitor) {
                visitor.visitInfotrygdArbeidsgiverDag(this, orgnummer, dato, beløp)
            }
        }

        internal class SpleisArbeidsgiver(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun accept(visitor: FeriepengeutbetalingVisitor) {
                visitor.visitSpleisArbeidsgiverDag(this, orgnummer, dato, beløp)
            }
        }
    }

    private class FinnUtbetalteDagerVisitor(
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        person: Person
    ) {
        private val utbetalteDager = mutableListOf<UtbetaltDag>()

        init {
            utbetalingshistorikkForFeriepenger.accept(InfotrygdUtbetalteDagerVisitor())
            person.accept(SpleisUtbetalteDagerVisitor())
        }

        fun utbetalteDager() = utbetalteDager.toList()

        private inner class InfotrygdUtbetalteDagerVisitor : FeriepengeutbetalingsperiodeVisitor {
            override fun visitPersonutbetalingsperiode(orgnr: String, periode: Periode, beløp: Int) {
                utbetalteDager.addAll(periode.filterNot { it.erHelg() }
                    .map { UtbetaltDag.InfotrygdPerson(orgnr, it, beløp) })
            }

            override fun visitArbeidsgiverutbetalingsperiode(orgnr: String, periode: Periode, beløp: Int) {
                utbetalteDager.addAll(periode.filterNot { it.erHelg() }
                    .map { UtbetaltDag.InfotrygdArbeidsgiver(orgnr, it, beløp) })
            }
        }

        private inner class SpleisUtbetalteDagerVisitor : PersonVisitor {
            private lateinit var orgnummer: String
            override fun preVisitArbeidsgiver(arbeidsgiver: Arbeidsgiver, id: UUID, organisasjonsnummer: String) {
                this.orgnummer = organisasjonsnummer
            }

            private var inUtbetalinger = false
            override fun preVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
                inUtbetalinger = true
            }

            override fun postVisitUtbetalinger(utbetalinger: List<Utbetaling>) {
                inUtbetalinger = false
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
                satstype: Satstype,
                beløp: Int?,
                aktuellDagsinntekt: Int?,
                grad: Double?,
                delytelseId: Int,
                refDelytelseId: Int?,
                refFagsystemId: String?,
                endringskode: Endringskode,
                datoStatusFom: LocalDate?,
                klassekode: Klassekode
            ) {
                if (inUtbetalinger && utbetaltUtbetaling && beløp != null) {
                    utbetalteDager.addAll((fom til tom).filterNot { it.erHelg() }.map { UtbetaltDag.SpleisArbeidsgiver(this.orgnummer, it, beløp) })
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
        }
    }
}
