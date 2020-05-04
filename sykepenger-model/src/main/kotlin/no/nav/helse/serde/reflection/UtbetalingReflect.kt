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
        "arbeidsgiverOppdrag" to
            OppdragReflect(utbetaling["arbeidsgiverOppdrag"]).toMap(),
        "personOppdrag" to
            OppdragReflect(utbetaling["personOppdrag"]).toMap(),
        "tidsstempel" to utbetaling["tidsstempel"]
    )
}

internal class OppdragReflect(private val oppdrag: Oppdrag) {
    internal fun toMap() = mutableMapOf(
        "mottaker" to oppdrag["mottaker"],
        "fagområde" to oppdrag.get<Fagområde>("fagområde").toString(),
        "linjer" to oppdrag.map { UtbetalingslinjeReflect(it).toMap() },
        "fagsystemId" to oppdrag["fagsystemId"],
        "endringskode" to oppdrag.get<Endringskode>("endringskode").toString(),
        "sisteArbeidsgiverdag" to oppdrag["sisteArbeidsgiverdag"],
        "sjekksum" to oppdrag["sjekksum"]
    )
}

internal class UtbetalingslinjeReflect(private val utbetalingslinje: Utbetalingslinje) {
    internal fun toMap() = mutableMapOf<String, Any?>(
        "fom" to utbetalingslinje.get<LocalDate>("fom").toString(),
        "tom" to utbetalingslinje.get<LocalDate>("tom").toString(),
        "dagsats" to utbetalingslinje["dagsats"],
        "lønn" to utbetalingslinje["lønn"],
        "grad" to utbetalingslinje["grad"],
        "refFagsystemId" to utbetalingslinje["refFagsystemId"],
        "delytelseId" to utbetalingslinje["delytelseId"],
        "datoStatusFom" to utbetalingslinje.get<LocalDate?>("datoStatusFom"),
        "statuskode" to utbetalingslinje.get<LocalDate?>("datoStatusFom")?.let { "OPPH" },
        "refDelytelseId" to utbetalingslinje["refDelytelseId"],
        "endringskode" to utbetalingslinje.get<Endringskode>("endringskode").toString(),
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
