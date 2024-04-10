package no.nav.helse.hendelser

import no.nav.helse.februar
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ForeldrepengerTest {

    @Test
    fun `foreldrepenger fullstendig i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 31.januar, 100)))
        assertTrue(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }

    @Test
    fun `foreldrepenger utenfor perioden`() {
        val vedtaksperiode = 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 31.januar, 100)))
        assertFalse(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen av perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(20.januar til 31.januar, 100)))
        assertTrue(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }
    @Test
    fun `foreldrepenger i snuten av perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 20.januar, 100)))
        assertFalse(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i snuten og før perioden`() {
        val vedtaksperiode = 1.februar til 20.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 5.februar, 100)))
        assertFalse(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen og etter perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(20.januar til 10.februar, 100)))
        assertTrue(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(
            ForeldrepengerPeriode(1.januar til 10.januar, 100),
            ForeldrepengerPeriode(20.januar til 31.januar, 100)))
        assertFalse(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode))
    }

    @Test
    fun `har vedtaksperiode rett etter`() {
        val vedtaksperiode= 1.januar til 31.januar
        val forlengelse= 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(ForeldrepengerPeriode(1.januar til 31.januar, 100)))
        assertFalse(foreldrepenger.skalOppdatereHistorikk(vedtaksperiode, forlengelse))
    }
}