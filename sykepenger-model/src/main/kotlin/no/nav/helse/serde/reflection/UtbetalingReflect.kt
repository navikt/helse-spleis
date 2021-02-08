package no.nav.helse.serde.reflection

import no.nav.helse.serde.PersonData.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.serde.reflection.ReflectInstance.Companion.maybe
import no.nav.helse.utbetalingslinjer.*
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.*
import java.time.LocalDate
import java.time.LocalDateTime

enum class Utbetalingstatus(private val tilstand: Utbetaling.Tilstand) {
    IKKE_UTBETALT(Utbetaling.Ubetalt),
    IKKE_GODKJENT(Utbetaling.IkkeGodkjent),
    GODKJENT(Utbetaling.Godkjent),
    SENDT(Utbetaling.Sendt),
    OVERFØRT(Utbetaling.Overført),
    UTBETALT(Utbetaling.Utbetalt),
    GODKJENT_UTEN_UTBETALING(Utbetaling.GodkjentUtenUtbetaling),
    UTBETALING_FEILET(Utbetaling.UtbetalingFeilet),
    ANNULLERT(Utbetaling.Annullert),
    FORKASTET(Utbetaling.Forkastet);

    internal fun tilTilstand() = tilstand

    internal companion object {
        fun fraTilstand(tilstand: Utbetaling.Tilstand) =
            values().first { it.tilstand == tilstand }
    }
}

internal class UtbetalingReflect(private val utbetaling: Utbetaling) {
    private val arbeidsgiverOppdrag: Oppdrag = utbetaling["arbeidsgiverOppdrag"]
    private val personOppdrag: Oppdrag = utbetaling["personOppdrag"]

    internal fun toMap(): MutableMap<String, Any?> = mutableMapOf(
        "id" to utbetaling["id"],
        "beregningId" to utbetaling["beregningId"],
        "utbetalingstidslinje" to UtbetalingstidslinjeReflect(utbetaling["utbetalingstidslinje"]).toMap(),
        "arbeidsgiverOppdrag" to OppdragReflect(arbeidsgiverOppdrag).toMap(),
        "personOppdrag" to OppdragReflect(personOppdrag).toMap(),
        "fom" to utbetaling.periode.start,
        "tom" to utbetaling.periode.endInclusive,
        "stønadsdager" to (arbeidsgiverOppdrag.stønadsdager() + personOppdrag.stønadsdager()), //TA MED
        "tidsstempel" to utbetaling["tidsstempel"],
        "status" to Utbetalingstatus.fraTilstand(utbetaling.get<Utbetaling.Tilstand>("tilstand")), //TA MED
        "type" to utbetaling.get<Utbetaling.Utbetalingtype>("type"), //TA MED
        "maksdato" to utbetaling.maybe<LocalDate>("maksdato"), //TA MED
        "forbrukteSykedager" to utbetaling.maybe<Int>("forbrukteSykedager"), //TA MED
        "gjenståendeSykedager" to utbetaling.maybe<Int>("gjenståendeSykedager"), //TA MED
        "vurdering" to utbetaling.maybe<Utbetaling.Vurdering>("vurdering")?.let { VurderingReflect(it).toMap() }, //TA MED
        "overføringstidspunkt" to utbetaling.maybe<LocalDateTime>("overføringstidspunkt"),
        "avstemmingsnøkkel" to utbetaling.maybe<Long>("avstemmingsnøkkel"),
        "avsluttet" to utbetaling.maybe<LocalDateTime>("avsluttet"),
        "oppdatert" to utbetaling.maybe<LocalDateTime>("oppdatert")
    )

    fun toSpeilMap() = mapOf(
        "utbetalingstidslinje" to UtbetalingstidslinjeReflect(utbetaling["utbetalingstidslinje"]).toMap(),
        "arbeidsgiverOppdrag" to OppdragReflect(arbeidsgiverOppdrag).toMap(),
    )
}

private class VurderingReflect(private val vurdering: Utbetaling.Vurdering) {
    internal fun toMap() = mapOf(
        "godkjent" to vurdering.get<Boolean>("godkjent"),
        "ident" to vurdering.get<String>("ident"),
        "epost" to vurdering.get<String>("epost"),
        "tidspunkt" to vurdering.get<LocalDateTime>("tidspunkt"),
        "automatiskBehandling" to vurdering.get<Boolean>("automatiskBehandling")
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
        "nettoBeløp" to oppdrag["nettoBeløp"],
        "stønadsdager" to oppdrag.stønadsdager(),
        "fom" to oppdrag.førstedato,
        "tom" to oppdrag.sistedato
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
        "stønadsdager" to utbetalingslinje.stønadsdager(),
        "totalbeløp" to utbetalingslinje.totalbeløp(),
        "endringskode" to utbetalingslinje.get<Endringskode>("endringskode").toString(),
        "delytelseId" to utbetalingslinje["delytelseId"],
        "refDelytelseId" to utbetalingslinje["refDelytelseId"],
        "refFagsystemId" to utbetalingslinje["refFagsystemId"],
        "statuskode" to utbetalingslinje.get<LocalDate?>("datoStatusFom")?.let { "OPPH" },
        "datoStatusFom" to utbetalingslinje.get<LocalDate?>("datoStatusFom")?.toString(),
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
