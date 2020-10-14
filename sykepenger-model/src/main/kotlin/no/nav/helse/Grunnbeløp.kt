package no.nav.helse

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import java.time.LocalDate

internal class Grunnbeløp private constructor(private val multiplier: Double) {
    private val grunnbeløp = listOf(
        101351.årlig.gyldigFra(1.mai(2020), virkningsdato = 21.september(2020)),
        99858.årlig.gyldigFra(1.mai(2019)),
        96883.årlig.gyldigFra(1.mai(2018)),
        93634.årlig.gyldigFra(1.mai(2017)),
        92576.årlig.gyldigFra(1.mai(2016)),
        90068.årlig.gyldigFra(1.mai(2015)),
        88370.årlig.gyldigFra(1.mai(2014)),
        85245.årlig.gyldigFra(1.mai(2013)),
        82122.årlig.gyldigFra(1.mai(2012)),
        79216.årlig.gyldigFra(1.mai(2011)),
        75641.årlig.gyldigFra(1.mai(2010))
    )

    internal fun beløp(dato: LocalDate) = gjeldende(dato).beløp(multiplier)
    internal fun beløp(dato: LocalDate, virkningFra: LocalDate) = gjeldende(dato, virkningFra).beløp(multiplier)

    internal fun dagsats(dato: LocalDate) = beløp(dato).rundTilDaglig()
    internal fun dagsats(dato: LocalDate, virkningFra: LocalDate) = beløp(dato, virkningFra).rundTilDaglig()

    private fun gjeldende(dato: LocalDate, virkningFra: LocalDate? = null) =
        HistoriskGrunnbeløp.gjeldendeGrunnbeløp(grunnbeløp, dato, virkningFra ?: dato)

    companion object {
        val `6G` = Grunnbeløp(6.0)
        val halvG = Grunnbeløp(0.5)
        val `2G` = Grunnbeløp(2.0)
        val `1G` = Grunnbeløp(1.0)

        private fun Inntekt.gyldigFra(gyldigFra: LocalDate, virkningsdato: LocalDate = gyldigFra) = HistoriskGrunnbeløp(this, gyldigFra, virkningsdato)
        private fun Int.mai(år: Int) = LocalDate.of(år, 5, this)
        private fun Int.september(år: Int) = LocalDate.of(år, 9, this)
    }

    private class HistoriskGrunnbeløp(private val beløp: Inntekt, private val gyldigFra: LocalDate, private val virkningsdato: LocalDate = gyldigFra) {
        init {
            require(virkningsdato >= gyldigFra) { "Virkningsdato må være nyere eller lik gyldighetstidspunktet" }
        }

        companion object {
            fun gjeldendeGrunnbeløp(grunnbeløper: List<HistoriskGrunnbeløp>, dato: LocalDate, virkningFra: LocalDate): HistoriskGrunnbeløp {
                require(virkningFra >= dato) { "Virkningsdato må være nyere eller lik beregningsdato" }
                return grunnbeløper
                    .filter { virkningFra >= it.virkningsdato }
                    .maxBy { dato >= it.gyldigFra }
                    ?: throw NoSuchElementException("Finner ingen grunnbeløp etter $dato")
            }
        }

        fun beløp(multiplier: Double) = beløp * multiplier
    }
}
