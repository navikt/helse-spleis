package no.nav.helse.person.inntekt

import no.nav.helse.dsl.BeløpstidslinjeDsl.Arbeidsgiver
import no.nav.helse.dsl.BeløpstidslinjeDsl.fra
import no.nav.helse.dsl.BeløpstidslinjeDsl.hele
import no.nav.helse.dsl.BeløpstidslinjeDsl.og
import no.nav.helse.dsl.BeløpstidslinjeDsl.oppgir
import no.nav.helse.dsl.BeløpstidslinjeDsl.til
import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.person.inntekt.NyInntektUnderveis.Companion.merge
import no.nav.helse.økonomi.Inntekt.Companion.daglig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class NyInntektUnderveisTest {

    @Test
    fun `merge - overskriver inntekt for periode`() {
        val eksisterende = listOf(
            NyInntektUnderveis("a1", Arbeidsgiver oppgir 500.daglig hele januar)
        )

        val ny = listOf(
            NyInntektUnderveis("a1", Arbeidsgiver oppgir 1000.daglig fra 10.januar til 25.januar)
        )
        val result = eksisterende.merge(10.januar til 25.januar, ny)

        val forventetTidslinje =
            (Arbeidsgiver oppgir 500.daglig fra 1.januar til 9.januar) og
                (Arbeidsgiver oppgir 1000.daglig fra 10.januar til 25.januar) og
                (Arbeidsgiver oppgir 500.daglig fra 26.januar til 31.januar)

        assertEquals(1, result.size)
        assertEquals(
            listOf(1.januar til 31.januar),
            result.single().beløpstidslinje.perioderMedBeløp
        )
        assertEquals(forventetTidslinje, result.single().beløpstidslinje)
    }

    @Test
    fun `merge - fjerner inntekt for periode`() {
        val eksisterende = listOf(
            NyInntektUnderveis("a1", Arbeidsgiver oppgir 500.daglig hele januar)
        )

        val ny = emptyList<NyInntektUnderveis>()
        val result = eksisterende.merge(10.januar til 25.januar, ny)

        assertEquals(1, result.size)
        assertEquals(
            listOf(1.januar til 9.januar, 26.januar til 31.januar),
            result.single().beløpstidslinje.perioderMedBeløp
        )
    }

    @Test
    fun `merge - fjerner ikke inntekt utenfor periode`() {
        val eksisterende = listOf(
            NyInntektUnderveis("a1", Arbeidsgiver oppgir 500.daglig hele januar)
        )

        val ny = emptyList<NyInntektUnderveis>()
        val result = eksisterende.merge(februar, ny)

        assertEquals(1, result.size)
        assertEquals(
            listOf(1.januar til 31.januar),
            result.single().beløpstidslinje.perioderMedBeløp
        )
    }
}
