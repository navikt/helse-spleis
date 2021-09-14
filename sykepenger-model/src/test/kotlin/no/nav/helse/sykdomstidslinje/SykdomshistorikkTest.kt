package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.testhelpers.S
import no.nav.helse.testhelpers.januar
import no.nav.helse.testhelpers.resetSeed
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*

internal class SykdomshistorikkTest {
    private lateinit var historikk: Sykdomshistorikk
    private val inspektør get() = SykdomshistorikkInspektør(historikk)
    @BeforeEach
    fun setup() {
        historikk = Sykdomshistorikk()
        resetSeed()
    }

    @Test
    fun `fjerner hel periode`() {
        val tidslinje = 10.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.fjernDager(tidslinje.periode()!!)
        assertEquals(2, inspektør.elementer())
        assertFalse(inspektør.tidslinje(0).iterator().hasNext())
        assertTrue(inspektør.tidslinje(1).iterator().hasNext())
    }

    @Test
    fun `fjerner del av periode`() {
        val tidslinje = 10.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.fjernDager(tidslinje.førsteDag() til tidslinje.sisteDag().minusDays(1))
        assertEquals(2, inspektør.elementer())
        assertEquals(1, inspektør.tidslinje(0).count())
    }

    @Test
    fun `tømmer historikk`() {
        historikk.håndter(Testhendelse(1.S))
        historikk.tøm()
        assertEquals(2, inspektør.elementer())
    }

    @Test
    fun `tømmer ikke tom historikk`() {
        historikk.tøm()
        assertEquals(0, inspektør.elementer())
    }

    @Test
    fun `tømmer ikke historikk når den er tom`() {
        val tidslinje = 10.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.fjernDager(tidslinje.periode()!!)
        historikk.tøm()
        assertEquals(2, inspektør.elementer())
        assertFalse(inspektør.tidslinje(0).iterator().hasNext())
        assertTrue(inspektør.tidslinje(1).iterator().hasNext())
    }

    @Test
    fun `kopierer låser - trimmet bort dager som ikke er låst`() {
        val tidslinje = 24.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.sykdomstidslinje().lås(1.januar til 10.januar)
        historikk.sykdomstidslinje().lås(11.januar til 20.januar)
        historikk.fjernDager(21.januar til 23.januar)

        assertTrue(historikk.sykdomstidslinje().erLåst(1.januar til 10.januar))
        assertTrue(historikk.sykdomstidslinje().erLåst(11.januar til 20.januar))
    }

    @Test
    fun `kopierer låser - trimmer bort deler av lås`() {
        val tidslinje = 24.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.sykdomstidslinje().lås(1.januar til 10.januar)
        historikk.sykdomstidslinje().lås(11.januar til 20.januar)
        historikk.fjernDager(15.januar til 24.januar)

        assertTrue(historikk.sykdomstidslinje().erLåst(1.januar til 10.januar))
        assertTrue(historikk.sykdomstidslinje().erLåst(11.januar til 14.januar))
    }

    @Test
    fun `kopierer låser - trimmer bort over 2 låser`() {
        val tidslinje = 24.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.sykdomstidslinje().lås(1.januar til 10.januar)
        historikk.sykdomstidslinje().lås(11.januar til 20.januar)
        historikk.sykdomstidslinje().lås(21.januar til 24.januar)
        historikk.fjernDager(18.januar til 24.januar)

        assertTrue(historikk.sykdomstidslinje().erLåst(1.januar til 10.januar))
        assertTrue(historikk.sykdomstidslinje().erLåst(11.januar til 17.januar))
    }

    @Test
    fun `kopierer låser - trimmer bort låser fra starten av tidslinjen`() {
        val tidslinje = 24.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.sykdomstidslinje().lås(1.januar til 10.januar)
        historikk.sykdomstidslinje().lås(11.januar til 20.januar)
        historikk.fjernDager(1.januar til 5.januar)

        assertTrue(historikk.sykdomstidslinje().erLåst(6.januar til 10.januar))
        assertTrue(historikk.sykdomstidslinje().erLåst(11.januar til 20.januar))
    }

    @Test
    fun `kopierer låser - trim bort hele perioden`() {
        val tidslinje = 24.S
        historikk.håndter(Testhendelse(tidslinje))
        historikk.sykdomstidslinje().lås(1.januar til 10.januar)
        historikk.sykdomstidslinje().lås(11.januar til 20.januar)
        historikk.fjernDager(1.januar til 24.januar)

        assertFalse(historikk.sykdomstidslinje().erLåst(1.januar til 10.januar))
        assertFalse(historikk.sykdomstidslinje().erLåst(11.januar til 20.januar))
        assertFalse(historikk.sykdomstidslinje().harSykedager())
    }

    private class Testhendelse(private val tidslinje: Sykdomstidslinje) : SykdomstidslinjeHendelse(UUID.randomUUID(), LocalDateTime.now()) {
        override fun organisasjonsnummer() = ""
        override fun aktørId() = ""
        override fun fødselsnummer() = ""
        override fun sykdomstidslinje() = tidslinje
        override fun valider(periode: Periode) = this
        override fun fortsettÅBehandle(arbeidsgiver: Arbeidsgiver) {}
    }

    private class SykdomshistorikkInspektør(historikk: Sykdomshistorikk) : SykdomshistorikkVisitor {
        private var elementteller = 0
        private val tidslinjer = mutableListOf<Sykdomstidslinje>()

        init {
            historikk.accept(this)
        }

        fun elementer() = elementteller
        fun tidslinje(elementIndeks: Int) = tidslinjer[elementIndeks]

        override fun postVisitSykdomshistorikkElement(element: Sykdomshistorikk.Element, id: UUID, hendelseId: UUID?, tidsstempel: LocalDateTime) {
            elementteller += 1
        }

        override fun preVisitBeregnetSykdomstidslinje(tidslinje: Sykdomstidslinje) {
            tidslinjer.add(elementteller, tidslinje)
        }
    }
}
