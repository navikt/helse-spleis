package no.nav.helse

import java.time.LocalDate

internal class Grunnbeløp private constructor(private val n: Int) {
    private val grunnbeløp = mapOf<LocalDate, Int>(
        LocalDate.of(2019, 5, 1) to 99858,
        LocalDate.of(2018, 5, 1) to 96883,
        LocalDate.of(2017, 5, 1) to 93634
    )

    internal operator fun invoke(dato: LocalDate) =
        grunnbeløp.entries.sortedBy { it.key }.last { dato.isAfter(it.key) }.value * n

    companion object {
        val `6G` = Grunnbeløp(6)
    }
}
