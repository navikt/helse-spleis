package no.nav.helse.utbetalingstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.somFødselsnummer
import no.nav.helse.testhelpers.april
import no.nav.helse.testhelpers.februar
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Year

internal class FeriepengeberegnerTest {
    private companion object {
        private const val ORGNUMMER = "123456789"
        private val UNG: Alder = "01026000014".somFødselsnummer().alder()
        private const val UNG_SATS = 0.102
        private val GAMMEL: Alder = "01025900065".somFødselsnummer().alder()
        private const val GAMMEL_SATS = 0.125
    }

    @Test
    fun `kun utbetaling i spleis - 48 utbetalte dager`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 17.januar)
        )

        assertEquals((1.januar til 17.januar).toList(), feriepengeberegner.feriepengedatoer())
    }

    @Test
    fun `kun utbetaling i spleis - 49 utbetalte dager`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 18.februar)
        )

        assertEquals((1.januar til 17.februar).toList(), feriepengeberegner.feriepengedatoer())
    }

    @Test
    fun `feriepengedager utbetalt til person i infotrygd skal helst ikke være med`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 17.februar)
        )

        assertEquals((1.januar til 17.februar).toList(), feriepengeberegner.feriepengedatoer())
    }

    @Test
    fun `feriepengedager beregnet fra arbeidsgiverutbetalinger i både spleis og infotrygd`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals((1.januar til 10.februar).toList() + (1.mars til 7.mars).toList(), feriepengeberegner.feriepengedatoer())
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals(41 * 3000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleis())
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdPerson())
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 2000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver())
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((12 * 1000 + 10 * 2000) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd())
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 3000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleis(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForSpleis("otherOrgn"))
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = "456789123".spleisArbeidsgiver(1.januar til 10.januar) + "789123456".spleisArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * 3000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleis())
        assertEquals(10 * 3000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleis("456789123"))
        assertEquals(12 * 3000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleis("789123456"))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar)
        )

        assertEquals(10 * 1000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdPerson("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = "456789123".itPerson(1.januar til 10.januar) + "789123456".itPerson(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * 1000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson())
        assertEquals(10 * 1000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson("456789123"))
        assertEquals(12 * 1000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson("789123456"))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 2000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = "456789123".itArbeidsgiver(1.januar til 10.januar) + "789123456".itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * 2000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver())
        assertEquals(10 * 2000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver("456789123"))
        assertEquals(12 * 2000 * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver("789123456"))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 * 1000 + 12 * 2000) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygd("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = "456789123".itPerson(1.januar til 10.januar) + "789123456".itPerson(1.januar til 12.januar),
            infotrygdArbeidsgiver = "456789123".itArbeidsgiver(1.januar til 14.januar) + "789123456".itArbeidsgiver(1.januar til 16.januar)
        )

        assertEquals(((10 + 12) * 1000 + (14 + 16) * 2000) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd())
        assertEquals((10 * 1000 + 14 * 2000) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd("456789123"))
        assertEquals((12 * 1000 + 16 * 2000) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd("789123456"))
    }

    @Test
    fun `beregner feriepengene for gammel person`() {
        val feriepengeberegner = feriepengeberegner(
            alder = GAMMEL,
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 14.januar),
            spleisArbeidsgiver = spleisArbeidsgiver(15.januar til 31.januar)
        )

        assertEquals((10 * 1000 + 14 * 2000) * GAMMEL_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd())
        assertEquals(17 * 3000 * GAMMEL_SATS, feriepengeberegner.beregnFeriepengerForSpleis())
    }

    @Test
    fun `beregner totalen av feriepengene til en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((10 * 2000 + 10 * 3000) * UNG_SATS, feriepengeberegner.beregnFeriepengerForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * 3000 * UNG_SATS, feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver der infotrygd har brukt opp alle feriepengedagene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((10 * 3000 - 10 * 2000) * UNG_SATS, feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner utbetalte feriepenger i infotrygd for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(38 * 2000 * UNG_SATS, feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(ORGNUMMER))
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

    private fun feriepengeberegner(
        alder: Alder = UNG,
        opptjeningsår: Year = Year.of(2018),
        infotrygdPerson: List<Feriepengeberegner.UtbetaltDag.InfotrygdPerson> = emptyList(),
        infotrygdArbeidsgiver: List<Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver> = emptyList(),
        spleisArbeidsgiver: List<Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver> = emptyList()
    ) = Feriepengeberegner(
        alder,
        opptjeningsår,
        infotrygdPerson + infotrygdArbeidsgiver + spleisArbeidsgiver
    )
}
