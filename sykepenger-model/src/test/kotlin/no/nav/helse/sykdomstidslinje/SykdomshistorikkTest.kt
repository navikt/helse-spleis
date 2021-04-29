package no.nav.helse.sykdomstidslinje

import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.SykdomshistorikkVisitor
import no.nav.helse.testhelpers.S
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
