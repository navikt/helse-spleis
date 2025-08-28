package no.nav.helse.feriepenger

import java.time.Month.DECEMBER
import java.time.Year
import kotlin.math.roundToInt
import no.nav.helse.Alder
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.ARBEIDSGIVER
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.PERSON
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.summer
import no.nav.helse.feriepenger.Feriepengeberegningsresultat.Beregningsverdier
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

    fun beregnFeriepenger(orgnummer: String): Feriepengeberegningsresultat {
        // de første 48 dagene, infotrygd + spleis samlet
        val faktiskFeriepengegrunnlag = feriepengedager.grunnlagFor(orgnummer)
        // de første 48 dagene i infotrygd
        val infotrygdFeriepengegrunnlag = feriepengedagerInfotrygd.grunnlagFor(orgnummer)

        val arbeidsgiverrefusjon = faktiskFeriepengegrunnlag.refusjonsresultat - infotrygdFeriepengegrunnlag.refusjonsresultat
        val brukerutbetaling = faktiskFeriepengegrunnlag.personresultat - infotrygdFeriepengegrunnlag.personresultat

        // av de første 48 dagene (samlet sett), spleis sin del
        val spleisdel = feriepengedagerSpleisdel.grunnlagFor(orgnummer)
        // av de første 48 dagene (samlet sett), infotrygd sin del
        val infotrygddel = feriepengedagerInfotrygddel.grunnlagFor(orgnummer)

        val refusjon = Beregningsverdier(
            infotrygdFeriepengebeløp = infotrygddel.refusjonsresultat.utbetalingsgrunnlag,
            spleisFeriepengebeløp = spleisdel.refusjonsresultat.utbetalingsgrunnlag,
            totaltFeriepengebeløp = faktiskFeriepengegrunnlag.refusjonsresultat.utbetalingsgrunnlag,
            differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = arbeidsgiverrefusjon,
            hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = infotrygdFeriepengegrunnlag.refusjonsresultat.utbetalingsgrunnlag
        )
        val person = Beregningsverdier(
            infotrygdFeriepengebeløp = infotrygddel.personresultat.utbetalingsgrunnlag,
            spleisFeriepengebeløp = spleisdel.personresultat.utbetalingsgrunnlag,
            totaltFeriepengebeløp = faktiskFeriepengegrunnlag.personresultat.utbetalingsgrunnlag,
            differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = brukerutbetaling,
            hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = infotrygdFeriepengegrunnlag.personresultat.utbetalingsgrunnlag
        )
        return Feriepengeberegningsresultat(
            orgnummer = orgnummer,
            arbeidsgiver = refusjon,
            person = person
        )
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer: String): Double {
        return beregnFeriepenger(orgnummer).arbeidsgiver.hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble
    }
}

internal data class Feriepengeberegningsresultat(
    val orgnummer: String,
    val arbeidsgiver: Beregningsverdier,
    val person: Beregningsverdier
) {
    internal data class Beregningsverdier(
        val infotrygdFeriepengebeløp: Double,
        val spleisFeriepengebeløp: Double,
        val totaltFeriepengebeløp: Double,
        val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd: Int,
        val hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble: Double
    ) {
        val hvaViHarBeregnetAtInfotrygdHarUtbetalt: Int = hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble.roundToInt()
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

            operator fun minus(other: BeregnetResultat): Int {
                return (this.utbetalingsgrunnlag - other.utbetalingsgrunnlag).roundToInt()
            }

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
