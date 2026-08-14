package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.domain.Kodeverkkode.OPPTJENING_MINST_4_UKER
import no.nav.helse.opptjening.domain.Opptjening.AutomatiskVurdering
import org.intellij.lang.annotations.Language
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OpptjeningsvurderingRiverTest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val rapid = TestRapid().apply {
        OpptjeningsvurderingRiver(this, OpptjeningService(repository))
    }

    // For arbeidstakere må vi hente arbeidsforhold før vi kan vurdere, så riveren
    // sender ut et nytt behov i stedet for en løsning
    @Test
    fun `arbeidstaker gir behov om arbeidsforhold`() {
        rapid.sendTestMessage(opptjeningsvurderingBehov(arbeidssituasjon = "Arbeidstaker"), "123")

        assertEquals(1, rapid.inspektør.size)
        val utgående = rapid.inspektør.message(0)
        val partisjonsnøkkelForUtgåendeMelding = rapid.inspektør.key(0)
        assertEquals("123", partisjonsnøkkelForUtgåendeMelding)
        assertEquals("behov", utgående.path("@event_name").asText())
        assertEquals(listOf("ArbeidsforholdV2"), utgående.path("@behov").map { it.asText() })
        assertEquals(FØDSELSNUMMER, utgående.path("fødselsnummer").asText())
        assertEquals("2018-02-01", utgående.path("skjæringstidspunkt").asText())
        assertTrue(utgående.hasNonNull("opprinneligBehov"))
    }

    // Selvstendig næringsdrivende kan vurderes med en gang, og løsningen legges på
    // det innkommende behovet
    @Test
    fun `selvstendig næringsdrivende løses umiddelbart`() {
        rapid.sendTestMessage(opptjeningsvurderingBehov(arbeidssituasjon = "SelvstendigNæringsdrivende"))

        assertEquals(1, rapid.inspektør.size)
        val løsning = rapid.inspektør.message(0)
        val vurderingId = UUID.fromString(løsning.path("@løsning").path("Opptjeningsvurdering").path("id").asText())

        val vurdering = repository.finn<AutomatiskVurdering>(vurderingId)!!
        assertTrue(vurdering.erKomplett)
        assertEquals(OPPTJENING_MINST_4_UKER, vurdering.kodeverkkode)
    }

    // Har vi allerede en komplett vurdering svarer vi med den eksisterende id-en
    // i stedet for å be om arbeidsforhold på nytt
    @Test
    fun `eksisterende vurdering svares ut direkte`() {
        val eksisterende = AutomatiskVurdering.nyAutomatiskVurdering(FØDSELSNUMMER, 1.februar, "")
            .also { it.fullfør(AutomatiskVurdering.OpptjeningsgrunnlagForAutomatiskVurdering.ForSelvstendigNæringsdrivende) }
        repository.lagre(eksisterende)

        rapid.sendTestMessage(opptjeningsvurderingBehov(arbeidssituasjon = "Arbeidstaker"))

        assertEquals(1, rapid.inspektør.size)
        val løsning = rapid.inspektør.message(0)
        assertEquals(eksisterende.id.toString(), løsning.path("@løsning").path("Opptjeningsvurdering").path("id").asText())
    }

    @Test
    fun `opprinneligBehov er json-objekt med identisk innhold som innkommende behov`() {
        val innkommendeBehov = opptjeningsvurderingBehov(arbeidssituasjon = "Arbeidstaker")
        rapid.sendTestMessage(innkommendeBehov)

        val utgående = rapid.inspektør.message(0)
        val opprinneligBehov = utgående.path("opprinneligBehov")

        assertInstanceOf(ObjectNode::class.java, opprinneligBehov) { "opprinneligBehov skal være et JSON-objekt, ikke en string" }

        val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        val forventet = objectMapper.readTree(innkommendeBehov)
        // Rapids beriker meldingen med metadata-felter — vi sjekker at alle opprinnelige felter er bevart
        forventet.fields().forEach { (key, value) ->
            assertEquals(value, opprinneligBehov.get(key)) { "Feltet '$key' i opprinneligBehov stemmer ikke" }
        }
    }

    // Riveren skal ikke plukke opp behov som allerede har fått en løsning
    @Test
    fun `behov med løsning ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Opptjeningsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "arbeidssituasjon": "Arbeidstaker",
          "@løsning": { "Opptjeningsvurdering": { "id": "${UUID.randomUUID()}" } }
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
          "skjæringstidspunkt": "2018-02-01",
          "arbeidssituasjon": "Arbeidstaker"
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    // Mangler påkrevde felter skal meldingen ikke behandles
    @Test
    fun `behov uten skjæringstidspunkt ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Opptjeningsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "arbeidssituasjon": "Arbeidstaker"
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        @Language("JSON")
        fun opptjeningsvurderingBehov(arbeidssituasjon: String) = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Opptjeningsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "arbeidssituasjon": "$arbeidssituasjon"
        }
        """
    }
}
