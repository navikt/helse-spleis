package no.nav.helse.utbetalingslinjer

import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.FeriepengeutbetalingVisitor
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner

internal class Feriepengeutbetaling(
    private val orgnummer: String,
    private val feriepengeberegner: Feriepengeberegner,
    private val utbetalingshistorikkForFeriepenger: UtbetalingshistorikkForFeriepenger
) {
    private val observers = mutableListOf<UtbetalingObserver>()

    internal fun registrer(observer: UtbetalingObserver) {
        observers.add(observer)
    }

    internal fun accept(visitor: FeriepengeutbetalingVisitor) {
        visitor.preVisitFeriepengeutbetaling(
            this,
            requireNotNull(infotrygdFeriepengebeløpPerson),
            requireNotNull(infotrygdFeriepengebeløpArbeidsgiver),
            requireNotNull(spleisFeriepengebeløpArbeidsgiver)
        )
        feriepengeberegner.accept(visitor)
        visitor.postVisitFeriepengeutbetaling(
            this,
            requireNotNull(infotrygdFeriepengebeløpPerson),
            requireNotNull(infotrygdFeriepengebeløpArbeidsgiver),
            requireNotNull(spleisFeriepengebeløpArbeidsgiver)
        )
    }

    private var infotrygdFeriepengebeløpPerson: Double? = null
    private var infotrygdFeriepengebeløpArbeidsgiver: Double? = null
    private var spleisFeriepengebeløpArbeidsgiver: Double? = null

    internal fun beregn() {
        val infotrygdHarUtbetaltTilArbeidsgiver = utbetalingshistorikkForFeriepenger.utbetalteFeriepengerTilArbeidsgiver(orgnummer)
        val hvaViHarBeregnetAtInfotrygdHarUtbetaltTilArbeidsgiver = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer)

        infotrygdFeriepengebeløpPerson = feriepengeberegner.beregnFeriepengerForInfotrygdPerson(orgnummer)
        infotrygdFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(orgnummer)
        spleisFeriepengebeløpArbeidsgiver = feriepengeberegner.beregnFeriepengerForSpleis(orgnummer)

        val kaEFasitenSomSkaUtbetalesTeArbeidsgiver: Double = feriepengeberegner.beregnFeriepengerForArbeidsgiver(orgnummer)
        val kaEFaktiskUtbetaltTeArbeidsgiver: Double = feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(orgnummer)
        val kaEKorrigeringa: Double = feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(orgnummer)
    }
}
