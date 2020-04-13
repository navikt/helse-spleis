package no.nav.helse.serde.reflection

import no.nav.helse.serde.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate

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

internal class OppdragReflect(
    private val utbetaling: Utbetaling,
    private val maksdato: LocalDate,
    private val saksbehandler: String
) {
    internal fun toMap(): MutableMap<String, Any> = mutableMapOf(
        "utbetalingslinjer" to
            UtbetalingslinjerReflect(utbetaling
                .get<Utbetalingslinjer>("arbeidsgiverUtbetalingslinjer").removeUEND())
                .toMap(),
        "maksdato" to maksdato.toString(),
        "saksbehandler" to saksbehandler
    )
}

internal class UtbetalingslinjerReflect(private val utbetalingslinjer: Utbetalingslinjer) {
    internal fun toMap() = mutableMapOf(
        "mottaker" to utbetalingslinjer["mottaker"],
        "fagområde" to utbetalingslinjer.get<Fagområde>("fagområde").toString(),
        "linjer" to utbetalingslinjer.map { UtbetalingslinjeReflect(it).toMap() },
        "utbetalingsreferanse" to utbetalingslinjer["utbetalingsreferanse"],
        "linjertype" to utbetalingslinjer.get<Linjetype>("linjertype").toString(),
        "sjekksum" to utbetalingslinjer["sjekksum"]
    )
}

internal class UtbetalingslinjeReflect(private val utbetalingslinje: Utbetalingslinje) {
    internal fun toMap() = mutableMapOf<String, Any?>(
        "fom" to utbetalingslinje.get<LocalDate>("fom").toString(),
        "tom" to utbetalingslinje.get<LocalDate>("tom").toString(),
        "dagsats" to utbetalingslinje["dagsats"],
        "grad" to utbetalingslinje["grad"],
        "delytelseId" to utbetalingslinje["delytelseId"],
        "refDelytelseId" to utbetalingslinje["refDelytelseId"],
        "linjetype" to utbetalingslinje.get<Linjetype>("linjetype").toString(),
        "klassekode" to utbetalingslinje.get<Klassekode>("klassekode").verdi
        )
}

private class UtbetalingstidslinjeReflect(private val utbetalingstidslinje: Utbetalingstidslinje) {
    internal fun toMap() = mutableMapOf<String, Any?>(
        "dager" to utbetalingstidslinje.map {
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
