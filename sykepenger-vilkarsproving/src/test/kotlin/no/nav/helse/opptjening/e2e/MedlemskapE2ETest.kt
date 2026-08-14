package no.nav.helse.opptjening.e2e

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.MedlemskapService
import no.nav.helse.opptjening.infra.kafka.GrunnlagForMedlemskapsvurderingRiver
import no.nav.helse.opptjening.infra.kafka.MedlemskapsvurderingResultatRiver
import no.nav.helse.opptjening.infra.kafka.MedlemskapsvurderingRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * E2E-tester for medlemskapsflyten gjennom alle tre rivers. Speiler [OpptjeningE2ETest]:
 *
 *   1. MedlemskapsvurderingRiver mottar Medlemskapsvurdering-behov → sender Medlemskap-behov
 *   2. GrunnlagForMedlemskapsvurderingRiver mottar Medlemskap-løsning → fullfører vurdering og besvarer opprinneligBehov
 *   3. MedlemskapsvurderingResultatRiver mottar MedlemskapsvurderingResultat-behov → svarer med ok=true/false
 */
internal class MedlemskapE2ETest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val rapid = TestRapid().apply {
        val medlemskapService = MedlemskapService(repository)
        MedlemskapsvurderingRiver(this, medlemskapService)
        GrunnlagForMedlemskapsvurderingRiver(this, medlemskapService)
        MedlemskapsvurderingResultatRiver(this, repository)
    }

    @Test
    fun `medlem i folketrygden får ok=true`() {
        val behovId = UUID.randomUUID()

        // Steg 1: spleis ber om medlemskapsvurdering
        rapid.sendTestMessage(medlemskapsvurderingBehov(behovId), FØDSELSNUMMER)
        assertEquals(1, rapid.inspektør.size)

        val medlemskapsbehov = rapid.inspektør.message(0)
        assertEquals("behov", medlemskapsbehov.path("@event_name").asText())
        assertEquals(listOf("Medlemskap"), medlemskapsbehov.path("@behov").map { it.asText() })

        // Steg 2: medlemskapsoppslaget svarer JA
        rapid.sendTestMessage(medlemskapløsning(behovId, "JA"), FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)

        val løsning = rapid.inspektør.message(1)
        assertEquals(behovId.toString(), løsning.path("@id").asText())
        val vurderingId = UUID.fromString(løsning.path("@løsning").path("Medlemskapsvurdering").path("id").asText())

        // Steg 3: spleis ber om resultatet av vurderingen
        rapid.sendTestMessage(medlemskapsvurderingResultatBehov(vurderingId), FØDSELSNUMMER)
        assertEquals(3, rapid.inspektør.size)

        assertTrue(rapid.inspektør.message(2).path("@løsning").path("MedlemskapsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `ikke medlem i folketrygden får ok=false`() {
        val behovId = UUID.randomUUID()

        rapid.sendTestMessage(medlemskapsvurderingBehov(behovId), FØDSELSNUMMER)
        rapid.sendTestMessage(medlemskapløsning(behovId, "NEI"), FØDSELSNUMMER)

        val vurderingId = UUID.fromString(
            rapid.inspektør.message(1).path("@løsning").path("Medlemskapsvurdering").path("id").asText()
        )
        rapid.sendTestMessage(medlemskapsvurderingResultatBehov(vurderingId), FØDSELSNUMMER)

        assertFalse(rapid.inspektør.message(2).path("@løsning").path("MedlemskapsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `partisjonsnøkkel bevares gjennom hele flyten`() {
        val behovId = UUID.randomUUID()
        val partisjonsnøkkel = "12029240045"

        rapid.sendTestMessage(medlemskapsvurderingBehov(behovId), partisjonsnøkkel)
        assertEquals(partisjonsnøkkel, rapid.inspektør.key(0), "Medlemskap-behov skal ha riktig partisjonsnøkkel")

        rapid.sendTestMessage(medlemskapløsning(behovId, "JA"), partisjonsnøkkel)
        assertEquals(partisjonsnøkkel, rapid.inspektør.key(1), "Medlemskapsvurdering-løsning skal ha riktig partisjonsnøkkel")
    }

    @Test
    fun `duplikat medlemskapsvurdering-behov sender ikke nytt Medlemskap-behov`() {
        val behovId = UUID.randomUUID()

        rapid.sendTestMessage(medlemskapsvurderingBehov(behovId), FØDSELSNUMMER)
        rapid.sendTestMessage(medlemskapløsning(behovId, "JA"), FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)

        rapid.sendTestMessage(medlemskapsvurderingBehov(UUID.randomUUID()), FØDSELSNUMMER)
        assertEquals(3, rapid.inspektør.size)

        val tredjeUtgang = rapid.inspektør.message(2)
        assertTrue(tredjeUtgang.hasNonNull("@løsning")) { "Skal svare med løsning, ikke nytt behov" }
        assertTrue(tredjeUtgang.path("@løsning").hasNonNull("Medlemskapsvurdering")) { "Løsningen skal inneholde Medlemskapsvurdering" }
    }

    @Test
    fun `duplikat medlemskapløsning gir ikke dobbelt svar`() {
        val behovId = UUID.randomUUID()
        rapid.sendTestMessage(medlemskapsvurderingBehov(behovId), FØDSELSNUMMER)

        val løsning = medlemskapløsning(behovId, "JA")
        rapid.sendTestMessage(løsning, FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)

        rapid.sendTestMessage(løsning, FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)
    }

    // Vi svarer kun på JA/NEI; alt annet må avklares før det kan bli en vurdering
    @Test
    fun `uavklart medlemskap plukkes ikke opp`() {
        val behovId = UUID.randomUUID()
        rapid.sendTestMessage(medlemskapsvurderingBehov(behovId), FØDSELSNUMMER)

        rapid.sendTestMessage(medlemskapløsning(behovId, "UAVKLART_MED_BRUKERSPORSMAAL"), FØDSELSNUMMER)

        assertEquals(1, rapid.inspektør.size)
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        @Language("JSON")
        fun medlemskapsvurderingBehov(id: UUID) = """
        {
          "@event_name": "behov",
          "@id": "$id",
          "@behov": ["Medlemskapsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01"
        }
        """

        @Language("JSON")
        fun medlemskapløsning(behovId: UUID, svar: String, erFinal: Boolean = true) = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Medlemskap"],
          "@final": $erFinal,
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "medlemskapPeriodeFom": "2018-02-01",
          "medlemskapPeriodeTom": "2018-02-01",
          "opprinneligBehov": {
            "@event_name": "behov",
            "@id": "$behovId",
            "@behov": ["Medlemskapsvurdering"],
            "fødselsnummer": "$FØDSELSNUMMER",
            "skjæringstidspunkt": "2018-02-01"
          },
          "@løsning": {
            "Medlemskap": { "resultat": { "svar": "$svar" } }
          }
        }
        """

        @Language("JSON")
        fun medlemskapsvurderingResultatBehov(medlemskapsvurderingId: UUID) = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["MedlemskapsvurderingResultat"],
          "MedlemskapsvurderingResultat": {
            "medlemskapsvurderingId": "$medlemskapsvurderingId"
          }
        }
        """
    }
}
