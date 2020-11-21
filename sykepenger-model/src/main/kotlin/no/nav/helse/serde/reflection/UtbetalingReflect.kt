package no.nav.helse.serde.reflection

import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate

internal class UtbetalingReflect(private val utbetaling: Utbetaling) {
    internal fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "id" to utbetaling["id"],
        "utbetalingstidslinje" to UtbetalingstidslinjeReflect(utbetaling["utbetalingstidslinje"]).toMap(),
        "arbeidsgiverOppdrag" to OppdragReflect(utbetaling["arbeidsgiverOppdrag"]).toMap(),
        "personOppdrag" to OppdragReflect(utbetaling["personOppdrag"]).toMap(),
        "tidsstempel" to utbetaling["tidsstempel"],
        "status" to utbetaling.get<Utbetaling.Status>("status").name,
        "annullert" to utbetaling.get<Boolean>("annullert")
    )
}

internal class OppdragReflect(private val oppdrag: Oppdrag) {
    internal fun toBehovMap() = mutableMapOf(
        "mottaker" to oppdrag["mottaker"],
        "fagområde" to oppdrag.get<Fagområde>("fagområde").verdi,
        "linjer" to oppdrag.map { UtbetalingslinjeReflect(it).toMap() },
        "fagsystemId" to oppdrag["fagsystemId"],
        "endringskode" to oppdrag.get<Endringskode>("endringskode").toString()
    )


    internal fun toMap() = mutableMapOf(
        "mottaker" to oppdrag["mottaker"],
        "fagområde" to oppdrag.get<Fagområde>("fagområde").verdi,
        "linjer" to oppdrag.map { UtbetalingslinjeReflect(it).toMap() },
        "fagsystemId" to oppdrag["fagsystemId"],
        "endringskode" to oppdrag.get<Endringskode>("endringskode").toString(),
        "sisteArbeidsgiverdag" to oppdrag["sisteArbeidsgiverdag"],
        "tidsstempel" to oppdrag["tidsstempel"],
        "nettoBeløp" to oppdrag["nettoBeløp"]
    )
}

internal class UtbetalingslinjeReflect(private val utbetalingslinje: Utbetalingslinje) {
    internal fun toMap() = mutableMapOf<String, Any?>(
        "fom" to utbetalingslinje.get<LocalDate>("fom").toString(),
        "tom" to utbetalingslinje.get<LocalDate>("tom").toString(),
        "dagsats" to utbetalingslinje["beløp"],           // TODO: change "dagsats" to "beløp",
                                                          //    but needs JSON migration and change in Need apps
        "lønn" to utbetalingslinje["aktuellDagsinntekt"], // TODO: change "lønn" to "aktuellDagsinntekt",
                                                          //    but needs JSON migration and change in Need apps
        "grad" to utbetalingslinje["grad"],
        "refFagsystemId" to utbetalingslinje["refFagsystemId"],
        "delytelseId" to utbetalingslinje["delytelseId"],
        "datoStatusFom" to utbetalingslinje.get<LocalDate?>("datoStatusFom")?.toString(),
        "statuskode" to utbetalingslinje.get<LocalDate?>("datoStatusFom")?.let { "OPPH" },
        "refDelytelseId" to utbetalingslinje["refDelytelseId"],
        "endringskode" to utbetalingslinje.get<Endringskode>("endringskode").toString(),
        "klassekode" to utbetalingslinje.get<Klassekode>("klassekode").verdi
    )
}

internal class UtbetalingstidslinjeReflect(private val utbetalingstidslinje: Utbetalingstidslinje) {
    fun toMap() = mutableMapOf<String, Any?>(
        "dager" to utbetalingstidslinje.map {
            when(it::class) {
                Arbeidsdag::class -> UtbetalingsdagReflect(it, TypeData.Arbeidsdag).toMap()
                ArbeidsgiverperiodeDag::class -> UtbetalingsdagReflect(it, TypeData.ArbeidsgiverperiodeDag).toMap()
                NavDag::class -> UtbetalingsdagReflect(it, TypeData.NavDag).toMap()
                NavHelgDag::class -> UtbetalingsdagReflect(it, TypeData.NavHelgDag).toMap()
                Fridag::class -> UtbetalingsdagReflect(it, TypeData.Fridag).toMap()
                UkjentDag::class -> UtbetalingsdagReflect(it, TypeData.UkjentDag).toMap()
                AvvistDag::class -> AvvistdagReflect(it as AvvistDag).toMap()
                ForeldetDag::class -> UtbetalingsdagReflect(it, TypeData.ForeldetDag).toMap()
                else -> throw IllegalStateException("Uventet utbetalingsdag")
            }
        }
    )
}
