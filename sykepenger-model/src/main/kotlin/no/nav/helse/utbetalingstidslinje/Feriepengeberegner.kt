package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.person.*
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Etterlevelse.Vurderingsresultat.Companion.`§8-33 ledd 1`
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.SPLEIS_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.and
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.feriepengedager
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.opptjeningsårFilter
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
        private const val ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET = 48
    }

    internal constructor(
        alder: Alder,
        opptjeningsår: Year,
        utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        person: Person
    ) : this(alder, opptjeningsår, FinnUtbetalteDagerVisitor(utbetalingshistorikkForFeriepenger, person).utbetalteDager(opptjeningsår))

    internal fun accept(visitor: FeriepengeutbetalingVisitor) {
        visitor.preVisitFeriepengeberegner(this, feriepengedager(), opptjeningsår, utbetalteDager)
        visitor.preVisitUtbetaleDager()
        utbetalteDager.forEach { it.accept(visitor) }
        visitor.postVisitUtbetaleDager()
        visitor.preVisitFeriepengedager()
        feriepengedager().forEach { it.accept(visitor) }
        visitor.postVisitFeriepengedager()
        visitor.postVisitFeriepengeberegner(this)
    }
    internal fun gjelderForÅr(år: Year) = opptjeningsår == år
    internal fun feriepengedatoer() = feriepengedager().tilDato()
    internal fun beregnFeriepengerForInfotrygdPerson() = beregnForFilter(INFOTRYGD_PERSON)
    internal fun beregnFeriepengerForInfotrygdPerson(orgnummer: String) = beregnForFilter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer))

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

    private fun feriepengedager() = utbetalteDager.feriepengedager().flatMap { (_, dagListe) -> dagListe }

    internal sealed class UtbetaltDag(
        protected val orgnummer: String,
        protected val dato: LocalDate,
        protected val beløp: Int
    ) {
        internal companion object {
            internal fun List<UtbetaltDag>.tilDato() = map { it.dato }.distinct()
            internal fun List<UtbetaltDag>.feriepengedager(): List<Map.Entry<LocalDate, List<UtbetaltDag>>> {
                Aktivitetslogg().`§8-33 ledd 1`() //TODO: Finne ut hvordan vi løser denne mtp. input Infotrygd/Spleis og pr. arbeidsgiver
                return this
                    .sortedBy { it.dato }
                    .groupBy { it.dato }
                    .entries
                    .take(ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET)
            }

            internal fun List<UtbetaltDag>.summer() = sumOf { it.beløp }

            internal val INFOTRYGD_PERSON: UtbetaltDagSelector = { it is InfotrygdPerson }
            internal val INFOTRYGD_ARBEIDSGIVER: UtbetaltDagSelector = { it is InfotrygdArbeidsgiver }
            internal val INFOTRYGD: UtbetaltDagSelector = INFOTRYGD_PERSON or INFOTRYGD_ARBEIDSGIVER
            internal val SPLEIS_ARBEIDSGIVER: UtbetaltDagSelector = { it is SpleisArbeidsgiver }
            internal val ARBEIDSGIVER: UtbetaltDagSelector = INFOTRYGD_ARBEIDSGIVER or SPLEIS_ARBEIDSGIVER
            internal fun orgnummerFilter(orgnummer: String): UtbetaltDagSelector = { it.orgnummer == orgnummer }
            internal fun opptjeningsårFilter(opptjeningsår: Year): UtbetaltDagSelector = { Year.from(it.dato) == opptjeningsår }
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
        private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger,
        person: Person
    ) {
        private val utbetalteDager = mutableListOf<UtbetaltDag>()

        init {
            utbetalingshistorikkForFeriepenger.accept(InfotrygdUtbetalteDagerVisitor())
            person.accept(SpleisUtbetalteDagerVisitor())
        }

        fun utbetalteDager(opptjeningsår: Year) = utbetalteDager.filter(opptjeningsårFilter(opptjeningsår))

        private companion object {
            // Hardkodet dato skal være datoen Infotrygd sist kjørte feriepenger
            private val DATO_FOR_SISTE_FERIEPENGEKJØRING_I_INFOTRYGD = LocalDate.of(2021, 11, 13)
        }

        private inner class InfotrygdUtbetalteDagerVisitor : FeriepengeutbetalingsperiodeVisitor {

            private fun erUtbetaltEtterFeriepengekjøringIT(utbetalt: LocalDate) = DATO_FOR_SISTE_FERIEPENGEKJØRING_I_INFOTRYGD <= utbetalt

            override fun visitPersonutbetalingsperiode(orgnr: String, periode: Periode, beløp: Int, utbetalt: LocalDate) {
                if(erUtbetaltEtterFeriepengekjøringIT(utbetalt)) return
                utbetalteDager.addAll(periode
                    .filterNot { it.erHelg() }
                    .filter { utbetalingshistorikkForFeriepenger.harRettPåFeriepenger(it, orgnr) }
                    .map { UtbetaltDag.InfotrygdPerson(orgnr, it, beløp) })
            }

            override fun visitArbeidsgiverutbetalingsperiode(orgnr: String, periode: Periode, beløp: Int, utbetalt: LocalDate) {
                if(erUtbetaltEtterFeriepengekjøringIT(utbetalt)) return
                utbetalteDager.addAll(periode
                    .filterNot { it.erHelg() }
                    .filter { utbetalingshistorikkForFeriepenger.harRettPåFeriepenger(it, orgnr) }
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
                utbetalteDager.addAll(utbetalteDagerForFagsystemId.values.flatten())
            }

            private var utbetaltUtbetaling = false
            private var annullertUtbetaling = false
            val utbetalteDagerForFagsystemId = mutableMapOf<String, MutableList<UtbetaltDag>>()
            var utbetalteDagerForOppdrag = mutableListOf<UtbetaltDag>()

            override fun preVisitUtbetaling(
                utbetaling: Utbetaling,
                id: UUID,
                korrelasjonsId: UUID,
                type: Utbetaling.Utbetalingtype,
                tilstand: Utbetaling.Tilstand,
                tidsstempel: LocalDateTime,
                oppdatert: LocalDateTime,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?,
                stønadsdager: Int,
                beregningId: UUID
            ) {
                utbetaltUtbetaling = tilstand == Utbetaling.Utbetalt
                annullertUtbetaling = tilstand == Utbetaling.Annullert
            }

            override fun preVisitOppdrag(
                oppdrag: Oppdrag,
                totalBeløp: Int,
                nettoBeløp: Int,
                tidsstempel: LocalDateTime,
                endringskode: Endringskode,
                avstemmingsnøkkel: Long?,
                status: Oppdragstatus?,
                overføringstidspunkt: LocalDateTime?
            ) {
                if (utbetaltUtbetaling || annullertUtbetaling) {
                    utbetalteDagerForOppdrag = mutableListOf()
                    utbetalteDagerForFagsystemId[oppdrag.fagsystemId()] = utbetalteDagerForOppdrag
                }
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
                if (inUtbetalinger && utbetaltUtbetaling && beløp != null && datoStatusFom == null) {
                    utbetalteDagerForOppdrag.addAll((fom til tom).filterNot { it.erHelg() }.map {
                        UtbetaltDag.SpleisArbeidsgiver(this.orgnummer, it, beløp)
                    })
                }
            }

            override fun postVisitUtbetaling(
                utbetaling: Utbetaling,
                id: UUID,
                korrelasjonsId: UUID,
                type: Utbetaling.Utbetalingtype,
                tilstand: Utbetaling.Tilstand,
                tidsstempel: LocalDateTime,
                oppdatert: LocalDateTime,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?,
                stønadsdager: Int,
                beregningId: UUID
            ) {
                utbetaltUtbetaling = false
            }
        }
    }
}
