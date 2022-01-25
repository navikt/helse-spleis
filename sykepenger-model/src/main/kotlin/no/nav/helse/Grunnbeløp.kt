package no.nav.helse

import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.årlig
import java.time.LocalDate

internal class Grunnbeløp private constructor(private val multiplier: Double) {
    private val grunnbeløp = listOf(
        106399.årlig.gyldigFra(1.mai(2021)),
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
        75641.årlig.gyldigFra(1.mai(2010)),
        72881.årlig.gyldigFra(1.mai(2009)),
        70256.årlig.gyldigFra(1.mai(2008)),
        66812.årlig.gyldigFra(1.mai(2007)),
        62892.årlig.gyldigFra(1.mai(2006)),
        60699.årlig.gyldigFra(1.mai(2005)),
        58778.årlig.gyldigFra(1.mai(2004)),
        56861.årlig.gyldigFra(1.mai(2003)),
        54170.årlig.gyldigFra(1.mai(2002)),
        51360.årlig.gyldigFra(1.mai(2001)),
        49090.årlig.gyldigFra(1.mai(2000)),
        46950.årlig.gyldigFra(1.mai(1999)),
        45370.årlig.gyldigFra(1.mai(1998)),
        42500.årlig.gyldigFra(1.mai(1997)),
        41000.årlig.gyldigFra(1.mai(1996)),
        39230.årlig.gyldigFra(1.mai(1995)),
        38080.årlig.gyldigFra(1.mai(1994)),
        37300.årlig.gyldigFra(1.mai(1993)),
        36500.årlig.gyldigFra(1.mai(1992)),
        35500.årlig.gyldigFra(1.mai(1991)),
        34100.årlig.gyldigFra(1.desember(1990)),
        34000.årlig.gyldigFra(1.mai(1990)),
        32700.årlig.gyldigFra(1.april(1989)),
        31000.årlig.gyldigFra(1.april(1988)),
        30400.årlig.gyldigFra(1.januar(1988)),
        29900.årlig.gyldigFra(1.mai(1987)),
        28000.årlig.gyldigFra(1.mai(1986)),
        26300.årlig.gyldigFra(1.januar(1986)),
        25900.årlig.gyldigFra(1.mai(1985)),
        24200.årlig.gyldigFra(1.mai(1984)),
        22600.årlig.gyldigFra(1.mai(1983)),
        21800.årlig.gyldigFra(1.januar(1983)),
        21200.årlig.gyldigFra(1.mai(1982)),
        19600.årlig.gyldigFra(1.oktober(1981)),
        19100.årlig.gyldigFra(1.mai(1981)),
        17400.årlig.gyldigFra(1.januar(1981)),
        16900.årlig.gyldigFra(1.mai(1980)),
        16100.årlig.gyldigFra(1.januar(1980)),
        15200.årlig.gyldigFra(1.januar(1979)),
        14700.årlig.gyldigFra(1.juli(1978)),
        14400.årlig.gyldigFra(1.desember(1977)),
        13400.årlig.gyldigFra(1.mai(1977)),
        13100.årlig.gyldigFra(1.januar(1977)),
        12100.årlig.gyldigFra(1.mai(1976)),
        11800.årlig.gyldigFra(1.januar(1976)),
        11000.årlig.gyldigFra(1.mai(1975)),
        10400.årlig.gyldigFra(1.januar(1975)),
        9700.årlig.gyldigFra(1.mai(1974)),
        9200.årlig.gyldigFra(1.januar(1974)),
        8500.årlig.gyldigFra(1.januar(1973)),
        7900.årlig.gyldigFra(1.januar(1972)),
        7500.årlig.gyldigFra(1.mai(1971)),
        7200.årlig.gyldigFra(1.januar(1971)),
        6800.årlig.gyldigFra(1.januar(1970)),
        6400.årlig.gyldigFra(1.januar(1969)),
        5900.årlig.gyldigFra(1.januar(1968)),
        5400.årlig.gyldigFra(1.januar(1967)),
    )

    internal fun beløp(dato: LocalDate) =
        gjeldende(dato).beløp(multiplier)

    internal fun beløp(dato: LocalDate, virkningFra: LocalDate) =
        gjeldende(dato, virkningFra).beløp(multiplier)

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
    }

    private class HistoriskGrunnbeløp(private val beløp: Inntekt, private val gyldigFra: LocalDate, private val virkningsdato: LocalDate = gyldigFra) {
        init {
            require(virkningsdato >= gyldigFra) { "Virkningsdato må være nyere eller lik gyldighetstidspunktet" }
        }

        companion object {
            fun gjeldendeGrunnbeløp(grunnbeløper: List<HistoriskGrunnbeløp>, dato: LocalDate, virkningFra: LocalDate): HistoriskGrunnbeløp {
                val virkningsdato = maxOf(dato, virkningFra)
                return grunnbeløper
                    .filter { virkningsdato >= it.virkningsdato && dato >= it.gyldigFra }
                    .maxByOrNull { it.virkningsdato }
                    ?: throw NoSuchElementException("Finner ingen grunnbeløp etter $dato")
            }
        }

        fun beløp(multiplier: Double) = beløp * multiplier
    }
}
