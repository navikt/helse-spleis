package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import java.time.Year
import no.nav.helse.Alder
import no.nav.helse.dto.FeriepengeberegnerDto
import no.nav.helse.dto.UtbetaltDagDto
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.utbetalingslinjer.Arbeidsgiverferiepengegrunnlag
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.INFOTRYGD_PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.SPLEIS_ARBEIDSGIVER
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.SPLEIS_PERSON
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.and
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.feriepengedager
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.opptjeningsårFilter
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.orgnummerFilter
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.summer
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.Companion.tilDato
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.InfotrygdPerson
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner.UtbetaltDag.SpleisPerson

private typealias UtbetaltDagSelector = (Feriepengeberegner.UtbetaltDag) -> Boolean

internal class Feriepengeberegner(
    private val alder: Alder,
    private val opptjeningsår: Year,
    private val utbetalteDager: List<UtbetaltDag>
) {
    internal companion object {

        internal fun gjenopprett(alder: Alder, dto: FeriepengeberegnerDto) =
            Feriepengeberegner(
                alder = alder,
                opptjeningsår = dto.opptjeningsår,
                utbetalteDager = dto.utbetalteDager.map { UtbetaltDag.gjenopprett(it) }
            )

        private const val ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET = 48

        private fun utbetalteDager(grunnlagFraInfotrygd: List<Arbeidsgiverferiepengegrunnlag>, grunnlagFraSpleisPerson: List<Arbeidsgiverferiepengegrunnlag>, opptjeningsår: Year): List<UtbetaltDag> {
            val infotrygd = grunnlagFraInfotrygd.flatMap { arbeidsgiver ->
                arbeidsgiver.utbetalinger.flatMap { utbetaling ->
                    utbetaling.arbeidsgiverUtbetalteDager.map { dag ->
                        InfotrygdArbeidsgiver(orgnummer = arbeidsgiver.orgnummer, dato = dag.dato, beløp = dag.beløp)
                    } + utbetaling.personUtbetalteDager.map { dag ->
                        InfotrygdPerson(orgnummer = arbeidsgiver.orgnummer, dato = dag.dato, beløp = dag.beløp)
                    }
                }
            }

            val spleis = grunnlagFraSpleisPerson.flatMap { arbeidsgiver ->
                arbeidsgiver.utbetalinger.flatMap { utbetaling ->
                    utbetaling.arbeidsgiverUtbetalteDager.map { dag ->
                        SpleisArbeidsgiver(orgnummer = arbeidsgiver.orgnummer, dato = dag.dato, beløp = dag.beløp)
                    } + utbetaling.personUtbetalteDager.map { dag ->
                        SpleisPerson(orgnummer = arbeidsgiver.orgnummer, dato = dag.dato, beløp = dag.beløp)
                    }
                }
            }

            return (infotrygd + spleis).filter(opptjeningsårFilter(opptjeningsår))
        }
    }

    internal constructor(
        alder: Alder,
        opptjeningsår: Year,
        grunnlagFraInfotrygd: List<Arbeidsgiverferiepengegrunnlag>,
        grunnlagFraSpleis: List<Arbeidsgiverferiepengegrunnlag>
    ) : this(alder, opptjeningsår, utbetalteDager(grunnlagFraInfotrygd, grunnlagFraSpleis, opptjeningsår))

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

            internal fun gjenopprett(dto: UtbetaltDagDto) =
                when (dto) {
                    is UtbetaltDagDto.InfotrygdArbeidsgiver -> InfotrygdArbeidsgiver.gjenopprett(dto)
                    is UtbetaltDagDto.InfotrygdPerson -> InfotrygdPerson.gjenopprett(dto)
                    is UtbetaltDagDto.SpleisArbeidsgiver -> SpleisArbeidsgiver.gjenopprett(dto)
                    is UtbetaltDagDto.SpleisPerson -> SpleisPerson.gjenopprett(dto)
                }
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
            override fun dto() = UtbetaltDagDto.InfotrygdPerson(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: UtbetaltDagDto.InfotrygdPerson): InfotrygdPerson {
                    return InfotrygdPerson(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
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
            override fun dto() = UtbetaltDagDto.InfotrygdArbeidsgiver(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: UtbetaltDagDto.InfotrygdArbeidsgiver): InfotrygdArbeidsgiver {
                    return InfotrygdArbeidsgiver(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
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
            override fun dto() = UtbetaltDagDto.SpleisArbeidsgiver(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: UtbetaltDagDto.SpleisArbeidsgiver): SpleisArbeidsgiver {
                    return SpleisArbeidsgiver(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
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

            override fun dto() = UtbetaltDagDto.SpleisPerson(orgnummer, dato, beløp)

            internal companion object {
                internal fun gjenopprett(dto: UtbetaltDagDto.SpleisPerson): SpleisPerson {
                    return SpleisPerson(
                        orgnummer = dto.orgnummer,
                        dato = dto.dato,
                        beløp = dto.beløp
                    )
                }
            }
        }

        abstract fun dto(): UtbetaltDagDto
    }

    internal fun dto() = FeriepengeberegnerDto(
        opptjeningsår = this.opptjeningsår,
        utbetalteDager = this.utbetalteDager.map { it.dto() },
        feriepengedager = this.feriepengedager().map { it.dto() }
    )
}
