package no.nav.helse.spleis.mediator

import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_1
import no.nav.helse.spleis.DatadelingMediator
import no.nav.helse.spleis.mediator.meldinger.TestRapid
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DatadelingMediatorTest {
    private val fødselsnummer = "12345678910"
    private val testRapid = TestRapid()
    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var datadelingMediator: DatadelingMediator

    @BeforeEach
    fun beforeEach() {
        aktivitetslogg = Aktivitetslogg()
        datadelingMediator = DatadelingMediator(aktivitetslogg, UUID.randomUUID(), fødselsnummer, "aktørId")
    }

    @Test
    fun `datadelingMediator fanger opp nye aktiviteter på hendelsen`() {
        aktivitetslogg.varsel(RV_SØ_1)
        datadelingMediator.ferdigstill(testRapid)
        assertEquals(1, testRapid.inspektør.antall())
        assertNotNull(testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet"))
    }

    @Test
    fun innhold() {
        aktivitetslogg.kontekst(TestKontekst("Person", "Person 1"))
        aktivitetslogg.info("Dette er en infomelding")
        datadelingMediator.ferdigstill(testRapid)
        assertEquals(1, testRapid.inspektør.antall())

        val info = testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet")["aktiviteter"][0]
        assertEquals("INFO", info["nivå"].asText())
        assertEquals("Dette er en infomelding", info["melding"].asText())
        assertNotNull(info["tidsstempel"].asText())
        assertDoesNotThrow {
            LocalDateTime.parse(info["tidsstempel"].asText())
        }
        assertEquals("Person", info["kontekster"][0]["konteksttype"].asText())
    }

    @Test
    fun nivåer() {
        aktivitetslogg.info("Dette er en infomelding")
        aktivitetslogg.varsel(RV_SØ_1)
        aktivitetslogg.funksjonellFeil(RV_VT_1)
        try {
            aktivitetslogg.logiskFeil("Dette er en severe")
        } catch (_: Exception) {}
        datadelingMediator.ferdigstill(testRapid)

        assertEquals(1, testRapid.inspektør.antall())
        val aktiviteter = testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet")["aktiviteter"]
        assertEquals(4, aktiviteter.size())
    }

    @Test
    fun `publiserer behov-aktiviteter`() {
        aktivitetslogg.behov(Aktivitet.Behov.Behovtype.Godkjenning, "melding")
        datadelingMediator.ferdigstill(testRapid)
        assertEquals(1, testRapid.inspektør.antall())
    }

    @Test
    fun mapping() {
        aktivitetslogg.kontekst(TestKontekst("Person", "Person 1"))
        aktivitetslogg.info("Dette er en infomelding")
        aktivitetslogg.varsel(RV_SØ_1)
        aktivitetslogg.funksjonellFeil(RV_VT_1)
        try { aktivitetslogg.logiskFeil("Dette er en infomelding") } catch (_: Exception) {}
        datadelingMediator.ferdigstill(testRapid)

        val info = testRapid.inspektør.siste("aktivitetslogg_ny_aktivitet")["aktiviteter"]
        assertEquals("INFO", info[0]["nivå"].asText())
        assertEquals("VARSEL", info[1]["nivå"].asText())
        assertEquals("FUNKSJONELL_FEIL", info[2]["nivå"].asText())
        assertEquals("LOGISK_FEIL", info[3]["nivå"].asText())
    }

    private class TestKontekst(
        private val type: String,
        private val melding: String
    ): Aktivitetskontekst {
        override fun toSpesifikkKontekst() = SpesifikkKontekst(type, mapOf(type to melding))
    }
}