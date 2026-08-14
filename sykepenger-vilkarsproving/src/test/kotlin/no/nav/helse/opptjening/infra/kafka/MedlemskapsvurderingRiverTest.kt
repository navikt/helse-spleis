package no.nav.helse.opptjening.infra.kafka

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.opptjening.application.InMemoryVilkårsprøvingRepository
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.MedlemskapService
import no.nav.helse.opptjening.domain.Grunnlagsbehov
import no.nav.helse.opptjening.domain.Medlemskapsgrunnlag
import no.nav.helse.opptjening.domain.Medlemskapsprøving
import no.nav.helse.opptjening.domain.Medlemskapssvar
import no.nav.helse.opptjening.domain.VurderingId
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MedlemskapsvurderingRiverTest {

    private val vurderinger = InMemoryVilkårsvurderingRepository()
    private val prøvinger = InMemoryVilkårsprøvingRepository()
    private val rapid = TestRapid().apply {
        MedlemskapsvurderingRiver(this, MedlemskapService(vurderinger, prøvinger))
    }

    // Medlemskap må alltid slås opp, så riveren sender ut et nytt behov i stedet for en løsning
    @Test
    fun `behovet gir behov om medlemskap`() {
        rapid.sendTestMessage(medlemskapsvurderingBehov(), "123")

        assertEquals(1, rapid.inspektør.size)
        val utgående = rapid.inspektør.message(0)
        assertEquals("123", rapid.inspektør.key(0))
        assertEquals("behov", utgående.path("@event_name").asText())
        assertEquals(listOf("Medlemskap"), utgående.path("@behov").map { it.asText() })
        assertEquals(FØDSELSNUMMER, utgående.path("fødselsnummer").asText())
        assertEquals("2018-02-01", utgående.path("skjæringstidspunkt").asText())
        assertTrue(utgående.hasNonNull("opprinneligBehov"))
    }

    // Prøvingen er prosessen som venter på grunnlaget; ingen vurdering finnes ennå
    @Test
    fun `det startes en prøving uten å produsere en vurdering`() {
        rapid.sendTestMessage(medlemskapsvurderingBehov())

        val prøving = prøvinger.alleProvinger.single()
        assertFalse(prøving.erAvsluttet)
        assertEquals(Grunnlagsbehov.Medlemskap, prøving.uteståendeBehov)
        assertEquals(0, vurderinger.antallLagringer)
    }

    @Test
    fun `eksisterende vurdering svares ut direkte`() {
        val eksisterende = fullførtPrøving()

        rapid.sendTestMessage(medlemskapsvurderingBehov())

        assertEquals(1, rapid.inspektør.size)
        val løsning = rapid.inspektør.message(0)
        assertEquals(eksisterende.toString(), løsning.path("@løsning").path("Medlemskapsvurdering").path("id").asText())
    }

    @Test
    fun `opprinneligBehov er json-objekt med identisk innhold som innkommende behov`() {
        val innkommendeBehov = medlemskapsvurderingBehov()
        rapid.sendTestMessage(innkommendeBehov)

        val opprinneligBehov = rapid.inspektør.message(0).path("opprinneligBehov")
        assertInstanceOf(ObjectNode::class.java, opprinneligBehov) { "opprinneligBehov skal være et JSON-objekt, ikke en string" }

        val forventet = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().readTree(innkommendeBehov)
        forventet.fields().forEach { (key, value) ->
            assertEquals(value, opprinneligBehov.get(key)) { "Feltet '$key' i opprinneligBehov stemmer ikke" }
        }
    }

    @Test
    fun `behov med løsning ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Medlemskapsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "@løsning": { "Medlemskapsvurdering": { "id": "${UUID.randomUUID()}" } }
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `andre behov ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["EtHeltAnnetBehov"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01"
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `behov uten skjæringstidspunkt ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Medlemskapsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER"
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    private fun fullførtPrøving(): VurderingId {
        val prøving = Medlemskapsprøving.start(FØDSELSNUMMER, 1.februar).prøving
        val vurdering = prøving.motta(Medlemskapsgrunnlag(Medlemskapssvar.Ja))
        prøvinger.opprett(prøving)
        vurderinger.lagre(vurdering)
        return vurdering.id
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        @Language("JSON")
        fun medlemskapsvurderingBehov() = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Medlemskapsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01"
        }
        """
    }
}
