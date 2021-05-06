package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.UtbetalingshistorikkForFeriepenger
import no.nav.helse.person.AbstractPersonTest
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.Utbetalingsperiode
import no.nav.helse.sykdomstidslinje.erHelg
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import no.nav.helse.økonomi.Inntekt.Companion.månedlig
import no.nav.helse.økonomi.Prosentdel.Companion.prosent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year
import java.util.*

internal class FeriepengeberegnerTest {
    private companion object {
        private val alder = Alder(AbstractPersonTest.UNG_PERSON_FNR_2018)
    }

    @Test
    fun `Finner datoer for feriepengeberegning med 48 sammenhengende utbetalingsdager i IT fra første januar`() {
        val historikk = utbetalingshistorikkForFeriepenger(
            listOf(Utbetalingsperiode(AbstractPersonTest.ORGNUMMER, 1.januar,  7.mars, 100.prosent, 1000.månedlig))
        )

        val beregner = Feriepengeberegner(historikk, alder)

        assertEquals(48, beregner.count())
    }

//    @Test
//    fun `Finner datoer for feriepengeberegning med 48 sammenhengende NAVdager fra første januar`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(66.NAVv2))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(utbetalingstidslinjer, 1.januar til 7.mars)
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med 49 sammenhengende NAVdager fra første januar`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(67.NAVv2))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(utbetalingstidslinjer, 1.januar til 7.mars)
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med 47 sammenhengende NAVdager fra første januar`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(65.NAVv2))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(utbetalingstidslinjer, 1.januar til 6.mars)
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med 48 sammenhengende NAVdager fra niende mai`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(66.NAVv2, startDato = 9.mai))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(utbetalingstidslinjer, 9.mai til 13.juli)
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med 47 ikke-sammenhengende NAVdager`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(22.NAVv2, 22.ARBv2, 11.NAVv2, 11.FRIv2, 11.NAVv2, 22.AVVv2, 22.NAVv2))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(
//            utbetalingstidslinjer,
//            1.januar til 22.januar,
//            14.februar til 24.februar,
//            8.mars til 18.mars,
//            10.april til 1.mai
//        )
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med over 48 ikke-sammenhengende NAVdager`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(22.NAVv2, 22.ARBv2, 11.NAVv2, 11.FRIv2, 11.NAVv2, 22.AVVv2, 33.NAVv2))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(
//            utbetalingstidslinjer,
//            1.januar til 22.januar,
//            14.februar til 24.februar,
//            8.mars til 18.mars,
//            10.april til 2.mai
//        )
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med to helt overlappende utbetalingstidslinjer`() {
//        val utbetalingstidslinjer = listOf(tidslinjeOf(66.NAVv2(500)), tidslinjeOf(66.NAVv2(500)))
//
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(
//            utbetalingstidslinjer,
//            1.januar til 7.mars,
//            1.januar til 7.mars,
//            dekningsgrunnlag = 500
//        )
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med to delvis overlappende utbetalingstidslinjer`() {
//        val utbetalingstidslinjer = listOf(
//            tidslinjeOf(7.NAVv2(500), startDato = 1.januar),
//            tidslinjeOf(10.NAVv2(500), startDato = 3.januar)
//        )
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(
//            utbetalingstidslinjer,
//            1.januar til 7.januar,
//            3.januar til 12.januar,
//            dekningsgrunnlag = 500
//        )
//    }
//
//    @Test
//    fun `Finner datoer for feriepengeberegning med to ikke-overlappende utbetalingstidslinjer`() {
//        val utbetalingstidslinjer = listOf(
//            tidslinjeOf(7.NAVv2(500), 14.ARBv2(500), startDato = 1.januar),
//            tidslinjeOf(7.NAVv2(500), startDato = 15.januar)
//        )
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.januar).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2018)).beregn()
//        assertFeriepengedager(utbetalingstidslinjer, 1.januar til 5.januar, 15.januar til 19.januar, dekningsgrunnlag = 500)
//    }
//
//    //TODO: Gir testen mening lenger?
//    @Test
//    fun `Finner datoer for feriepengeberegning der utbetalingstidslinjen går over nyttår`() {
//        val utbetalingstidslinjer = listOf(
//            tidslinjeOf(92.NAVv2, 68.NAVv2, startDato = 1.oktober(2020))
//        )
//        MaksimumUtbetaling(utbetalingstidslinjer, Aktivitetslogg(), 1.oktober(2020)).betal()
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2020)).beregn()
//        assertFeriepengedager(
//            utbetalingstidslinjer,
//            1.oktober(2020) til 7.desember(2020)
//        )
//        Feriepengeberegner(utbetalingstidslinjer, alder, Year.of(2021)).beregn()
//        assertFeriepengedager(
//            utbetalingstidslinjer,
//            1.oktober(2020) til 7.desember(2020),
//            1.januar(2021) til 9.mars(2021)
//        )
//    }

    private fun assertFeriepengedager(utbetalingstidslinjer: List<Utbetalingstidslinje>, vararg perioder: Periode, dekningsgrunnlag: Number = 1200) {
        val datoer = perioder.fold(emptyList<LocalDate>()) { acc, periode ->
            acc + periode.filterNot { it.erHelg() }
        }
        val expectedSum = datoer.sumByDouble { dekningsgrunnlag.toDouble() * 0.102 }
    }

    private fun utbetalingshistorikkForFeriepenger(utbetalinger: List<Infotrygdperiode>) =
        UtbetalingshistorikkForFeriepenger(
            UUID.randomUUID(),
            AbstractPersonTest.AKTØRID,
            AbstractPersonTest.ORGNUMMER,
            utbetalinger,
            emptyList(),
            emptyList(),
            false,
            emptyMap(),
            Year.of(2020)
        )
}
