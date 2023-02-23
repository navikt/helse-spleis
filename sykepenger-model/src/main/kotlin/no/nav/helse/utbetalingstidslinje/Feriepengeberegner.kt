package no.nav.helse.utbetalingstidslinje

import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID
import no.nav.helse.Alder
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.SimuleringResultat
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.person.FeriepengeutbetalingsperiodeVisitor
import no.nav.helse.person.Person
import no.nav.helse.person.PersonVisitor
import no.nav.helse.utbetalingslinjer.Endringskode
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Klassekode
import no.nav.helse.utbetalingslinjer.Oppdrag
import no.nav.helse.utbetalingslinjer.Oppdragstatus
import no.nav.helse.utbetalingslinjer.Satstype
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.Utbetalingtype
import no.nav.helse.utbetalingslinjer.Utbetalingslinje
import no.nav.helse.utbetalingslinjer.Utbetalingstatus
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.SPLEIS_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.SPLEIS_PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.and
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.feriepengedager
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.opptjeningsårFilter
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.orgnummerFilter
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.summer
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.tilDato

private typealias UtbetaltDagSelector = (Feriepengeberegner.UtbetaltDag) -> Boolean

internal class Feriepengeberegner(
    private val alder: Alder,
    private val opptjeningsår: Year,
    private val utbetalteDager: List<UtbetaltDag>
) {
    internal companion object {
        internal fun ferdigFeriepengeberegner(alder: Alder, opptjeningsår: Year, utbetalteDager: List<UtbetaltDag>): Feriepengeberegner =
            Feriepengeberegner(alder, opptjeningsår, utbetalteDager)

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
        visitor.postVisitFeriepengeberegner(this, feriepengedager(), opptjeningsår, utbetalteDager)
    }
    internal fun gjelderForÅr(år: Year) = opptjeningsår == år
    internal fun feriepengedatoer() = feriepengedager().tilDato()
    internal fun beregnFeriepengerForInfotrygdPerson() = beregnForFilter(INFOTRYGD_PERSON)
    internal fun beregnFeriepengerForInfotrygdPerson(orgnummer: String) = beregnForFilter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer))

    internal fun beregnFeriepengerForInfotrygdArbeidsgiver() = beregnForFilter(INFOTRYGD_ARBEIDSGIVER)
    internal fun beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer: String) = beregnForFilter(INFOTRYGD_ARBEIDSGIVER and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForSpleisArbeidsgiver() = beregnForFilter(SPLEIS_ARBEIDSGIVER)
    internal fun beregnFeriepengerForSpleisArbeidsgiver(orgnummer: String) = beregnForFilter(SPLEIS_ARBEIDSGIVER and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForSpleisPerson(orgnummer: String) = beregnForFilter(SPLEIS_PERSON and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForInfotrygd() = beregnForFilter(INFOTRYGD)
    internal fun beregnFeriepengerForInfotrygd(orgnummer: String) = beregnForFilter(INFOTRYGD and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForArbeidsgiver(orgnummer: String) = beregnForFilter(ARBEIDSGIVER and orgnummerFilter(orgnummer))
    internal fun beregnFeriepengerForPerson(orgnummer: String) = beregnForFilter(PERSON and orgnummerFilter(orgnummer))
    internal fun beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygdTilArbeidsgiver(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag)
    }
    internal fun beregnUtbetalteFeriepengerForInfotrygdPerson(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygdTilPerson(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag)
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdPersonForEnArbeidsgiver(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygdTilPersonForArbeidsgiver(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag)
    }

    internal fun beregnFeriepengedifferansenForArbeidsgiver(orgnummer: String): Double {
        val grunnlag = feriepengedager().filter(ARBEIDSGIVER and orgnummerFilter(orgnummer)).summer()
        val grunnlagUtbetaltAvInfotrygd = grunnlagForFeriepengerUtbetaltAvInfotrygdTilArbeidsgiver(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag - grunnlagUtbetaltAvInfotrygd)
    }

    internal fun beregnFeriepengedifferansenForPerson(orgnummer: String): Double {
        val grunnlag = feriepengedager().filter(PERSON and orgnummerFilter(orgnummer)).summer()
        val grunnlagUtbetaltAvInfotrygd = grunnlagForFeriepengerUtbetaltAvInfotrygdTilPerson(orgnummer)
        return alder.beregnFeriepenger(opptjeningsår, grunnlag - grunnlagUtbetaltAvInfotrygd)
    }

    private fun første48dageneUtbetaltIInfotrygd() = utbetalteDager.filter(INFOTRYGD).feriepengedager().flatMap { (_, dagListe) -> dagListe }

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygdTilArbeidsgiver(orgnummer: String) =
        første48dageneUtbetaltIInfotrygd().filter(INFOTRYGD_ARBEIDSGIVER and orgnummerFilter(orgnummer)).summer()

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygdTilPerson(orgnummer: String) =
        første48dageneUtbetaltIInfotrygd().filter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer)).summer()

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygdTilPersonForArbeidsgiver(orgnummer: String): Int {
        val itFeriepengedager = utbetalteDager.filter(INFOTRYGD).feriepengedager().flatMap { (_, dagListe) -> dagListe }
        return itFeriepengedager.filter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer)).summer()
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
                //TODO: subsumsjonObserver.`§8-33 ledd 1`() //TODO: Finne ut hvordan vi løser denne mtp. input Infotrygd/Spleis og pr. arbeidsgiver
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
            internal val SPLEIS_PERSON: UtbetaltDagSelector = { it is SpleisPerson }
            internal val ARBEIDSGIVER: UtbetaltDagSelector = INFOTRYGD_ARBEIDSGIVER or SPLEIS_ARBEIDSGIVER
            internal val PERSON: UtbetaltDagSelector = INFOTRYGD_PERSON or SPLEIS_PERSON
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
        internal class SpleisPerson(
            orgnummer: String,
            dato: LocalDate,
            beløp: Int
        ) : UtbetaltDag(orgnummer, dato, beløp) {
            override fun accept(visitor: FeriepengeutbetalingVisitor) {
                visitor.visitSpleisPersonDag(this, orgnummer, dato, beløp)
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
            private val DATO_FOR_SISTE_FERIEPENGEKJØRING_I_INFOTRYGD = LocalDate.of(2023, 1, 14)
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
            var oppdragFagområde: Fagområde? = null
            private fun dagFabrikk(orgnummer: String, dato: LocalDate, beløp: Int): UtbetaltDag =
                when(oppdragFagområde) {
                    Fagområde.Sykepenger -> UtbetaltDag.SpleisPerson(orgnummer, dato, beløp)
                    Fagområde.SykepengerRefusjon -> UtbetaltDag.SpleisArbeidsgiver(orgnummer, dato, beløp)
                    else -> throw IllegalStateException("Kunne ikke lage UtbetaltDag for fagområde $oppdragFagområde")
                }

            override fun preVisitUtbetaling(
                utbetaling: Utbetaling,
                id: UUID,
                korrelasjonsId: UUID,
                type: Utbetalingtype,
                tilstand: Utbetaling.Tilstand,
                periode: Periode,
                tidsstempel: LocalDateTime,
                oppdatert: LocalDateTime,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?,
                stønadsdager: Int,
                beregningId: UUID,
                overføringstidspunkt: LocalDateTime?,
                avsluttet: LocalDateTime?,
                avstemmingsnøkkel: Long?
            ) {
                utbetaltUtbetaling = tilstand == Utbetaling.Utbetalt
                annullertUtbetaling = tilstand == Utbetaling.Annullert
            }

            override fun preVisitOppdrag(
                oppdrag: Oppdrag,
                fagområde: Fagområde,
                fagsystemId: String,
                mottaker: String,
                sisteArbeidsgiverdag: LocalDate?,
                stønadsdager: Int,
                totalBeløp: Int,
                nettoBeløp: Int,
                tidsstempel: LocalDateTime,
                endringskode: Endringskode,
                avstemmingsnøkkel: Long?,
                status: Oppdragstatus?,
                overføringstidspunkt: LocalDateTime?,
                erSimulert: Boolean,
                simuleringsResultat: SimuleringResultat?
            ) {
                if (utbetaltUtbetaling || annullertUtbetaling) {
                    utbetalteDagerForOppdrag = mutableListOf()
                    utbetalteDagerForFagsystemId[oppdrag.fagsystemId()] = utbetalteDagerForOppdrag
                    oppdragFagområde = fagområde
                }
            }

            override fun visitUtbetalingslinje(
                linje: Utbetalingslinje,
                fom: LocalDate,
                tom: LocalDate,
                stønadsdager: Int,
                totalbeløp: Int,
                satstype: Satstype,
                beløp: Int?,
                aktuellDagsinntekt: Int?,
                grad: Int?,
                delytelseId: Int,
                refDelytelseId: Int?,
                refFagsystemId: String?,
                endringskode: Endringskode,
                datoStatusFom: LocalDate?,
                statuskode: String?,
                klassekode: Klassekode
            ) {
                if (inUtbetalinger && utbetaltUtbetaling && beløp != null && datoStatusFom == null) {
                    utbetalteDagerForOppdrag.addAll((fom til tom).filterNot { it.erHelg() }.map {
                        dagFabrikk(this.orgnummer, it, beløp)
                    })
                }
            }

            override fun postVisitUtbetaling(
                utbetaling: Utbetaling,
                id: UUID,
                korrelasjonsId: UUID,
                type: Utbetalingtype,
                tilstand: Utbetalingstatus,
                periode: Periode,
                tidsstempel: LocalDateTime,
                oppdatert: LocalDateTime,
                arbeidsgiverNettoBeløp: Int,
                personNettoBeløp: Int,
                maksdato: LocalDate,
                forbrukteSykedager: Int?,
                gjenståendeSykedager: Int?,
                stønadsdager: Int,
                beregningId: UUID,
                overføringstidspunkt: LocalDateTime?,
                avsluttet: LocalDateTime?,
                avstemmingsnøkkel: Long?
            ) {
                utbetaltUtbetaling = false
            }
        }
    }
}
