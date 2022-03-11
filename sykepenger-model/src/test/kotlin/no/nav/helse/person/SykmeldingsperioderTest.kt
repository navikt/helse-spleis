package no.nav.helse.person

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SykmeldingsperioderTest() {

    @Test
    fun `Kan lagre Sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertEquals(listOf(1.januar til 31.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `utvider ikke perioder ved duplikate sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertEquals(listOf(1.januar til 31.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `utvider periode ved overlappende sykmeldingsperioder, lager ikke ny periode`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(20.januar til 20.februar)
        assertEquals(listOf(1.januar til 20.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `gap fører til to sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(20.februar til 28.februar)
        assertEquals(listOf(1.januar til 31.januar, 20.februar til 28.februar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `to perioder med gap, kommer en periode i mellom som overlapper med begge`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 10.januar)
        sykmeldingsperioder.lagre(15.januar til 25.januar)
        assertEquals(listOf(1.januar til 10.januar, 15.januar til 25.januar), sykmeldingsperioder.perioder())

        sykmeldingsperioder.lagre(9.januar til 15.januar)
        assertEquals(listOf(1.januar til 25.januar), sykmeldingsperioder.perioder())
    }

    @Test
    fun `sykmeldingsperioder lagres i riktig rekkefølge`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(15.januar til 25.januar)
        sykmeldingsperioder.lagre(5.januar til 10.januar)
        assertEquals(listOf(5.januar til 10.januar, 15.januar til 25.januar), sykmeldingsperioder.perioder())
    }
    class Inspektør() : SykmeldingsperioderVisitor {

        val perioder = mutableListOf<Periode>()

        override fun visitSykmeldingsperiode(periode: Periode) {
            perioder.add(periode)
        }
    }

    fun Sykmeldingsperioder.perioder() = Inspektør().also(::accept).perioder
}
