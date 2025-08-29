package no.nav.helse.feriepenger

import java.time.LocalDate
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

        assertEquals((1.januar til 17.januar).toList(), feriepengeberegner.beregnFeriepenger(a1).second.datoer)
    }

    @Test
    fun `kun utbetaling i spleis - 49 utbetalte dager`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 18.februar)
        )

        assertEquals((1.januar til 17.februar).toList(), feriepengeberegner.beregnFeriepenger(a1).second.datoer)
    }

    @Test
    fun `feriepengedager utbetalt til person i infotrygd skal helst ikke være med`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 17.februar)
        )

        assertEquals((1.januar til 17.februar).toList(), feriepengeberegner.beregnFeriepenger(a1).second.datoer)
    }

    @Test
    fun `feriepengedager beregnet fra arbeidsgiverutbetalinger i både spleis og infotrygd`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals((1.januar til 10.februar).toList() + (1.mars til 7.mars).toList(), feriepengeberegner.beregnFeriepenger(a1).second.datoer)
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1428.0,
                spleisFeriepengebeløp = 12546.0,
                totaltFeriepengebeløp = 13974.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 11934,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.februar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1428.0,
                spleisFeriepengebeløp = 12546.0,
                totaltFeriepengebeløp = 13974.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 11934,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = -1224,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1224.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2039.9999999999998,
                spleisFeriepengebeløp = 3060.0,
                totaltFeriepengebeløp = 5100.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 3060,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1224.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1224.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1224.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 3060.0,
                totaltFeriepengebeløp = 3060.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 3060,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = a2.spleisArbeidsgiver(1.januar til 10.januar) + a3.spleisArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a2,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 3060.0,
                totaltFeriepengebeløp = 3060.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 3060,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a2).first)

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a3,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 3671.9999999999995,
                totaltFeriepengebeløp = 3671.9999999999995,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 3672,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a3).first)
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1019.9999999999999,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1019.9999999999999,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1019.9999999999999
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = a2.itPerson(1.januar til 10.januar) + a3.itPerson(1.januar til 12.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a2,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1019.9999999999999,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1019.9999999999999,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1019.9999999999999
            )
        ), feriepengeberegner.beregnFeriepenger(a2).first)

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a3,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1224.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1224.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1224.0
            )
        ), feriepengeberegner.beregnFeriepenger(a3).first)
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2039.9999999999998,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 2039.9999999999998,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = a2.itArbeidsgiver(1.januar til 10.januar) + a3.itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a2,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2039.9999999999998,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 2039.9999999999998,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a2).first)

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a3,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2448.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 2448.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2448.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a3).first)
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2448.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 2448.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2448.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1019.9999999999999,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1019.9999999999999,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1019.9999999999999
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = a2.itPerson(1.januar til 10.januar) + a3.itPerson(1.januar til 12.januar),
            infotrygdArbeidsgiver = a2.itArbeidsgiver(1.januar til 14.januar) + a3.itArbeidsgiver(1.januar til 16.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a2,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2856.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 2856.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2856.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1019.9999999999999,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1019.9999999999999,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1019.9999999999999
            )
        ), feriepengeberegner.beregnFeriepenger(a2).first)

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a3,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 3264.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 3264.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 3264.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1224.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1224.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1224.0
            )
        ), feriepengeberegner.beregnFeriepenger(a3).first)
    }

    @Test
    fun `beregner feriepengene for gammel person`() {
        val feriepengeberegner = feriepengeberegner(
            alder = GAMMEL,
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 14.januar),
            spleisArbeidsgiver = spleisArbeidsgiver(15.januar til 31.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 3500.0,
                spleisFeriepengebeløp = 6375.0,
                totaltFeriepengebeløp = 9875.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 6375,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 3500.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1250.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1250.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1250.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner totalen av feriepengene til en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2039.9999999999998,
                spleisFeriepengebeløp = 3060.0,
                totaltFeriepengebeløp = 5100.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 3060,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1224.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1224.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1224.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner totalen av feriepengene til en person`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.mars til 20.mars),
            spleisPerson = spleisPerson(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 0.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 0.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 0.0
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2039.9999999999998,
                spleisFeriepengebeløp = 4079.9999999999995,
                totaltFeriepengebeløp = 6120.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 4080,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 2039.9999999999998,
                spleisFeriepengebeløp = 3060.0,
                totaltFeriepengebeløp = 5100.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 3060,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 2039.9999999999998
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1224.0,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1224.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1224.0
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver der infotrygd har brukt opp alle feriepengedagene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(Feriepengeberegningsresultat(
            orgnummer = a1,
            arbeidsgiver = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 5712.0,
                spleisFeriepengebeløp = 3060.0,
                totaltFeriepengebeløp = 8772.0,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 1020,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 7751.999999999999
            ),
            person = Feriepengeberegningsresultat.Beregningsverdier(
                infotrygdFeriepengebeløp = 1019.9999999999999,
                spleisFeriepengebeløp = 0.0,
                totaltFeriepengebeløp = 1019.9999999999999,
                differanseMellomTotalOgAlleredeUtbetaltAvInfotrygd = 0,
                hvaViHarBeregnetAtInfotrygdHarUtbetaltDouble = 1019.9999999999999
            )
        ), feriepengeberegner.beregnFeriepenger(a1).first)
    }

    private fun itPerson(periode: Periode) =
        a1.itPerson(periode)

    private fun String.itPerson(periode: Periode) =
        periode.map { Triple(it, this, InfotrygdPersonbeløp) }

    private fun itArbeidsgiver(periode: Periode) =
        a1.itArbeidsgiver(periode)

    private fun String.itArbeidsgiver(periode: Periode) =
        periode.map { Triple(it, this, InfotrygdArbeidsgiverbeløp) }

    private fun spleisArbeidsgiver(periode: Periode) =
        a1.spleisArbeidsgiver(periode)

    private fun spleisPerson(periode: Periode) =
        a1.spleisPerson(periode)

    private fun String.spleisArbeidsgiver(periode: Periode) =
        periode.map { Triple(it, this, SpleisArbeidsgiverbeløp) }

    private fun String.spleisPerson(periode: Periode) =
        periode.map { Triple(it, this, SpleisPersonbeløp) }

    private fun feriepengeberegner(
        alder: Alder = UNG,
        opptjeningsår: Year = Year.of(2018),
        infotrygdPerson: List<Triple<LocalDate, String, Int>> = emptyList(),
        infotrygdArbeidsgiver: List<Triple<LocalDate, String, Int>> = emptyList(),
        spleisArbeidsgiver: List<Triple<LocalDate, String, Int>> = emptyList(),
        spleisPerson: List<Triple<LocalDate, String, Int>> = emptyList()
    ): Feriepengeberegner {
        val builder = Feriepengegrunnlagstidslinje.Builder()
        leggTilGrunnlag(builder, infotrygdPerson, Feriepengegrunnlagsdag.Mottaker.PERSON, Feriepengegrunnlagsdag.Kilde.INFOTRYGD)
        leggTilGrunnlag(builder, infotrygdArbeidsgiver, Feriepengegrunnlagsdag.Mottaker.ARBEIDSGIVER, Feriepengegrunnlagsdag.Kilde.INFOTRYGD)
        leggTilGrunnlag(builder, spleisArbeidsgiver, Feriepengegrunnlagsdag.Mottaker.ARBEIDSGIVER, Feriepengegrunnlagsdag.Kilde.SPLEIS)
        leggTilGrunnlag(builder, spleisPerson, Feriepengegrunnlagsdag.Mottaker.PERSON, Feriepengegrunnlagsdag.Kilde.SPLEIS)
        val tidslinje = builder.build()

        return Feriepengeberegner(
            alder = alder,
            opptjeningsår = opptjeningsår,
            utbetalteDager = tidslinje
        )
    }

    private fun leggTilGrunnlag(builder: Feriepengegrunnlagstidslinje.Builder, liste: List<Triple<LocalDate, String, Int>>, mottaker: Feriepengegrunnlagsdag.Mottaker, kilde: Feriepengegrunnlagsdag.Kilde) {
        liste.forEach { (dato, orgnummer, beløp) ->
            builder.leggTilUtbetaling(dato, orgnummer, mottaker, kilde, beløp)
        }
    }
}
