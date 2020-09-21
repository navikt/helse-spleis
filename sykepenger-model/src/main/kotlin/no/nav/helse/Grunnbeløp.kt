package no.nav.helse

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import java.time.LocalDate


internal class Grunnbeløp private constructor(private val multiplier: Double) {
    private val grunnbeløp = mapOf<LocalDate, Inntekt>(
        LocalDate.of(2020, 9, 21) to 101351.årlig,
        LocalDate.of(2019, 5, 1) to 99858.årlig,
        LocalDate.of(2018, 5, 1) to 96883.årlig,
        LocalDate.of(2017, 5, 1) to 93634.årlig,
        LocalDate.of(2016, 5, 1) to 92576.årlig,
        LocalDate.of(2015, 5, 1) to 90068.årlig,
        LocalDate.of(2014, 5, 1) to 88370.årlig,
        LocalDate.of(2013, 5, 1) to 85245.årlig,
        LocalDate.of(2012, 5, 1) to 82122.årlig,
        LocalDate.of(2011, 5, 1) to 79216.årlig,
        LocalDate.of(2010, 5, 1) to 75641.årlig
    )

    internal fun beløp(dato: LocalDate) =
        grunnbeløp.entries
            .filter { dato >= it.key }
            .maxBy { it.key }
            ?.let { it.value * multiplier }
            ?: throw NoSuchElementException("Finner ingen grunnbeløp etter $dato")

    internal fun dagsats(dato: LocalDate) = (beløp(dato)).rundTilDaglig()

    companion object {
        val `6G` = Grunnbeløp(6.0)
        val halvG = Grunnbeløp(0.5)
        val `2G` = Grunnbeløp(2.0)
        val `1G` = Grunnbeløp(1.0)
    }
}
