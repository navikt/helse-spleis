package no.nav.helse.serde.reflection

import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal class UtbetalingsdagReflect(utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag, private val type: String) {
    private val inntekt: Double = utbetalingsdag["inntekt"]
    private val dato: LocalDate = utbetalingsdag["dato"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "inntekt" to inntekt,
        "dato" to dato
    )
}

internal class AvvistdagReflect(avvistdag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
    private val inntekt: Double = avvistdag["inntekt"]
    private val dato: LocalDate = avvistdag["dato"]
    private val begrunnelse: Begrunnelse = avvistdag["begrunnelse"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to "AvvistDag",
        "inntekt" to inntekt,
        "dato" to dato,
        "begrunnelse" to begrunnelse.name
    )
}
