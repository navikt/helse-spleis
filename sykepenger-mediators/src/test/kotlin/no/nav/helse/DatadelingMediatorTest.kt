package no.nav.helse

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.Aktivitetskontekst
import no.nav.helse.person.Aktivitetslogg
import no.nav.helse.person.PersonHendelse
import no.nav.helse.person.SpesifikkKontekst
import no.nav.helse.spleis.DatadelingMediator
import no.nav.helse.spleis.meldinger.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DatadelingMediatorTest {
    private val fødselsnummer = "12345678910"
    private val testRapid = TestRapid()
    private lateinit var testhendelse: TestHendelse
    private lateinit var datadelingMediator: DatadelingMediator

    @BeforeEach
    fun beforeEach() {
        testhendelse = TestHendelse(fødselsnummer)
        datadelingMediator = DatadelingMediator(testhendelse)
    }

    @Test
    fun `datadelingMediator fanger opp nye aktiviteter på hendelsen`() {
        testhendelse.varsel("Dette er et varsel")
        datadelingMediator.finalize(testRapid)
        assertEquals(1, testRapid.inspektør.antall())
        assertNotNull(testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet"))
    }

    @Test
    fun innhold() {
        testhendelse.kontekst(TestKontekst("Person", "Person 1"))
        testhendelse.info("Dette er en infomelding")
        datadelingMediator.finalize(testRapid)
        assertEquals(1, testRapid.inspektør.antall())

        val info = testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet")["aktiviteter"][0]
        assertEquals("Info", info["nivå"].asText())
        assertEquals("Dette er en infomelding", info["melding"].asText())
        assertNotNull(info["tidsstempel"].asText())
        assertDoesNotThrow {
            LocalDateTime.parse(info["tidsstempel"].asText())
        }
        assertEquals("TestHendelse", info["kontekster"][0]["konteksttype"].asText())
        assertEquals("Person", info["kontekster"][1]["konteksttype"].asText())
    }

    @Test
    fun nivåer() {
        testhendelse.info("Dette er en infomelding")
        testhendelse.varsel("Dette er et varsel")
        testhendelse.funksjonellFeil("Dette er en error")
        try {
            testhendelse.logiskFeil("Dette er en severe")
        } catch (_: Exception) {}
        datadelingMediator.finalize(testRapid)

        assertEquals(1, testRapid.inspektør.antall())
        val aktiviteter = testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet")["aktiviteter"]
        assertEquals(4, aktiviteter.size())
    }

    @Test
    fun `publiserer ikke behov-aktiviteter`() {
        testhendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Godkjenning, "melding")
        datadelingMediator.finalize(testRapid)
        assertEquals(0, testRapid.inspektør.antall())
    }

    private class TestHendelse(fødselsnummer: String) : PersonHendelse(UUID.randomUUID(), fødselsnummer, "1234567891011", Aktivitetslogg())

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }
}