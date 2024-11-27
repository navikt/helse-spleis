package no.nav.helse.behov

import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AktivitetsloggBehovTest {
    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var person: TestKontekst

    @BeforeEach
    internal fun setUp() {
        person = TestKontekst("Person", "Person")
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `kan hente ut behov`() {
        val hendelse1 = aktivitetslogg.barn()
        hendelse1.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.behov(Godkjenning, "Trenger godkjenning")

        val hendelse2 = aktivitetslogg.barn()
        hendelse2.kontekst(person)
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        val tilstand = TestKontekst("Tilstand", "Tilstand 1")
        hendelse2.kontekst(tilstand)
        hendelse2.behov(Utbetaling, "Skal utbetale")

        assertEquals(2, aktivitetslogg.behov.size)
        assertEquals(
            3,
            aktivitetslogg.behov
                .first()
                .kontekst()
                .size,
        )
        assertEquals(
            3,
            aktivitetslogg.behov
                .last()
                .kontekst()
                .size,
        )
        assertEquals("Tilstand 1", aktivitetslogg.behov.last().kontekst()["Tilstand"])
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String,
    ) : Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }
}
