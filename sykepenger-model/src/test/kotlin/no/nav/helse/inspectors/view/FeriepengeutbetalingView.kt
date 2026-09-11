package no.nav.helse.inspectors.view

import no.nav.helse.feriepenger.Feriepengeoppdrag
import no.nav.helse.feriepenger.Feriepengeutbetaling

internal data class FeriepengeutbetalingView(
    val infotrygdFeriepengebeløpPerson: Double,
    val infotrygdFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpArbeidsgiver: Double,
    val spleisFeriepengebeløpPerson: Double,
    val oppdrag: Feriepengeoppdrag,
    val personoppdrag: Feriepengeoppdrag
)

internal fun Feriepengeutbetaling.view() = FeriepengeutbetalingView(
    infotrygdFeriepengebeløpPerson = infotrygdFeriepengebeløpPerson,
    infotrygdFeriepengebeløpArbeidsgiver = infotrygdFeriepengebeløpArbeidsgiver,
    spleisFeriepengebeløpArbeidsgiver = spleisFeriepengebeløpArbeidsgiver,
    spleisFeriepengebeløpPerson = spleisFeriepengebeløpPerson,
    oppdrag = oppdrag,
    personoppdrag = personoppdrag
)
