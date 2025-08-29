package no.nav.helse.feriepenger

import java.time.LocalDate
import java.time.Month.DECEMBER
import java.time.Year
import kotlin.math.roundToInt
import no.nav.helse.Alder
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.ARBEIDSGIVER
import no.nav.helse.feriepenger.Feriepengeberegner.Companion.PERSON
import no.nav.helse.feriepenger.Feriepengeberegningsresultat.Beregningsverdier
import no.nav.helse.feriepenger.Feriepengegrunnlagsdag.Kilde
import no.nav.helse.feriepenger.Feriepengegrunnlagstidslinje.DagerForArbeidsgiver.BeregnetResultat.Companion.grunnlag

private typealias UtbetaltDagSelector = (Feriepengegrunnlagsdag.UtbetaltDag) -> Boolean

internal class Feriepengeberegner(
    alder: Alder,
    val opptjeningsår: Year,
    utbetalteDager: Feriepengegrunnlagstidslinje
) {
    val utbetalteDager = utbetalteDager.fra(opptjeningsår)

    val alderVedSluttenAvÅret = alder.alderPåDato(opptjeningsår.atMonth(DECEMBER).atEndOfMonth())
    val prosentsats = if (alderVedSluttenAvÅret < ALDER_FOR_FORHØYET_FERIEPENGESATS) 0.102 else 0.125

    // de første 48 dagene, infotrygd + spleis samlet
    val feriepengedager = this.utbetalteDager.feriepengegrunnlag()
    // av de første 48 dagene (samlet sett), infotrygd sin del
    val feriepengedagerInfotrygddel = feriepengedager.kun(Kilde.INFOTRYGD)
    // av de første 48 dagene (samlet sett), spleis sin del
    val feriepengedagerSpleisdel = feriepengedager.kun(Kilde.SPLEIS)

    // de første 48 dagene i infotrygd
    val feriepengedagerInfotrygd = this.utbetalteDager.kun(Kilde.INFOTRYGD).feriepengegrunnlag()

    internal companion object {
        private const val ALDER_FOR_FORHØYET_FERIEPENGESATS = 59

        internal val ARBEIDSGIVER: UtbetaltDagSelector = { it.mottaker == Feriepengegrunnlagsdag.Mottaker.ARBEIDSGIVER }
        internal val PERSON: UtbetaltDagSelector = { it.mottaker == Feriepengegrunnlagsdag.Mottaker.PERSON }
    }

    internal constructor(
        alder: Alder,
        opptjeningsår: Year,
        grunnlagFraInfotrygd: Feriepengegrunnlagstidslinje,
        grunnlagFraSpleis: Feriepengegrunnlagstidslinje
    ) : this(alder, opptjeningsår, grunnlagFraInfotrygd + grunnlagFraSpleis)

    fun beregnFeriepenger(orgnummer: String): Pair<Feriepengeberegningsresultat, Feriepengeutbetalinggrunnlag> {
        val faktiskFeriepengegrunnlag = feriepengedager.grunnlagFor(orgnummer, prosentsats)
        val infotrygdFeriepengegrunnlag = feriepengedagerInfotrygd.grunnlagFor(orgnummer, prosentsats)

        val arbeidsgiverrefusjon = faktiskFeriepengegrunnlag.refusjonsresultat - infotrygdFeriepengegrunnlag.refusjonsresultat
        val brukerutbetaling = faktiskFeriepengegrunnlag.personresultat - infotrygdFeriepengegrunnlag.personresultat

        val spleisdel = feriepengedagerSpleisdel.grunnlagFor(orgnummer, prosentsats)
        val infotrygddel = feriepengedagerInfotrygddel.grunnlagFor(orgnummer, prosentsats)

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
        val grunnlag = Feriepengeutbetalinggrunnlag(
            opptjeningsår = opptjeningsår,
            utbetalteDager = utbetalteDager.utbetalteDager(),
            feriepengedager = feriepengedager.utbetalteDager()
        )
        return Feriepengeberegningsresultat(
            orgnummer = orgnummer,
            arbeidsgiver = refusjon,
            person = person
        )  to grunnlag
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

internal data class Feriepengegrunnlagsdag(
    val dato: LocalDate,
    val utbetalinger: List<UtbetaltDag>
) {
    fun fra(opptjeningsår: Year) = takeIf { it.dato.year == opptjeningsår.value }

    fun kun(kilde: Kilde) = this
        .copy(utbetalinger = utbetalinger.filter { it.kilde == kilde })
        .takeIf { it.utbetalinger.isNotEmpty() }

    fun grunnlagFor(orgnummer: String) = this
        .copy(utbetalinger = utbetalinger.filter { it.orgnummer == orgnummer })
        .takeIf { it.utbetalinger.isNotEmpty() }

    data class UtbetaltDag(
        val orgnummer: String,
        val mottaker: Mottaker,
        val kilde: Kilde,
        val beløp: Int
    )
    enum class Mottaker {
        ARBEIDSGIVER, PERSON
    }
    enum class Kilde {
        INFOTRYGD, SPLEIS
    }
}

internal class Feriepengegrunnlagstidslinje(dager: Collection<Feriepengegrunnlagsdag>) {
    private val dager = dager.sortedBy { it.dato }

    init {
        require(dager.distinctBy { it.dato }.size == dager.size) {
            "Dager i en Feriepengegrunnlagstidslinje må være unike på dato"
        }
    }

    fun fra(opptjeningsår: Year): Feriepengegrunnlagstidslinje {
        return Feriepengegrunnlagstidslinje(
            dager = dager.mapNotNull { it.fra(opptjeningsår) }
        )
    }

    fun kun(kilde: Kilde): Feriepengegrunnlagstidslinje {
        return Feriepengegrunnlagstidslinje(
            dager = dager.mapNotNull { it.kun(kilde) }
        )
    }

    fun grunnlagFor(orgnummer: String, prosentsats: Double): DagerForArbeidsgiver {
        return DagerForArbeidsgiver(
            prosentsats = prosentsats,
            dager = this
                .dager
                .mapNotNull { it.grunnlagFor(orgnummer) }
        )
    }

    fun feriepengegrunnlag() = Feriepengegrunnlagstidslinje(
        dager = dager.take(ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET)
    )

    operator fun plus(other: Feriepengegrunnlagstidslinje): Feriepengegrunnlagstidslinje {
        val resultat = (this.dager + other.dager)
            .groupBy { it.dato }
            .map { (dato, dager) ->
                Feriepengegrunnlagsdag(
                    dato = dato,
                    utbetalinger = dager.flatMap { it.utbetalinger }
                )
            }
        return Feriepengegrunnlagstidslinje(dager = resultat)
    }

    fun utbetalteDager(): List<Feriepengeutbetalinggrunnlag.UtbetaltDag> {
        return dager.flatMap {
            it.utbetalinger.map { utbetaltDag ->
                when (utbetaltDag.mottaker) {
                    Feriepengegrunnlagsdag.Mottaker.ARBEIDSGIVER -> when (utbetaltDag.kilde) {
                        Kilde.INFOTRYGD -> Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdArbeidsgiver(utbetaltDag.orgnummer, it.dato, utbetaltDag.beløp)
                        Kilde.SPLEIS -> Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisArbeidsgiver(utbetaltDag.orgnummer, it.dato, utbetaltDag.beløp)
                    }
                    Feriepengegrunnlagsdag.Mottaker.PERSON -> when (utbetaltDag.kilde) {
                        Kilde.INFOTRYGD -> Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdPerson(utbetaltDag.orgnummer, it.dato, utbetaltDag.beløp)
                        Kilde.SPLEIS -> Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisPerson(utbetaltDag.orgnummer, it.dato, utbetaltDag.beløp)
                    }
                }
            }
        }
    }

    data class DagerForArbeidsgiver(
        val prosentsats: Double, val dager: List<Feriepengegrunnlagsdag>
    ) {
        val refusjonsresultat = dager.flatMap { it.utbetalinger }.filter(ARBEIDSGIVER).grunnlag(prosentsats)
        val personresultat = dager.flatMap { it.utbetalinger }.filter(PERSON).grunnlag(prosentsats)

        data class BeregnetResultat(
            val feriepengegrunnlag: Int,
            val utbetalingsgrunnlag: Double
        ) {
            val utbetaling = utbetalingsgrunnlag.roundToInt()

            operator fun minus(other: BeregnetResultat): Int {
                return (this.utbetalingsgrunnlag - other.utbetalingsgrunnlag).roundToInt()
            }

            companion object {
                fun List<Feriepengegrunnlagsdag.UtbetaltDag>.grunnlag(prosentsats: Double): BeregnetResultat {
                    val grunnlag = this.sumOf { it.beløp }
                    return BeregnetResultat(
                        feriepengegrunnlag = grunnlag,
                        utbetalingsgrunnlag = grunnlag * prosentsats
                    )
                }
            }
        }
    }

    class Builder {
        private val dager = mutableMapOf<LocalDate, Feriepengegrunnlagsdag>()

        fun leggTilUtbetaling(dag: LocalDate, orgnummer: String, mottaker: Feriepengegrunnlagsdag.Mottaker, kilde: Kilde, beløp: Int) {
            val nyDag = Feriepengegrunnlagsdag.UtbetaltDag(orgnummer = orgnummer, mottaker = mottaker, kilde = kilde, beløp = beløp)
            dager.compute(dag) { _, eksisterende ->
                eksisterende?.copy(utbetalinger = eksisterende.utbetalinger + nyDag) ?: Feriepengegrunnlagsdag(dato = dag, utbetalinger = listOf(nyDag))
            }
        }
        fun build() = Feriepengegrunnlagstidslinje(dager.values)
    }

    private companion object {
        private const val ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET = 48
    }
}
