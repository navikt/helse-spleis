package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.august
import no.nav.helse.desember
import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.i
import no.nav.helse.januar
import no.nav.helse.juni
import no.nav.helse.person.Refusjonshistorikk.Refusjon
import no.nav.helse.person.Refusjonshistorikk.Refusjon.EndringIRefusjon.Companion.gråsonen
import no.nav.helse.september
import no.nav.helse.økonomi.Inntekt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class GråsoneTest  {

    @Test
    fun `eksempler hvor det ikke er noen gråsoner`() {
        assertNull(gråsonen(emptyList(), null))
        assertNull(gråsonen(listOf(1.januar til 31.januar), null))
        assertNull(gråsonen(emptyList(), 1.januar))

        assertNull(gråsonen(listOf(1.januar til 31.januar), 31.desember(2017)))
        assertNull(gråsonen(listOf(1.januar til 31.januar), 1.januar))
        assertNull(gråsonen(listOf(1.januar til 31.januar), 31.januar))
        assertNull(gråsonen(listOf(1.januar til 31.januar), 1.februar))
    }

    @Test
    fun `eksempel på sprø gråsone`() {
        val arbeidsgiverperiode = listOf(
            (17.august til 17.august i 2022),
            (22.august til 26.august i 2022),
            (7.september til 7.september i 2022),
            (12.september til 20.september i 2022)
        )

        assertNull(gråsonen(arbeidsgiverperiode, 21.september i 2022))
        assertEquals(21.september(2022) til 21.september(2022), gråsonen(arbeidsgiverperiode, 22.september i 2022))
        assertEquals(21.september(2022) til 22.september(2022), gråsonen(arbeidsgiverperiode, 23.september i 2022))
    }

    @Test
    fun `eksempler hvor det er gråsoner`() {
        val arbeidsgiverperiode = listOf(1.januar til 31.januar)
        assertEquals(1.februar til 1.februar, gråsonen(arbeidsgiverperiode, 2.februar))
        assertEquals(1.februar til 6.juni, gråsonen(arbeidsgiverperiode, 7.juni))
    }

    private companion object {
        private fun gråsonen(arbeidsgiverperiode: List<Periode>, førsteFraværsdag: LocalDate?) =
            Refusjon(UUID.randomUUID(), førsteFraværsdag, arbeidsgiverperiode, Inntekt.INGEN, null, emptyList()).gråsonen()
    }
}