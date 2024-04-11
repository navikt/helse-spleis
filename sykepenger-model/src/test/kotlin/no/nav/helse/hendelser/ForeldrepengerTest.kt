package no.nav.helse.hendelser

import no.nav.helse.februar
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ForeldrepengerTest {

    private fun skalOppdatereHistorikk(foreldrepenger: Foreldrepenger, periode: Periode, periodeRettEtter: Periode? = null): Boolean {
        return foreldrepenger.skalOppdatereHistorikk(periode, periodeRettEtter).first
    }

    @Test
    fun `foreldrepenger fullstendig i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 31.januar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger før perioden`() {
        val vedtaksperiode = 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger før og i perioden`() {
        val vedtaksperiode = 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 28.februar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen av perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(20.januar til 31.januar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }
    @Test
    fun `foreldrepenger i snuten av perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 20.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i snuten og før perioden`() {
        val vedtaksperiode = 1.februar til 20.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 5.februar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen og etter perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(20.januar til 10.februar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(
            ForeldrepengerPeriode(1.januar til 10.januar, 100),
            ForeldrepengerPeriode(20.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket sammenhengende i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(
            ForeldrepengerPeriode(5.januar til 10.januar, 100),
            ForeldrepengerPeriode(11.januar til 31.januar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket sammenhengende i og før perioden`() {
        val vedtaksperiode = 5.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(
            ForeldrepengerPeriode(1.januar til 10.januar, 100),
            ForeldrepengerPeriode(11.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `har vedtaksperiode rett etter`() {
        val vedtaksperiode = 1.januar til 31.januar
        val forlengelse= 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode, forlengelse))
    }
}