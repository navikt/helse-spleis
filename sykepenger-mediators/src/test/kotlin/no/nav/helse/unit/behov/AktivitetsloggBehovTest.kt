package no.nav.helse.unit.behov

import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Behovtype.Utbetaling
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.SpesifikkKontekst
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
    internal fun `kan hente ut behov`(){
        val hendelse1 = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse1.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode", "Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.behov(Godkjenning, "Trenger godkjenning")
        hendelse1.warn("Advarsel")
        val hendelse2 = TestHendelse("Hendelse2", aktivitetslogg.barn())
        hendelse2.kontekst(person)
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver", "Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        val tilstand = TestKontekst("Tilstand", "Tilstand 1")
        hendelse2.kontekst(tilstand)
        hendelse2.behov(Utbetaling, "Skal utbetale")
        hendelse2.info("Infomelding")

        assertEquals(2, aktivitetslogg.behov().size)
        assertEquals(3, aktivitetslogg.behov().first().kontekst().size)
        assertEquals(3, aktivitetslogg.behov().last().kontekst().size)
        assertEquals("Tilstand 1", aktivitetslogg.behov().last().kontekst()["Tilstand"])
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }

    private class TestHendelse(
        private val melding: String,
        internal val logg: Aktivitetslogg
    ): Aktivitetskontekst, IAktivitetslogg by logg {
        init {
            logg.kontekst(this)
        }
        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")
        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }
    }
}
