package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate

internal class Inntektsberegner {

    private val inntekter = mutableMapOf<LocalDate, Int>()

    fun add(dagen: LocalDate, dagsats: Int) {
        inntekter[dagen] = dagsats
    }

    fun inntekt(dagen: LocalDate) = inntekter.entries.sortedBy { it.key }.last { dagen.isAfter(it.key) }.value

}
