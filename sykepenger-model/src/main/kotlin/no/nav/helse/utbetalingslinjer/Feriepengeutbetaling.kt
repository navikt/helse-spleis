package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.serde.reflection.OppdragReflect
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import org.slf4j.LoggerFactory
import java.time.Month
import kotlin.math.roundToInt

internal class Feriepengeutbetaling private constructor(
    private val feriepengeberegner: Feriepengeberegner,
    private val infotrygdFeriepengebeløpPerson: Double,
    private val infotrygdFeriepengebeløpArbeidsgiver: Double,
    private val spleisFeriepengebeløpArbeidsgiver: Double,
    private val oppdrag: Oppdrag
) {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    private val observers = mutableListOf<UtbetalingObserver>()

    internal fun registrer(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun accept(visitor: FeriepengeutbetalingVisitor) {
        visitor.preVisitFeriepengeutbetaling(
            this,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver
        )
        feriepengeberegner.accept(visitor)
        oppdrag.accept(visitor)
        visitor.postVisitFeriepengeutbetaling(
            this,
            infotrygdFeriepengebeløpPerson,
            infotrygdFeriepengebeløpArbeidsgiver,
            spleisFeriepengebeløpArbeidsgiver
        )
    }

    internal class Builder(
        private val aktørId: String,
        private val orgnummer: String,
        private val feriepengeberegner: Feriepengeberegner,
        private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
    ) {
        internal fun build(): Feriepengeutbetaling {
            val infotrygdHarUtbetaltTilArbeidsgiver = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilArbeidsgiver(orgnummer)
            val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer)

            if (hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt() != 0 &&
                hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt() !in infotrygdHarUtbetaltTilArbeidsgiver) {
                sikkerLogg.info(
                    """
                    Beregnet feriepengebeløp til arbeidsgiver i IT samsvarer ikke med faktisk utbetalt beløp
                    AktørId: $aktørId
                    Arbeidsgiver: $orgnummer
                    Infotrygd har utbetalt $infotrygdHarUtbetaltTilArbeidsgiver
                    Vi har beregnet at infotrygd har utbetalt ${hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver.roundToInt()}
                    """.trimIndent()
                )
            }

            val infotrygdFeriepengebeløpPerson = feriepengeberegner.beregnFeriepengerForInfotrygdPerson(orgnummer)
            val infotrygdFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer)
            val spleisFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForSpleis(orgnummer)

            val infotrygdFeriepengebeløpPersonUtenPersonhack = feriepengeberegner.beregnFeriepengerForInfotrygdPersonUtenPersonhack(orgnummer)

            if (infotrygdFeriepengebeløpPerson != infotrygdFeriepengebeløpPersonUtenPersonhack) {
                sikkerLogg.info(
                    """
                    Feriepengebeløp utbetalt til person er forskjellig fra beløpet som skulle vært utbetalt til person etter ordinære regler
                    AktørId: $aktørId
                    Arbeidsgiver: $orgnummer
                    Faktisk utbetalt til person: $infotrygdFeriepengebeløpPerson
                    Burde egentlig vært utbetalt: $infotrygdFeriepengebeløpPersonUtenPersonhack
                    """.trimIndent()
                )
            }

            val totaltFeriepengebeløpArbeidsgiver: Double = feriepengeberegner.beregnFeriepengerForArbeidsgiver(orgnummer)
            val differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd: Double = feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(orgnummer)

            val oppdrag = Oppdrag(
                mottaker = orgnummer,
                fagområde = Fagområde.SykepengerRefusjon,
                linjer = listOf(
                    Utbetalingslinje(
                        fom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atDay(1),
                        tom = utbetalingshistorikkForFeriepenger.opptjeningsår.plusYears(1).atMonth(Month.MAY).atEndOfMonth(),
                        satstype = Satstype.ENG,
                        beløp = differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd.roundToInt(),
                        aktuellDagsinntekt = null,
                        grad = null,
                        klassekode = Klassekode.RefusjonFeriepengerIkkeOpplysningspliktig
                    )
                ),
                sisteArbeidsgiverdag = null
            )

            sikkerLogg.info(
                """
                Nøkkelverdier om feriepengeberegning
                AktørId: $aktørId
                Arbeidsgiver: $orgnummer
                IT har utbetalt til arbeidsgiver: $infotrygdHarUtbetaltTilArbeidsgiver
                Hva vi har beregnet at IT har utbetalt til arbeidsgiver: $hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver
                IT sin personandel: $infotrygdFeriepengebeløpPerson
                IT sin arbeidsgiverandel: $infotrygdFeriepengebeløpArbeidsgiver
                Spleis sin arbeidsgiverandel: $spleisFeriepengebeløpArbeidsgiver
                Totalt feriepengebeløp: $totaltFeriepengebeløpArbeidsgiver
                Differanse: $differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd
                Oppdrag: ${OppdragReflect(oppdrag).toMap()}
                Datoer: ${feriepengeberegner.feriepengedatoer()}
                """.trimIndent()
            )

            return Feriepengeutbetaling(
                feriepengeberegner = feriepengeberegner,
                infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
                infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
                spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
                oppdrag = oppdrag
            )
        }
    }
}
