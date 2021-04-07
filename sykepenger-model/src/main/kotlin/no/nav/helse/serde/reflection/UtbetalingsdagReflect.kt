package no.nav.helse.serde.reflection

import no.nav.helse.serde.PersonData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class UtbetalingsdagReflect(utbetalingsdag: Utbetalingsdag, private val type: PersonData.UtbetalingstidslinjeData.TypeData) {
    private val dato: LocalDate = utbetalingsdag["dato"]
    private val økonomi: Økonomi = utbetalingsdag["økonomi"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type,
        "dato" to dato
    ).apply {
        putAll(serialiserØkonomi(økonomi))
    }
}

internal class AvvistdagReflect(avvistdag: AvvistDag) {
    private val dato: LocalDate = avvistdag["dato"]
    private val økonomi: Økonomi = avvistdag["økonomi"]
    private val begrunnelse: Begrunnelse = avvistdag["begrunnelse"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag,
        "dato" to dato,
        "begrunnelse" to PersonData.UtbetalingstidslinjeData.BegrunnelseData.fraBegrunnelse(begrunnelse).name
    ).apply {
        putAll(serialiserØkonomi(økonomi))
    }
}
