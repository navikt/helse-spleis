package no.nav.helse

import java.time.LocalDate

internal class Grunnbeløp private constructor(private val multiplier: Double) {
    private val grunnbeløp = mapOf<LocalDate, Int>(
        LocalDate.of(2019, 5, 1) to 99858,
        LocalDate.of(2018, 5, 1) to 96883,
        LocalDate.of(2017, 5, 1) to 93634
    )

    internal fun beløp(dato: LocalDate) =
        grunnbeløp.entries.sortedBy { it.key }.last { dato.isAfter(it.key) }.value * multiplier

    internal fun dagsats(dato: LocalDate) = beløp(dato) / 260.0

    companion object {
        val `6G` = Grunnbeløp(6.0)
        val halvG = Grunnbeløp(0.5)
        val `2G` = Grunnbeløp(2.0)
    }
}
