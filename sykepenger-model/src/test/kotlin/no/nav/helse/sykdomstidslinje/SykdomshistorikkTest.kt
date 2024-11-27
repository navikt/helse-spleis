package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.januar
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.TestHendelse
import no.nav.helse.testhelpers.U
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class SykdomshistorikkTest {
    private lateinit var historikk: Sykdomshistorikk
    private val Sykdomshistorikk.inspektør get() = historikk.view().inspektør

    @BeforeEach
    fun setup() {
        historikk = Sykdomshistorikk()
        resetSeed()
    }

    @Test
    fun `fjerner ingenting`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje), Aktivitetslogg())
        historikk.fjernDager(emptyList())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals(tidslinje, historikk.sykdomstidslinje())
    }

    @Test
    fun `fjerner hel periode`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje), Aktivitetslogg())
        historikk.fjernDager(listOf(tidslinje.periode()!!))
        assertEquals(2, historikk.inspektør.elementer())
        assertFalse(
            historikk.inspektør
                .tidslinje(0)
                .iterator()
                .hasNext(),
        )
        assertTrue(
            historikk.inspektør
                .tidslinje(1)
                .iterator()
                .hasNext(),
        )
    }

    @Test
    fun `fjerner del av periode`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje), Aktivitetslogg())
        historikk.fjernDager(listOf(tidslinje.førsteDag() til tidslinje.sisteDag().minusDays(1)))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(1, historikk.inspektør.tidslinje(0).count())
    }

    @Test
    fun `fjerner flere perioder`() {
        val tidslinje = 10.S
        historikk.håndter(TestHendelse(tidslinje), Aktivitetslogg())
        historikk.fjernDager(listOf(1.januar til 2.januar, 5.januar til 10.januar))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(3.januar til 4.januar, historikk.sykdomstidslinje().periode())
    }

    @Test
    fun `håndterer kun hendelser èn gang`() {
        val tidslinje = 10.S
        val hendelse = TestHendelse(tidslinje)
        historikk.håndter(hendelse, Aktivitetslogg())
        assertEquals(1, historikk.inspektør.elementer())
        historikk.håndter(hendelse, Aktivitetslogg())
        assertEquals(1, historikk.inspektør.elementer())
    }

    @Test
    fun `hendelser skal kunne håndteres i biter`() {
        val bit1 = TestHendelse(8.U)
        val bit2 = TestHendelse(8.U)
        historikk.håndter(bit1, Aktivitetslogg())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals("UUUUUGG U", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(bit2, Aktivitetslogg())
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("UUUUUGG UUUUUGG UU", historikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Overlappende biter`() {
        val meldingsreferanseId = UUID.randomUUID()
        val bit1 = TestHendelse(8.U(meldingsreferanseId = meldingsreferanseId))
        resetSeed()
        val bit2 = TestHendelse(16.U(meldingsreferanseId = meldingsreferanseId))
        historikk.håndter(bit1, Aktivitetslogg())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals("UUUUUGG U", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(bit2, Aktivitetslogg())
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("UUUUUGG UUUUUGG UU", historikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `Hele hendelsen er håndtert for en hendelse siden`() {
        val søknad = TestHendelse(10.S)
        val heleBiten = TestHendelse(16.U)
        historikk.håndter(heleBiten, Aktivitetslogg())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals("UUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(søknad, Aktivitetslogg())
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(heleBiten, Aktivitetslogg())
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
    }

    @Test
    fun `En bit av inntektsmeldingen håndteres før annen hendelse, og den andre biten etterpå`() {
        val søknad = TestHendelse(10.S)
        val bit1 = TestHendelse(8.U)
        val bit2 = TestHendelse(8.U)
        historikk.håndter(bit1, Aktivitetslogg())
        assertEquals(1, historikk.inspektør.elementer())
        assertEquals("UUGG UUUU", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(søknad, Aktivitetslogg())
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUU", historikk.sykdomstidslinje().toShortString())
        historikk.håndter(bit2, Aktivitetslogg())
        assertEquals(3, historikk.inspektør.elementer())
        assertEquals("SSSSSHH SSSUUGG UUUUUGG UUUUU", historikk.sykdomstidslinje().toShortString())
    }
}
