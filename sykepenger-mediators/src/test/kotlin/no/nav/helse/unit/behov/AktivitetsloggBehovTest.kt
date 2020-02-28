package no.nav.helse.unit.behov

import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.Aktivitetslogg
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
        person = TestKontekst("Person")
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    internal fun `kan hente ut behov`(){
        val hendelse1 = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse1.kontekst(person)
        val arbeidsgiver1 = TestKontekst("Arbeidsgiver 1")
        hendelse1.kontekst(arbeidsgiver1)
        val vedtaksperiode1 = TestKontekst("Vedtaksperiode 1")
        hendelse1.kontekst(vedtaksperiode1)
        hendelse1.behov("Godkjenning")
        hendelse1.warn("Advarsel")
        val hendelse2 = TestHendelse("Hendelse2", aktivitetslogg.barn())
        hendelse2.kontekst(person)
        val arbeidsgiver2 = TestKontekst("Arbeidsgiver 2")
        hendelse2.kontekst(arbeidsgiver2)
        hendelse2.behov("Utbetaling")
        hendelse2.info("Infomelding")

        assertEquals(2, aktivitetslogg.behov().size)
        assertEquals(3, aktivitetslogg.behov().first().kontekst().size)
        assertEquals(2, aktivitetslogg.behov().last().kontekst().size)
    }

    private class TestKontekst(
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(melding, mapOf(melding to melding))
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
