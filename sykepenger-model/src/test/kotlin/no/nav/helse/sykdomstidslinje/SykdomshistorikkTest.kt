package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.TestHendelse
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SykdomshistorikkTest {
    private lateinit var historikk: Sykdomshistorikk

    @BeforeEach
    fun setup() {
        historikk = Sykdomshistorikk()
        resetSeed()
    }

    @Test
    fun `fjerner ingenting`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(emptyList())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals(tidslinje, historikk.sykdomstidslinje())
    }

    @Test
    fun `fjerner hel periode`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(listOf(tidslinje.periode()!!))
        assertEquals(2, historikk.inspektør.elementer())
        assertFalse(historikk.inspektør.tidslinje(0).iterator().hasNext())
        assertTrue(historikk.inspektør.tidslinje(1).iterator().hasNext())
    }

    @Test
    fun `fjerner del av periode`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(listOf(tidslinje.førsteDag() til tidslinje.sisteDag().minusDays(1)))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(1, historikk.inspektør.tidslinje(0).count())
    }

    @Test
    fun `fjerner flere perioder`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje))
        historikk.fjernDager(listOf(1.januar til 2.januar, 5.januar til 10.januar))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(3.januar til 4.januar, historikk.sykdomstidslinje().periode())
    }
}
