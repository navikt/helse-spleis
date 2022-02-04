package no.nav.helse.sykdomstidslinje

import no.nav.helse.februar
import no.nav.helse.hendelser.til
import no.nav.helse.januar
import no.nav.helse.testhelpers.A
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.TestHendelse
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomstidslinjeHendelseTest {
    @BeforeEach
    fun setup() {
        resetSeed()
    }

    @Test
    fun kontekst() {
        assertEquals(setOf("meldingsreferanseId", "aktørId", "fødselsnummer", "organisasjonsnummer"), TestHendelse().toSpesifikkKontekst().kontekstMap.keys)
    }

    @Test
    fun oppdaterFom() {
        val hendelse = TestHendelse(31.S)
        assertEquals(1.januar til 10.januar, hendelse.oppdaterFom(5.januar til 10.januar))
        hendelse.trimLeft(10.januar)
        assertEquals(11.januar til 31.januar, hendelse.oppdaterFom(15.januar til 31.januar))
        hendelse.trimLeft(31.januar)
        assertEquals(1.februar til 5.februar, hendelse.oppdaterFom(1.februar til 5.februar))
    }

    @Test
    fun `oppdaterer ikke fom til arbeidsdager`() {
        val hendelse = TestHendelse(15.A + 16.S)
        assertEquals(1.januar til 10.januar, hendelse.oppdaterFom(5.januar til 10.januar))
        hendelse.trimLeft(10.januar)
        assertEquals(15.januar til 31.januar, hendelse.oppdaterFom(15.januar til 31.januar))
        hendelse.trimLeft(31.januar)
        assertEquals(1.februar til 5.februar, hendelse.oppdaterFom(1.februar til 5.februar))
    }
}
