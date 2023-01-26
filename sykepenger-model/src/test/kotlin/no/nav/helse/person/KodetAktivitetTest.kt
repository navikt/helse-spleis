package no.nav.helse.person

import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KodetAktivitetTest {


    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var person: TestKontekst

    @BeforeEach
    internal fun setUp() {
        person = TestKontekst("Person", "Person 1")
        aktivitetslogg = Aktivitetslogg()
    }

    @Test
    fun `kodede aktiveter`() {
        val hendelse1 = TestHendelse(aktivitetslogg.barn())
        hendelse1.kontekst(person)
        hendelse1.varsel(Varselkode.RV_SØ_1)
        Assertions.assertTrue(
            aktivitetslogg.harVarslerEllerVerre()
        ) { "Expected $aktivitetslogg to contain varsel" }
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }

    private class TestHendelse(val logg: Aktivitetslogg): Aktivitetskontekst, IAktivitetslogg by logg {
        init {
            logg.kontekst(this)
        }
        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")
        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }

    }
}