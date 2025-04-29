package no.nav.helse.feriepenger

import java.time.Month.DECEMBER
import java.time.Year
import no.nav.helse.Alder
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

    val feriepengedager = utbetalteDager.feriepengedager()

    internal companion object {
        private const val ALDER_FOR_FORHØYET_FERIEPENGESATS = 59
        private const val ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET = 48

        internal fun List<UtbetaltDag>.tilDato() = map { it.dato }.distinct()
        internal fun List<UtbetaltDag>.feriepengedager(): List<UtbetaltDag> {
            //TODO: subsumsjonObserver.`§8-33 ledd 1`() //TODO: Finne ut hvordan vi løser denne mtp. input Infotrygd/Spleis og pr. arbeidsgiver
            return this
                .sortedBy { it.dato }
                .groupBy { it.dato }
                .entries
                .take(ANTALL_FERIEPENGEDAGER_I_OPPTJENINGSÅRET)
                .flatMap { (_, v) -> v }
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
        feriepengedager = feriepengedager
    )

    internal fun feriepengedatoer() = feriepengedager.tilDato()
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

    private fun beregnFeriepenger(beløp: Int): Double {
        val feriepenger = beløp * prosentsats
        //TODO: subsumsjonObserver.`§8-33 ledd 3`(beløp, opptjeningsår, prosentsats, alderVedSluttenAvÅret, feriepenger)
        return feriepenger
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygdTilArbeidsgiver(orgnummer)
        return beregnFeriepenger(grunnlag)
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdPerson(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygdTilPerson(orgnummer)
        return beregnFeriepenger(grunnlag)
    }

    internal fun beregnUtbetalteFeriepengerForInfotrygdPersonForEnArbeidsgiver(orgnummer: String): Double {
        val grunnlag = grunnlagForFeriepengerUtbetaltAvInfotrygdTilPersonForArbeidsgiver(orgnummer)
        return beregnFeriepenger(grunnlag)
    }

    internal fun beregnFeriepengedifferansenForArbeidsgiver(orgnummer: String): Double {
        val grunnlag = feriepengedager.filter(ARBEIDSGIVER and orgnummerFilter(orgnummer)).summer()
        val grunnlagUtbetaltAvInfotrygd = grunnlagForFeriepengerUtbetaltAvInfotrygdTilArbeidsgiver(orgnummer)
        return beregnFeriepenger(grunnlag - grunnlagUtbetaltAvInfotrygd)
    }

    internal fun beregnFeriepengedifferansenForPerson(orgnummer: String): Double {
        val grunnlag = feriepengedager.filter(PERSON and orgnummerFilter(orgnummer)).summer()
        val grunnlagUtbetaltAvInfotrygd = grunnlagForFeriepengerUtbetaltAvInfotrygdTilPerson(orgnummer)
        return beregnFeriepenger(grunnlag - grunnlagUtbetaltAvInfotrygd)
    }

    private fun første48dageneUtbetaltIInfotrygd() = utbetalteDager.filter(INFOTRYGD).feriepengedager()

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygdTilArbeidsgiver(orgnummer: String) =
        første48dageneUtbetaltIInfotrygd().filter(INFOTRYGD_ARBEIDSGIVER and orgnummerFilter(orgnummer)).summer()

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygdTilPerson(orgnummer: String) =
        første48dageneUtbetaltIInfotrygd().filter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer)).summer()

    private fun grunnlagForFeriepengerUtbetaltAvInfotrygdTilPersonForArbeidsgiver(orgnummer: String): Int {
        val itFeriepengedager = utbetalteDager.filter(INFOTRYGD).feriepengedager()
        return itFeriepengedager.filter(INFOTRYGD_PERSON and orgnummerFilter(orgnummer)).summer()
    }

    private fun beregnForFilter(filter: UtbetaltDagSelector): Double {
        val grunnlag = feriepengedager.filter(filter).summer()
        return beregnFeriepenger(grunnlag)
    }
}
