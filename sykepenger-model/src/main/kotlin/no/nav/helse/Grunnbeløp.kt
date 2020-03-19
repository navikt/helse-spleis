package no.nav.helse

import java.time.LocalDate

internal class Grunnbeløp private constructor(private val multiplier: Double) {
    private val grunnbeløp = mapOf<LocalDate, Int>(
        LocalDate.of(2019, 5, 1) to 99858,
        LocalDate.of(2018, 5, 1) to 96883,
        LocalDate.of(2017, 5, 1) to 93634,
        LocalDate.of(2016, 5, 1) to 92576,
        LocalDate.of(2015, 5, 1) to 90068,
        LocalDate.of(2014, 5, 1) to 88370,
        LocalDate.of(2013, 5, 1) to 85245,
        LocalDate.of(2012, 5, 1) to 82122,
        LocalDate.of(2011, 5, 1) to 79216,
        LocalDate.of(2010, 5, 1) to 75641
    )

    internal fun beløp(dato: LocalDate) =
        grunnbeløp.entries.sortedBy { it.key }.lastOrNull { dato.isAfter(it.key) }?.let { it.value * multiplier } ?:
            throw NoSuchElementException("Finner ingen grunnbeløp etter $dato")

    internal fun dagsats(dato: LocalDate) = beløp(dato) / 260.0

    companion object {
        val `6G` = Grunnbeløp(6.0)
        val halvG = Grunnbeløp(0.5)
        val `2G` = Grunnbeløp(2.0)
    }
}
