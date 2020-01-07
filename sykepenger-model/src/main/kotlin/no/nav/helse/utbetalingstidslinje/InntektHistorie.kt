package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class InntektHistorie {
    private val inntekter = mutableMapOf<LocalDate, Double>()

    fun add(dagen: LocalDate, inntekt: Double) {
        inntekter[dagen] = inntekt
    }
    fun inntekt(dagen: LocalDate) = inntekter.entries.sortedBy { it.key }.last { dagen.isAfter(it.key) }.value
}
