package no.nav.helse.serde.reflection

import no.nav.helse.serde.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*

internal class UtbetalingReflect(private val utbetaling: Utbetaling) {
    internal fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "utbetalingstidslinje" to
            UtbetalingstidslinjeReflect(utbetaling["utbetalingstidslinje"]).toMap(),
        "arbeidsgiverUtbetalingslinjer" to
            UtbetalingslinjerReflect(utbetaling["arbeidsgiverUtbetalingslinjer"]).toMap(),
        "personUtbetalingslinjer" to
            UtbetalingslinjerReflect(utbetaling["personUtbetalingslinjer"]).toMap(),
        "tidsstempel" to utbetaling["tidsstempel"]
    )
}

private class UtbetalingslinjerReflect(private val utbetalingslinjer: Utbetalingslinjer) {
    internal fun toMap() = mutableMapOf(
        "mottaker" to utbetalingslinjer["mottaker"],
        "mottakertype" to utbetalingslinjer.get<Utbetalingslinjer, Mottakertype>("mottakertype").melding,
        "linjer" to utbetalingslinjer.map { UtbetalingslinjeReflect(it).toMap() },
        "utbetalingsreferanse" to utbetalingslinjer["utbetalingsreferanse"],
        "linjertype" to utbetalingslinjer.get<Utbetalingslinjer, Linjetype>("linjertype").toString(),
        "sjekksum" to utbetalingslinjer["sjekksum"]
    )
}

internal class UtbetalingslinjeReflect(private val utbetalingslinje: Utbetalingslinje) {
    internal fun toMap() = mutableMapOf<String, Any?>(
        "fom" to utbetalingslinje["fom"],
        "tom" to utbetalingslinje["tom"],
        "dagsats" to utbetalingslinje["dagsats"],
        "grad" to utbetalingslinje["grad"],
        "delytelseId" to utbetalingslinje["delytelseId"],
        "refDelytelseId" to utbetalingslinje["refDelytelseId"],
        "linjetype" to utbetalingslinje.get<Utbetalingslinje, Linjetype>("linjetype").toString()
        )
}

private class UtbetalingstidslinjeReflect(private val utbetalingstidslinje: Utbetalingstidslinje) {
    internal fun toMap() = mutableMapOf<String, Any?>(
        "utbetalingsdager" to utbetalingstidslinje.map {
            when(it::class) {
                Arbeidsdag::class -> UtbetalingsdagReflect(it, TypeData.Arbeidsdag).toMap()
                ArbeidsgiverperiodeDag::class -> UtbetalingsdagReflect(it, TypeData.ArbeidsgiverperiodeDag).toMap()
                NavDag::class -> NavDagReflect(it, TypeData.NavDag).toMap()
                NavHelgDag::class -> UtbetalingsdagMedGradReflect(it, TypeData.NavHelgDag).toMap()
                Fridag::class -> UtbetalingsdagReflect(it, TypeData.Fridag).toMap()
                UkjentDag::class -> UtbetalingsdagReflect(it, TypeData.UkjentDag).toMap()
                AvvistDag::class -> AvvistdagReflect(it as AvvistDag).toMap()
                ForeldetDag::class -> UtbetalingsdagReflect(it, TypeData.ForeldetDag).toMap()
                else -> throw IllegalStateException("Uventet utbetalingsdag")
            }
        }
    )
}
