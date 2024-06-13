package no.nav.helse.hendelser

import java.time.LocalDate
import no.nav.helse.februar
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AndreYtelserOppdatereHistorikkTest {

    private fun skalOppdatereHistorikk(
        ytelse: AnnenYtelseSomKanOppdatereHistorikk,
        periode: Periode,
        periodeRettEtter: Periode? = null,
        skjæringstidspunkt: LocalDate = periode.start,
    ): Boolean {
        return ytelse.skalOppdatereHistorikk(Aktivitetslogg(), ytelse, periode, skjæringstidspunkt, periodeRettEtter)
    }

    @Test
    fun `foreldrepenger fullstendig i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 31.januar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger før perioden`() {
        val vedtaksperiode = 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger før og i perioden`() {
        val vedtaksperiode = 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 28.februar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen av perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(20.januar til 31.januar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen av perioden, med skjæringstidspunkt midt i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(10.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode, skjæringstidspunkt = 15.januar))
    }

    @Test
    fun `foreldrepenger i halen av perioden med start lik skjæringstidspunkt`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(15.januar til 31.januar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode, skjæringstidspunkt = 15.januar))
    }

    @Test
    fun `foreldrepenger i snuten av perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 20.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i snuten og før perioden`() {
        val vedtaksperiode = 1.februar til 20.februar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 5.februar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger i halen og etter perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(20.januar til 10.februar, 100)))
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(
            listOf(
                GradertPeriode(1.januar til 10.januar, 100),
                GradertPeriode(20.januar til 31.januar, 100))
        )
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket sammenhengende i perioden`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(
            listOf(
                GradertPeriode(5.januar til 10.januar, 100),
                GradertPeriode(11.januar til 31.januar, 100))
        )
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `foreldrepenger oppstykket sammenhengende i og før perioden`() {
        val vedtaksperiode = 5.januar til 31.januar
        val foreldrepenger = Foreldrepenger(
            listOf(
                GradertPeriode(1.januar til 10.januar, 100),
                GradertPeriode(11.januar til 31.januar, 100))
        )
        assertTrue(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }

    @Test
    fun `har vedtaksperiode rett etter`() {
        val vedtaksperiode = 1.januar til 31.januar
        val forlengelse= 1.februar til 28.februar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 31.januar, 100)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode, forlengelse))
    }

    @Test
    fun `graderte foreldrepenger`() {
        val vedtaksperiode = 1.januar til 31.januar
        val foreldrepenger = Foreldrepenger(listOf(GradertPeriode(1.januar til 31.januar, 50)))
        assertFalse(skalOppdatereHistorikk(foreldrepenger, vedtaksperiode))
    }
}