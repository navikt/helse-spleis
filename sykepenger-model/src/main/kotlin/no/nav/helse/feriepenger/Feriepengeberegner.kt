package no.nav.helse.feriepenger

import java.time.Month.DECEMBER
import java.time.Year
import kotlin.math.roundToInt
import no.nav.helse.Alder
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.ARBEIDSGIVER
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.PERSON
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.summer
import no.nav.helse.feriepenger.Feriepengedager.DagerForArbeidsgiver.BeregnetResultat.Companion.grunnlag
import no.nav.helse.feriepenger.Feriepengeutbetalinggrunnlag.UtbetaltDag
import no.nav.helse.feriepenger.Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdArbeidsgiver
import no.nav.helse.feriepenger.Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdPerson
import no.nav.helse.feriepenger.Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisArbeidsgiver
import no.nav.helse.feriepenger.Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisPerson

private typealias UtbetaltDagSelector = (UtbetaltDag) -> Boolean

internal class Feriepengeberegner(
    alder: Alder,
    val opptjeningsår: Year,
    val utbetalteDager: List<UtbetaltDag>
) {
    val alderVedSluttenAvÅret = alder.alderPåDato(opptjeningsår.atMonth(DECEMBER).atEndOfMonth())
    val prosentsats = if (alderVedSluttenAvÅret < ALDER_FOR_FORHØYET_FERIEPENGESATS) 0.102 else 0.125

    val feriepengedager = utbetalteDager.feriepengedager(prosentsats)
    val feriepengedagerInfotrygddel = feriepengedager.dager.filter(INFOTRYGD).feriepengedager(prosentsats)
    val feriepengedagerSpleisdel = feriepengedager.dager.filter(SPLEIS).feriepengedager(prosentsats)

    val feriepengedagerInfotrygd = utbetalteDager.filter(INFOTRYGD).feriepengedager(prosentsats)

    internal companion object {
        private const val ALDER_FOR_FORHØYET_FERIEPENGESATS = 59
        private const val ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET = 48

        internal fun List<UtbetaltDag>.tilDato() = map { it.dato }.distinct()
        internal fun List<UtbetaltDag>.feriepengedager(prosentsats: Double): Feriepengedager {
            //TODO: subsumsjonObserver.`§8-33 ledd 1`() //TODO: Finne ut hvordan vi løser denne mtp. input Infotrygd/Spleis og pr. arbeidsgiver
            return Feriepengedager(
                prosentsats = prosentsats,
                dager = this
                    .sortedBy { it.dato }
                    .groupBy { it.dato }
                    .entries
                    .take(ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET)
                    .flatMap { (_, v) -> v }
            )
        }

        internal fun List<UtbetaltDag>.summer() = sumOf { it.beløp }

        internal val INFOTRYGD_PERSON: UtbetaltDagSelector = { it is InfotrygdPerson }
        internal val INFOTRYGD_ARBEIDSGIVER: UtbetaltDagSelector = { it is InfotrygdArbeidsgiver }
        internal val INFOTRYGD: UtbetaltDagSelector = INFOTRYGD_PERSON or INFOTRYGD_ARBEIDSGIVER
        internal val SPLEIS_ARBEIDSGIVER: UtbetaltDagSelector = { it is SpleisArbeidsgiver }
        internal val SPLEIS_PERSON: UtbetaltDagSelector = { it is SpleisPerson }
        internal val SPLEIS: UtbetaltDagSelector = SPLEIS_PERSON or SPLEIS_ARBEIDSGIVER
        internal val ARBEIDSGIVER: UtbetaltDagSelector = INFOTRYGD_ARBEIDSGIVER or SPLEIS_ARBEIDSGIVER
        internal val PERSON: UtbetaltDagSelector = INFOTRYGD_PERSON or SPLEIS_PERSON
        internal fun orgnummerFilter(orgnummer: String): UtbetaltDagSelector = { it.orgnummer == orgnummer }
        internal fun opptjeningsårFilter(opptjeningsår: Year): UtbetaltDagSelector = { Year.from(it.dato) == opptjeningsår }
        private infix fun (UtbetaltDagSelector).or(other: UtbetaltDagSelector): UtbetaltDagSelector = { this(it) || other(it) }
        internal infix fun (UtbetaltDagSelector).and(other: UtbetaltDagSelector): UtbetaltDagSelector = { this(it) && other(it) }

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

    fun grunnlag() = Feriepengeutbetalinggrunnlag(
        opptjeningsår = opptjeningsår,
        utbetalteDager = utbetalteDager,
        feriepengedager = feriepengedager.dager
    )

    internal fun feriepengedatoer() = feriepengedager.dager.tilDato()
    internal fun beregnFeriepengerForInfotrygdPerson() = feriepengedagerInfotrygddel.persongrunnlag.personresultat.utbetalingsgrunnlag
    internal fun beregnFeriepengerForInfotrygdPerson(orgnummer: String) = feriepengedagerInfotrygddel.grunnlagFor(orgnummer).personresultat.utbetalingsgrunnlag

    internal fun beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer: String) = feriepengedagerInfotrygddel.grunnlagFor(orgnummer).refusjonsresultat.utbetalingsgrunnlag
    internal fun beregnFeriepengerForSpleisArbeidsgiver(orgnummer: String) = feriepengedagerSpleisdel.grunnlagFor(orgnummer).refusjonsresultat.utbetalingsgrunnlag
    internal fun beregnFeriepengerForSpleisPerson(orgnummer: String) = feriepengedagerSpleisdel.grunnlagFor(orgnummer).personresultat.utbetalingsgrunnlag
    internal fun beregnFeriepengerForArbeidsgiver(orgnummer: String) = feriepengedager.grunnlagFor(orgnummer).refusjonsresultat.utbetalingsgrunnlag
    internal fun beregnFeriepengerForPerson(orgnummer: String) = feriepengedager.grunnlagFor(orgnummer).personresultat.utbetalingsgrunnlag

    // kun bruk i test:
    internal fun beregnFeriepengerForInfotrygd(orgnummer: String) = feriepengedagerInfotrygddel.grunnlagFor(orgnummer).refusjonsresultat.utbetalingsgrunnlag + feriepengedagerInfotrygddel.grunnlagFor(orgnummer).personresultat.utbetalingsgrunnlag
    internal fun beregnFeriepengerForInfotrygdArbeidsgiver() = feriepengedagerInfotrygddel.dagerForArbeidsgivere.values.sumOf { it.refusjonsresultat.utbetalingsgrunnlag }
    internal fun beregnFeriepengerForSpleisArbeidsgiver() = feriepengedagerSpleisdel.dagerForArbeidsgivere.values.sumOf { it.refusjonsresultat.utbetalingsgrunnlag }
    internal fun beregnFeriepengerForInfotrygd() = feriepengedagerInfotrygddel.persongrunnlag.refusjonsresultat.utbetalingsgrunnlag + feriepengedagerInfotrygddel.persongrunnlag.personresultat.utbetalingsgrunnlag

    private fun beregnFeriepenger(beløp: Int): Double {
        val feriepenger = beløp * prosentsats
        //TODO: subsumsjonObserver.`§8-33 ledd 3`(beløp, opptjeningsår, prosentsats, alderVedSluttenAvÅret, feriepenger)
        return feriepenger
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer: String): Double {
        return feriepengedagerInfotrygd.grunnlagFor(orgnummer).refusjonsresultat.utbetalingsgrunnlag
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdPerson(orgnummer: String): Double {
        return feriepengedagerInfotrygd.grunnlagFor(orgnummer).personresultat.utbetalingsgrunnlag
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdPersonForEnArbeidsgiver(orgnummer: String): Double {
        return beregnUtbetalteFeriepengerForInfotrygdPerson(orgnummer)
    }

    internal fun beregnFeriepengedifferansenForArbeidsgiver(orgnummer: String): Int {
        return resultat(orgnummer, Feriepengedager.DagerForArbeidsgiver::refusjonsresultat)
    }

    internal fun beregnFeriepengedifferansenForPerson(orgnummer: String): Int {
        return resultat(orgnummer, Feriepengedager.DagerForArbeidsgiver::personresultat)
    }

    private fun resultat(orgnummer: String, velger: (Feriepengedager.DagerForArbeidsgiver) -> Feriepengedager.DagerForArbeidsgiver.BeregnetResultat): Int {
        val spleis = feriepengedager.grunnlagFor(orgnummer)
        val infotrygd = feriepengedagerInfotrygd.grunnlagFor(orgnummer)
        return (velger(spleis).utbetalingsgrunnlag - velger(infotrygd).utbetalingsgrunnlag).roundToInt()
    }
}

internal data class Feriepengedager(
    val prosentsats: Double,
    val dager: List<UtbetaltDag>
) {
    val dagerForArbeidsgivere = dager
        .groupBy { it.orgnummer }
        .mapValues { DagerForArbeidsgiver(prosentsats, it.value) }

    val persongrunnlag = DagerForArbeidsgiver(prosentsats, dager)

    internal fun grunnlagFor(orgnummer: String) = dagerForArbeidsgivere[orgnummer] ?: DagerForArbeidsgiver(0.0, emptyList())

    data class DagerForArbeidsgiver(
        val prosentsats: Double, val dager: List<UtbetaltDag>
    ) {
        val refusjonsresultat = dager.filter(ARBEIDSGIVER).grunnlag(prosentsats)
        val personresultat = dager.filter(PERSON).grunnlag(prosentsats)

        data class BeregnetResultat(
            val feriepengegrunnlag: Int,
            val utbetalingsgrunnlag: Double
        ) {
            val utbetaling = utbetalingsgrunnlag.roundToInt()

            companion object {
                fun List<UtbetaltDag>.grunnlag(prosentsats: Double): BeregnetResultat {
                    val grunnlag = this.summer()
                    return BeregnetResultat(
                        feriepengegrunnlag = grunnlag,
                        utbetalingsgrunnlag = grunnlag * prosentsats
                    )
                }
            }
        }
    }
}
