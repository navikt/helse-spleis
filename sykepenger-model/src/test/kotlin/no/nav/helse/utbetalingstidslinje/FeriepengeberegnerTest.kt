package no.nav.helse.utbetalingstidslinje

import no.nav.helse.*
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Year
import no.nav.helse.Alder.Companion.alder

internal class FeriepengeberegnerTest {
    private companion object {
        private const val ORGNUMMER = "123456789"
        private val UNG: Alder = 1.februar(1960).alder
        private const val UNG_SATS = 0.102
        private val GAMMEL: Alder = 1.februar(1959).alder
        private const val GAMMEL_SATS = 0.125

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

        assertEquals(41 * SpleisArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver())
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

        assertEquals(10 * InfotrygdArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver())
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((12 * InfotrygdPersonbeløp + 10 * InfotrygdArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd())
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * SpleisArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver("otherOrgn"))
    }

    @Test
    fun `beregner spleis sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            spleisArbeidsgiver = "456789123".spleisArbeidsgiver(1.januar til 10.januar) + "789123456".spleisArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * SpleisArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver())
        assertEquals(10 * SpleisArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver("456789123"))
        assertEquals(12 * SpleisArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver("789123456"))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar)
        )

        assertEquals(10 * InfotrygdPersonbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdPerson("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd-person sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = "456789123".itPerson(1.januar til 10.januar) + "789123456".itPerson(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * InfotrygdPersonbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson())
        assertEquals(10 * InfotrygdPersonbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson("456789123"))
        assertEquals(12 * InfotrygdPersonbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdPerson("789123456"))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * InfotrygdArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd-arbeidsgiver sitt bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdArbeidsgiver = "456789123".itArbeidsgiver(1.januar til 10.januar) + "789123456".itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 + 12) * InfotrygdArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver())
        assertEquals(10 * InfotrygdArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver("456789123"))
        assertEquals(12 * InfotrygdArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygdArbeidsgiver("789123456"))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 12.januar)
        )

        assertEquals((10 * InfotrygdPersonbeløp + 12 * InfotrygdArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd(ORGNUMMER))
        assertEquals(0.0, feriepengeberegner.beregnFeriepengerForInfotrygd("otherOrgn"))
    }

    @Test
    fun `beregner infotrygd sitt totale bidrag til feriepengene for to arbeidsgivere`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = "456789123".itPerson(1.januar til 10.januar) + "789123456".itPerson(1.januar til 12.januar),
            infotrygdArbeidsgiver = "456789123".itArbeidsgiver(1.januar til 14.januar) + "789123456".itArbeidsgiver(1.januar til 16.januar)
        )

        assertEquals(((10 + 12) * InfotrygdPersonbeløp + (14 + 16) * InfotrygdArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd())
        assertEquals((10 * InfotrygdPersonbeløp + 14 * InfotrygdArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd("456789123"))
        assertEquals((12 * InfotrygdPersonbeløp + 16 * InfotrygdArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd("789123456"))
    }

    @Test
    fun `beregner feriepengene for gammel person`() {
        val feriepengeberegner = feriepengeberegner(
            alder = GAMMEL,
            infotrygdPerson = itPerson(1.januar til 10.januar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.januar til 14.januar),
            spleisArbeidsgiver = spleisArbeidsgiver(15.januar til 31.januar)
        )

        assertEquals((10 * InfotrygdPersonbeløp + 14 * InfotrygdArbeidsgiverbeløp) * GAMMEL_SATS, feriepengeberegner.beregnFeriepengerForInfotrygd())
        assertEquals(17 * SpleisArbeidsgiverbeløp * GAMMEL_SATS, feriepengeberegner.beregnFeriepengerForSpleisArbeidsgiver())
    }

    @Test
    fun `beregner totalen av feriepengene til en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((10 * InfotrygdArbeidsgiverbeløp + 10 * SpleisArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner totalen av feriepengene til en person`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.mars til 20.mars),
            spleisPerson = spleisPerson(1.januar til 10.januar)
        )

        assertEquals((20 * InfotrygdPersonbeløp + 10 * SpleisPersonbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengerForPerson(ORGNUMMER))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(20.mars til 31.mars),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 10.mars),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(10 * SpleisArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner differansen av feriepengene som er utbetalt fra infotrygd og det som faktisk skal utbetales for arbeidsgiver der infotrygd har brukt opp alle feriepengedagene`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals((10 * SpleisArbeidsgiverbeløp - 10 * InfotrygdArbeidsgiverbeløp) * UNG_SATS, feriepengeberegner.beregnFeriepengedifferansenForArbeidsgiver(ORGNUMMER))
    }

    @Test
    fun `beregner utbetalte feriepenger i infotrygd for en arbeidsgiver`() {
        val feriepengeberegner = feriepengeberegner(
            infotrygdPerson = itPerson(1.februar til 10.februar),
            infotrygdArbeidsgiver = itArbeidsgiver(1.mars til 17.april),
            spleisArbeidsgiver = spleisArbeidsgiver(1.januar til 10.januar)
        )

        assertEquals(38 * InfotrygdArbeidsgiverbeløp * UNG_SATS, feriepengeberegner.beregnUtbetalteFeriepengerForInfotrygdArbeidsgiver(ORGNUMMER))
    }

    private fun itPerson(periode: Periode) =
        ORGNUMMER.itPerson(periode)

    private fun String.itPerson(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.InfotrygdPerson(this, it, InfotrygdPersonbeløp) }

    private fun itArbeidsgiver(periode: Periode) =
        ORGNUMMER.itArbeidsgiver(periode)

    private fun String.itArbeidsgiver(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver(this, it, InfotrygdArbeidsgiverbeløp) }

    private fun spleisArbeidsgiver(periode: Periode) =
        ORGNUMMER.spleisArbeidsgiver(periode)

    private fun spleisPerson(periode: Periode) =
        ORGNUMMER.spleisPerson(periode)

    private fun String.spleisArbeidsgiver(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver(this, it, SpleisArbeidsgiverbeløp) }

    private fun String.spleisPerson(periode: Periode) =
        periode.map { Feriepengeberegner.UtbetaltDag.SpleisPerson(this, it, SpleisPersonbeløp) }

    private fun feriepengeberegner(
        alder: Alder = UNG,
        opptjeningsår: Year = Year.of(2018),
        infotrygdPerson: List<Feriepengeberegner.UtbetaltDag.InfotrygdPerson> = emptyList(),
        infotrygdArbeidsgiver: List<Feriepengeberegner.UtbetaltDag.InfotrygdArbeidsgiver> = emptyList(),
        spleisArbeidsgiver: List<Feriepengeberegner.UtbetaltDag.SpleisArbeidsgiver> = emptyList(),
        spleisPerson: List<Feriepengeberegner.UtbetaltDag.SpleisPerson> = emptyList()
    ) = Feriepengeberegner(
        alder,
        opptjeningsår,
        infotrygdPerson + infotrygdArbeidsgiver + spleisArbeidsgiver + spleisPerson
    )
}
