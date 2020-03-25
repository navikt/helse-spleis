package no.nav.helse.serde.reflection

import no.nav.helse.serde.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal class UtbetalingsdagReflect(utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag, private val type: TypeData) {
    private val inntekt: Double = utbetalingsdag["inntekt"]
    private val dato: LocalDate = utbetalingsdag["dato"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "inntekt" to inntekt,
        "dato" to dato
    )
}

internal class UtbetalingsdagMedGradReflect(
    utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag,
    private val type: TypeData
) {
    private val inntekt: Double = utbetalingsdag["inntekt"]
    private val dato: LocalDate = utbetalingsdag["dato"]
    private val grad: Double = utbetalingsdag["grad"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "inntekt" to inntekt,
        "dato" to dato,
        "grad" to grad
    )
}


internal class NavDagReflect(utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag, private val type: TypeData) {
    private val inntekt: Double = utbetalingsdag["inntekt"]
    private val dato: LocalDate = utbetalingsdag["dato"]
    private val utbetaling: Int = utbetalingsdag["utbetaling"]
    private val grad: Double = utbetalingsdag["grad"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "inntekt" to inntekt,
        "dato" to dato,
        "utbetaling" to utbetaling,
        "grad" to grad
    )
}

internal class AvvistdagReflect(avvistdag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
    private val inntekt: Double = avvistdag["inntekt"]
    private val dato: LocalDate = avvistdag["dato"]
    private val begrunnelse: Begrunnelse = avvistdag["begrunnelse"]
    private val grad: Double = avvistdag["grad"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to TypeData.AvvistDag,
        "inntekt" to inntekt,
        "dato" to dato,
        "begrunnelse" to begrunnelse.name,
        "grad" to grad
    )
}
