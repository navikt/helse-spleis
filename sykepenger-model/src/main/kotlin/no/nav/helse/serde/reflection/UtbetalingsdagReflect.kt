package no.nav.helse.serde.reflection

import no.nav.helse.serde.PersonData
import no.nav.helse.serde.reflection.ReflectInstance.Companion.get
import no.nav.helse.utbetalingstidslinje.Begrunnelse
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.økonomi.Økonomi

internal class UtbetalingsdagReflect(utbetalingsdag: Utbetalingsdag, private val type: PersonData.UtbetalingstidslinjeData.TypeData) {
    private val økonomi: Økonomi = utbetalingsdag["økonomi"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to type
    ).apply {
        putAll(serialiserØkonomi(økonomi))
    }
}

internal class AvvistdagReflect(avvistdag: AvvistDag) {
    private val økonomi: Økonomi = avvistdag["økonomi"]
    private val begrunnelser: List<Begrunnelse> = avvistdag["begrunnelser"]

    internal fun toMap() = mutableMapOf<String, Any?>(
        "type" to PersonData.UtbetalingstidslinjeData.TypeData.AvvistDag,
        "begrunnelser" to begrunnelser.map { PersonData.UtbetalingstidslinjeData.BegrunnelseData.fraBegrunnelse(it).name }
    ).apply {
        putAll(serialiserØkonomi(økonomi))
    }
}
