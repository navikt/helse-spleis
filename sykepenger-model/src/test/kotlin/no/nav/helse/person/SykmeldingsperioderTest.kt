package no.nav.helse.person

import no.nav.helse.februar
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class SykmeldingsperioderTest() {

    @Test
    fun `Kan lagre Sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertEquals(listOf(1.januar til 31.januar), sykmeldingsperioder.perioder())
    }

    @Disabled
    @Test
    fun `utvider ikke perioder ved duplikate sykmeldingsperioder`() {
        val sykmeldingsperioder = Sykmeldingsperioder()
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        sykmeldingsperioder.lagre(1.januar til 31.januar)
        assertEquals(listOf(1.januar til 31.januar), sykmeldingsperioder.perioder())
    }

    @Disabled
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

    class Inspektør() : Sykmeldingsperioder.Visitor {

        val perioder = mutableListOf<Periode>()

        override fun visitSykmeldingsperiode(periode: Periode) {
            perioder.add(periode)
        }
    }

    fun Sykmeldingsperioder.perioder() = Inspektør().also(::accept).perioder
}
