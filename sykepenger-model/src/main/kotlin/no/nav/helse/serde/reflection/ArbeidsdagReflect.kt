package no.nav.helse.serde.reflection

import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import java.time.LocalDate

internal class ArbeidsdagReflect(arbeidsdag: Utbetalingstidslinje.Utbetalingsdag.Arbeidsdag) {
    private val inntekt: Double = arbeidsdag.getProp("inntekt")
    private val dato: LocalDate = arbeidsdag.getProp("dato")

    internal fun toMap(): Map<String, Any?> = mapOf(
        "inntekt" to inntekt,
        "dato" to dato
    )
}
