package no.nav.helse.person

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

    class Inspektør() : Sykmeldingsperioder.Visitor {

        val perioder = mutableListOf<Periode>()

        override fun visitSykmeldingsperiode(periode: Periode) {
            perioder.add(periode)
        }
    }

    fun Sykmeldingsperioder.perioder() = Inspektør().also(::accept).perioder
}
