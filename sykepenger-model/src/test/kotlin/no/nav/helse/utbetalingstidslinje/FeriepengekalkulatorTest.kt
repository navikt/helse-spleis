package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Year

internal class FeriepengekalkulatorTest {
    private companion object {
        private const val ORGNUMMER = "123456789"
        private val UNG: Alder = Alder("01026000014")
        private const val UNG_SATS = 0.102
        private val GAMMEL: Alder = Alder("01025900065")
        private const val GAMMEL_SATS = 0.125
    }

    @Test
    fun `kun utbetaling i spleis - 48 utbetalte dager`() {
        val kalkulator = kalkulator(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 17.januar)
        )

        assertEquals((1.januar til 17.januar).toList(), kalkulator.feriepengedatoer())
    }

    @Test
    fun `kun utbetaling i spleis - 49 utbetalte dager`() {
        val kalkulator = kalkulator(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 18.februar)
        )

        assertEquals((1.januar til 17.februar).toList(), kalkulator.feriepengedatoer())
    }

    @Test
    fun `feriepengedager utbetalt til person i infotrygd skal alltid være med`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 17.februar)
        )

        assertEquals((1.januar til 7.februar).toList() + (1.mars til 10.mars).toList(), kalkulator.feriepengedatoer())
    }

    @Test
    fun `feriepengedager beregnet fra arbeidsgiverutbetalinger i både spleis og infotrygd`() {
        val kalkulator = kalkulator(
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals((1.januar til 10.februar).toList() + (1.mars til 7.mars).toList(), kalkulator.feriepengedatoer())
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene`() {
        val kalkulator = kalkulator(
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals(41 * 3000 * UNG_SATS, kalkulator.beregnFeriepengerForSpleis())
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals(12 * 1000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdPerson())
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 2000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdArbeidsgiver())
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((12 * 1000 + 10 * 2000) * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygd())
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for en arbeidsgiver`() {
        val kalkulator = kalkulator(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 3000 * UNG_SATS, kalkulator.beregnFeriepengerForSpleis(ORGNUMMER))
        assertEquals(0.0, kalkulator.beregnFeriepengerForSpleis("otherOrgn"))
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for to arbeidsgivere`() {
        val kalkulator = kalkulator(
            spleisArbeidsgiver = "456789123".spleisArbeidsgiver(1.januar til 10.januar) + "789123456".spleisArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * 3000 * UNG_SATS, kalkulator.beregnFeriepengerForSpleis())
        assertEquals(10 * 3000 * UNG_SATS, kalkulator.beregnFeriepengerForSpleis("456789123"))
        assertEquals(12 * 3000 * UNG_SATS, kalkulator.beregnFeriepengerForSpleis("789123456"))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for en arbeidsgiver`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(1.januar til 10.januar)
        )

        assertEquals(10 * 1000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdPerson(ORGNUMMER))
        assertEquals(0.0, kalkulator.beregnFeriepengerForInfotrygdPerson("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for to arbeidsgivere`() {
        val kalkulator = kalkulator(
            infotrygdPerson = "456789123".itPerson(1.januar til 10.januar) + "789123456".itPerson(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * 1000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdPerson())
        assertEquals(10 * 1000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdPerson("456789123"))
        assertEquals(12 * 1000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdPerson("789123456"))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for en arbeidsgiver`() {
        val kalkulator = kalkulator(
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 2000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdArbeidsgiver(ORGNUMMER))
        assertEquals(0.0, kalkulator.beregnFeriepengerForInfotrygdArbeidsgiver("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for to arbeidsgivere`() {
        val kalkulator = kalkulator(
            infotrygdArbeidsgiver = "456789123".itArbeidsgiver(1.januar til 10.januar) + "789123456".itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * 2000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdArbeidsgiver())
        assertEquals(10 * 2000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdArbeidsgiver("456789123"))
        assertEquals(12 * 2000 * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygdArbeidsgiver("789123456"))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for en arbeidsgiver`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 * 1000 + 12 * 2000) * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygd(ORGNUMMER))
        assertEquals(0.0, kalkulator.beregnFeriepengerForInfotrygd("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for to arbeidsgivere`() {
        val kalkulator = kalkulator(
            infotrygdPerson = "456789123".itPerson(1.januar til 10.januar) + "789123456".itPerson(1.januar til 12.januar),
            infotrygdArbeidsgiver = "456789123".itArbeidsgiver(1.januar til 14.januar) + "789123456".itArbeidsgiver(1.januar til 16.januar)
        )

        assertEquals(((10 + 12) * 1000 + (14 + 16) * 2000) * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygd())
        assertEquals((10 * 1000 + 14 * 2000) * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygd("456789123"))
        assertEquals((12 * 1000 + 16 * 2000) * UNG_SATS, kalkulator.beregnFeriepengerForInfotrygd("789123456"))
    }

    @Test
    fun `beregner feriepengene for gammel person`() {
        val kalkulator = kalkulator(
            alder = GAMMEL,
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 14.januar),
            spleisArbeidsgiver = spleisArbeidsgiver(15.januar til 31.januar)
        )

        assertEquals((10 * 1000 + 14 * 2000) * GAMMEL_SATS, kalkulator.beregnFeriepengerForInfotrygd())
        assertEquals(17 * 3000 * GAMMEL_SATS, kalkulator.beregnFeriepengerForSpleis())
    }

    @Test
    fun `beregner totalen av feriepengene til en arbeidsgiver`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((10 * 2000 + 10 * 3000) * UNG_SATS, kalkulator.beregnFeriepengerForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver`() {
        val kalkulator = kalkulator(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 3000 * UNG_SATS, kalkulator.beregnFeriepengedifferansenForArbeidsgiver(ORGNUMMER))
    }

    private fun itPerson(periode: Periode) =
        ORGNUMMER.itPerson(periode)

    private fun String.itPerson(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.InfotrygdPerson(this, it, 1000) }

    private fun itArbeidsgiver(periode: Periode) =
        ORGNUMMER.itArbeidsgiver(periode)

    private fun String.itArbeidsgiver(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver(this, it, 2000) }

    private fun spleisArbeidsgiver(periode: Periode) =
        ORGNUMMER.spleisArbeidsgiver(periode)

    private fun String.spleisArbeidsgiver(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver(this, it, 3000) }

    private fun kalkulator(
        alder: Alder = UNG,
        feriepengeår: Year = Year.of(2018),
        infotrygdPerson: List<Feriepengeberegner.UtbetaltDag.InfotrygdPerson> = emptyList(),
        infotrygdArbeidsgiver: List<Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver> = emptyList(),
        spleisArbeidsgiver: List<Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver> = emptyList()
    ) = Feriepengeberegner(
        alder,
        feriepengeår,
        infotrygdPerson + infotrygdArbeidsgiver + spleisArbeidsgiver
    )
}
