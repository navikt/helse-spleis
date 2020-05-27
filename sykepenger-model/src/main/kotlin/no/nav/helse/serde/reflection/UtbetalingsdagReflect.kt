package no.nav.helse.serde.reflection

import no.nav.helse.serde.UtbetalingstidslinjeData.TypeData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingsdagReflect(utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag, private val type: TypeData) {
    private val dagsats: Int = utbetalingsdag["dagsats"]
    private val dato: LocalDate = utbetalingsdag["dato"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "dagsats" to dagsats,
        "dato" to dato
    )
}

internal class UtbetalingsdagMedGradReflect(
    utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag,
    private val type: TypeData
) {
    private val dagsats: Int = utbetalingsdag["dagsats"]
    private val dato: LocalDate = utbetalingsdag["dato"]
    private val grad: Double = utbetalingsdag["grad"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "dagsats" to dagsats,
        "dato" to dato,
        "grad" to grad
    )
}


internal class NavDagReflect(utbetalingsdag: Utbetalingstidslinje.Utbetalingsdag, private val type: TypeData) {
    private val dagsats: Int = utbetalingsdag["dagsats"]
    private val dato: LocalDate = utbetalingsdag["dato"]
    private val økonomi: Økonomi = utbetalingsdag["økonomi"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "dagsats" to dagsats,
        "dato" to dato
    ).also { it.putAll(økonomi.toMap()) }
}

internal class AvvistdagReflect(avvistdag: Utbetalingstidslinje.Utbetalingsdag.AvvistDag) {
    private val dagsats: Int = avvistdag["dagsats"]
    private val dato: LocalDate = avvistdag["dato"]
    private val begrunnelse: Begrunnelse = avvistdag["begrunnelse"]
    private val grad: Double = avvistdag["grad"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to TypeData.AvvistDag,
        "dagsats" to dagsats,
        "dato" to dato,
        "begrunnelse" to begrunnelse.name,
        "grad" to grad
    )
}
