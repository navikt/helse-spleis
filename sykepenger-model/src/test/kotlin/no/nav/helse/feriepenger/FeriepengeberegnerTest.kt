package no.nav.helse.feriepenger

import java.time.Year
import no.nav.helse.Alder
import no.nav.helse.Alder.Companion.alder
import no.nav.helse.april
import no.nav.helse.dsl.a1
import no.nav.helse.dsl.a2
import no.nav.helse.dsl.a3
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.mars
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class FeriepengeberegnerTest {
    private companion object {
        private val UNG: Alder = 1.februar(1960).alder
        private val GAMMEL: Alder = 1.februar(1959).alder

        private const val InfotrygdPersonbeløp = 1000
        private const val InfotrygdArbeidsgiverbeløp = 2000
        private const val SpleisArbeidsgiverbeløp = 3000
        private const val SpleisPersonbeløp = 4000
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

        assertEquals(12546.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver())
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

        assertEquals(2039.9999999999998, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver())
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(3264.0, feriepengeberegner.beregnFeriepengerForInfotrygd())
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(3060.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver(a1))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver(a2))
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = a2.spleisArbeidsgiver(1.januar til 10.januar) + a3.spleisArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals(6732.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver())
        assertEquals(3060.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver(a2))
        assertEquals(3671.9999999999995, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver(a3))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar)
        )

        assertEquals(1019.9999999999999, feriepengeberegner.beregnFeriepengerForInfotrygdPerson(a1))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdPerson(a2))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = a2.itPerson(1.januar til 10.januar) + a3.itPerson(1.januar til 12.januar)
        )

        assertEquals(2244.0, feriepengeberegner.beregnFeriepengerForInfotrygdPerson())
        assertEquals(1019.9999999999999, feriepengeberegner.beregnFeriepengerForInfotrygdPerson(a2))
        assertEquals(1224.0, feriepengeberegner.beregnFeriepengerForInfotrygdPerson(a3))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(2039.9999999999998, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(a1))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(a2))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = a2.itArbeidsgiver(1.januar til 10.januar) + a3.itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals(4488.0, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver())
        assertEquals(2039.9999999999998, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(a2))
        assertEquals(2448.0, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(a3))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals(3468.0, feriepengeberegner.beregnFeriepengerForInfotrygd(a1))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygd(a2))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = a2.itPerson(1.januar til 10.januar) + a3.itPerson(1.januar til 12.januar),
            infotrygdArbeidsgiver = a2.itArbeidsgiver(1.januar til 14.januar) + a3.itArbeidsgiver(1.januar til 16.januar)
        )

        assertEquals(8364.0, feriepengeberegner.beregnFeriepengerForInfotrygd())
        assertEquals(3875.9999999999995, feriepengeberegner.beregnFeriepengerForInfotrygd(a2))
        assertEquals(4488.0, feriepengeberegner.beregnFeriepengerForInfotrygd(a3))
    }

    @Test
    fun `beregner feriepengene for gammel person`() {
        val feriepengeberegner = feriepengeberegner(
            alder = GAMMEL,
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 14.januar),
            spleisArbeidsgiver = spleisArbeidsgiver(15.januar til 31.januar)
        )

        assertEquals(4750.0, feriepengeberegner.beregnFeriepengerForInfotrygd())
        assertEquals(6375.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver())
    }

    @Test
    fun `beregner totalen av feriepengene til en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(5100.0, feriepengeberegner.beregnFeriepengerForArbeidsgiver(a1))
    }

    @Test
    fun `beregner totalen av feriepengene til en person`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.mars til 20.mars),
            spleisPerson = spleisPerson(1.januar til 10.januar)
        )

        assertEquals(6120.0, feriepengeberegner.beregnFeriepengerForPerson(a1))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(3060.0, feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(a1))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver der infotrygd har brukt opp alle feriepengedagene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(1019.9999999999999, feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(a1))
    }

    @Test
    fun `beregner utbetalte feriepenger i infotrygd for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(7751.999999999999, feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(a1))
    }

    private fun itPerson(periode: Periode) =
        a1.itPerson(periode)

    private fun String.itPerson(periode: Periode) =
        periode.map { Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdPerson(this, it, InfotrygdPersonbeløp) }

    private fun itArbeidsgiver(periode: Periode) =
        a1.itArbeidsgiver(periode)

    private fun String.itArbeidsgiver(periode: Periode) =
        periode.map { Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdArbeidsgiver(this, it, InfotrygdArbeidsgiverbeløp) }

    private fun spleisArbeidsgiver(periode: Periode) =
        a1.spleisArbeidsgiver(periode)

    private fun spleisPerson(periode: Periode) =
        a1.spleisPerson(periode)

    private fun String.spleisArbeidsgiver(periode: Periode) =
        periode.map { Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisArbeidsgiver(this, it, SpleisArbeidsgiverbeløp) }

    private fun String.spleisPerson(periode: Periode) =
        periode.map { Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisPerson(this, it, SpleisPersonbeløp) }

    private fun feriepengeberegner(
        alder: Alder = UNG,
        opptjeningsår: Year = Year.of(2018),
        infotrygdPerson: List<Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdPerson> = emptyList(),
        infotrygdArbeidsgiver: List<Feriepengeutbetalinggrunnlag.UtbetaltDag.InfotrygdArbeidsgiver> = emptyList(),
        spleisArbeidsgiver: List<Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisArbeidsgiver> = emptyList(),
        spleisPerson: List<Feriepengeutbetalinggrunnlag.UtbetaltDag.SpleisPerson> = emptyList()
    ) = Feriepengeberegner(
        alder,
        opptjeningsår,
        infotrygdPerson + infotrygdArbeidsgiver + spleisArbeidsgiver + spleisPerson
    )
}
