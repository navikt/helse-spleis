package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.inspectors.inspektør
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.testhelpers.S
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SykdomshistorikkTest {
    private lateinit var historikk: Sykdomshistorikk

    @BeforeEach
    fun setup() {
        historikk = Sykdomshistorikk()
    }

    @Test
    fun `fjerner hel periode`() {
        val tidslinje = 10.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.fjernDager(tidslinje.periode()!!)
        assertEquals(2, historikk.inspektør.elementer())
        assertFalse(historikk.inspektør.tidslinje(0).iterator().hasNext())
        assertTrue(historikk.inspektør.tidslinje(1).iterator().hasNext())
    }

    @Test
    fun `fjerner del av periode`() {
        val tidslinje = 10.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.fjernDager(tidslinje.førsteDag() til tidslinje.sisteDag().minusDays(1))
        assertEquals(2, historikk.inspektør.elementer())
        assertEquals(1, historikk.inspektør.tidslinje(0).count())
    }

    @Test
    fun `tømmer historikk`() {
        historikk.håndter(Testhendelse(1.S))
        historikk.tøm()
        assertEquals(2, historikk.inspektør.elementer())
    }

    @Test
    fun `tømmer ikke tom historikk`() {
        historikk.tøm()
        assertEquals(0, historikk.inspektør.elementer())
    }

    @Test
    fun `tømmer ikke historikk når den er tom`() {
        val tidslinje = 10.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.fjernDager(tidslinje.periode()!!)
        historikk.tøm()
        assertEquals(2, historikk.inspektør.elementer())
        assertFalse(historikk.inspektør.tidslinje(0).iterator().hasNext())
        assertTrue(historikk.inspektør.tidslinje(1).iterator().hasNext())
    }

    private class Testhendelse(private val tidslinje: Sykdomstidslinje) : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
        override fun organisasjonsnummer() = ""
        override fun aktørId() = ""
        override fun fødselsnummer() = ""
        override fun sykdomstidslinje() = tidslinje
        override fun valider(periode: Periode) = this
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {}
    }
}
