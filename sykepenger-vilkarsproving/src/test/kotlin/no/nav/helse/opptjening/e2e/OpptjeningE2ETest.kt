package no.nav.helse.opptjening.e2e

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.OpptjeningService
import no.nav.helse.opptjening.infra.kafka.GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver
import no.nav.helse.opptjening.infra.kafka.OpptjeningsvurderingResultatRiver
import no.nav.helse.opptjening.infra.kafka.OpptjeningsvurderingRiver
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * E2E-tester for opptjeningsflyten gjennom alle tre rivers.
 *
 * Flyt for arbeidstaker:
 *   1. OpptjeningsvurderingRiver mottar Opptjeningsvurdering-behov → sender ArbeidsforholdV2-behov
 *   2. GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver mottar ArbeidsforholdV2-løsning → fullfører vurdering og besvarer opprinneligBehov
 *   3. OpptjeningsvurderingResultatRiver mottar OpptjeningsvurderingResultat-behov → svarer med ok=true/false
 *
 * Flyt for selvstendig næringsdrivende:
 *   1. OpptjeningsvurderingRiver mottar Opptjeningsvurdering-behov → besvarer direkte
 *   2. OpptjeningsvurderingResultatRiver mottar OpptjeningsvurderingResultat-behov → svarer med ok=true
 */
internal class OpptjeningE2ETest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val rapid = TestRapid().apply {
        val opptjeningService = OpptjeningService(repository)
        OpptjeningsvurderingRiver(this, opptjeningService)
        GrunnlagForAutomatiskArbeidstakerOpptjeningsvurderingRiver(this, opptjeningService)
        OpptjeningsvurderingResultatRiver(this, repository)
    }

    // === Arbeidstaker-flyt ===

    @Test
    fun `arbeidstaker med nok opptjening får ok=true`() {
        val behovId = UUID.randomUUID()

        // Steg 1: spleis ber om opptjeningsvurdering
        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "Arbeidstaker"), FØDSELSNUMMER)
        assertEquals(1, rapid.inspektør.size)

        val arbeidsforholdBehov = rapid.inspektør.message(0)
        assertEquals("behov", arbeidsforholdBehov.path("@event_name").asText())
        assertEquals(listOf("ArbeidsforholdV2"), arbeidsforholdBehov.path("@behov").map { it.asText() })

        // Steg 2: Aareg svarer med arbeidsforhold som gir nok opptjening (28+ dager)
        rapid.sendTestMessage(
            arbeidsforholdløsning(
                behovId = behovId,
                arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31")
            ),
            FØDSELSNUMMER
        )
        assertEquals(2, rapid.inspektør.size)

        val opptjeningsvurderingLøsning = rapid.inspektør.message(1)
        assertEquals(behovId.toString(), opptjeningsvurderingLøsning.path("@id").asText())
        val vurderingId = UUID.fromString(
            opptjeningsvurderingLøsning.path("@løsning").path("Opptjeningsvurdering").path("id").asText()
        )

        // Steg 3: spleis ber om resultatet av vurderingen
        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurderingId), FØDSELSNUMMER)
        assertEquals(3, rapid.inspektør.size)

        val resultat = rapid.inspektør.message(2)
        assertTrue(resultat.path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `arbeidstaker med for kort opptjening får ok=false`() {
        val behovId = UUID.randomUUID()

        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "Arbeidstaker"), FØDSELSNUMMER)

        // For kort: kun 27 dager (2018-01-05 til 2018-01-31 = 27 dager)
        rapid.sendTestMessage(
            arbeidsforholdløsning(
                behovId = behovId,
                arbeidsforhold(ansattSiden = "2018-01-05", ansattTil = "2018-01-31")
            ),
            FØDSELSNUMMER
        )

        val vurderingId = UUID.fromString(
            rapid.inspektør.message(1).path("@løsning").path("Opptjeningsvurdering").path("id").asText()
        )

        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurderingId), FØDSELSNUMMER)

        assertFalse(rapid.inspektør.message(2).path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `løpende arbeidsforhold (uten ansattTil) gir ok=true`() {
        val behovId = UUID.randomUUID()

        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "Arbeidstaker"), FØDSELSNUMMER)
        rapid.sendTestMessage(
            arbeidsforholdløsning(
                behovId = behovId,
                arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = null)
            ),
            FØDSELSNUMMER
        )

        val vurderingId = UUID.fromString(
            rapid.inspektør.message(1).path("@løsning").path("Opptjeningsvurdering").path("id").asText()
        )
        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurderingId), FØDSELSNUMMER)

        assertTrue(rapid.inspektør.message(2).path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `partisjonsnøkkel bevares gjennom hele arbeidstakerflyt`() {
        val behovId = UUID.randomUUID()
        val partisjonsnøkkel = "12029240045"

        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "Arbeidstaker"), partisjonsnøkkel)
        assertEquals(partisjonsnøkkel, rapid.inspektør.key(0), "ArbeidsforholdV2-behov skal ha riktig partisjonsnøkkel")

        rapid.sendTestMessage(
            arbeidsforholdløsning(behovId = behovId, arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31")),
            partisjonsnøkkel
        )
        assertEquals(partisjonsnøkkel, rapid.inspektør.key(1), "Opptjeningsvurdering-løsning skal ha riktig partisjonsnøkkel")
    }

    @Test
    fun `duplikat opptjeningsvurdering-behov sender ikke nytt ArbeidsforholdV2-behov`() {
        val behovId = UUID.randomUUID()

        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "Arbeidstaker"), FØDSELSNUMMER)
        assertEquals(1, rapid.inspektør.size)

        // Fullfør vurderingen
        rapid.sendTestMessage(
            arbeidsforholdløsning(behovId = behovId, arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31")),
            FØDSELSNUMMER
        )
        assertEquals(2, rapid.inspektør.size)

        // Spleis sender behovet på nytt (replay)
        rapid.sendTestMessage(opptjeningsvurderingBehov(UUID.randomUUID(), "Arbeidstaker"), FØDSELSNUMMER)
        assertEquals(3, rapid.inspektør.size)

        // Skal svare direkte med eksisterende vurdering, ikke sende nytt ArbeidsforholdV2-behov
        val tredjeUtgang = rapid.inspektør.message(2)
        assertTrue(tredjeUtgang.hasNonNull("@løsning")) { "Skal svare med løsning, ikke nytt behov" }
        assertTrue(tredjeUtgang.path("@løsning").hasNonNull("Opptjeningsvurdering")) { "Løsningen skal inneholde Opptjeningsvurdering" }
    }

    @Test
    fun `duplikat arbeidsforholdløsning gir ikke dobbelt svar`() {
        val behovId = UUID.randomUUID()
        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "Arbeidstaker"), FØDSELSNUMMER)

        val løsning = arbeidsforholdløsning(behovId = behovId, arbeidsforhold(ansattSiden = "2018-01-01", ansattTil = "2018-01-31"))
        rapid.sendTestMessage(løsning, FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)

        rapid.sendTestMessage(løsning, FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size) // Ingen ny melding
    }

    // === Selvstendig næringsdrivende-flyt ===

    @Test
    fun `selvstendig næringsdrivende løses uten ArbeidsforholdV2-behov og får ok=true`() {
        val behovId = UUID.randomUUID()

        // Steg 1: spleis ber om opptjeningsvurdering
        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId, "SelvstendigNæringsdrivende"), FØDSELSNUMMER)
        assertEquals(1, rapid.inspektør.size)

        val løsning = rapid.inspektør.message(0)
        assertFalse(løsning.path("@behov").any { it.asText() == "ArbeidsforholdV2" }) {
            "SelvstendigNæringsdrivende skal ikke trenge ArbeidsforholdV2"
        }
        val vurderingId = UUID.fromString(løsning.path("@løsning").path("Opptjeningsvurdering").path("id").asText())

        // Steg 2: spleis ber om resultatet
        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurderingId), FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)

        val resultat = rapid.inspektør.message(1)
        assertTrue(resultat.path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `selvstendig næringsdrivende med allerede komplett vurdering svares direkte`() {
        val behovId1 = UUID.randomUUID()
        val behovId2 = UUID.randomUUID()

        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId1, "SelvstendigNæringsdrivende"), FØDSELSNUMMER)
        val vurderingId1 = UUID.fromString(
            rapid.inspektør.message(0).path("@løsning").path("Opptjeningsvurdering").path("id").asText()
        )

        // Nytt behov for samme skjæringstidspunkt
        rapid.sendTestMessage(opptjeningsvurderingBehov(behovId2, "SelvstendigNæringsdrivende"), FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)

        val vurderingId2 = UUID.fromString(
            rapid.inspektør.message(1).path("@løsning").path("Opptjeningsvurdering").path("id").asText()
        )
        assertEquals(vurderingId1, vurderingId2) { "Eksisterende vurdering skal gjenbrukes" }
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"
        const val ORGNUMMER = "987654321"

        @Language("JSON")
        fun opptjeningsvurderingBehov(id: UUID, arbeidssituasjon: String) = """
        {
          "@event_name": "behov",
          "@id": "$id",
          "@behov": ["Opptjeningsvurdering"],
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "arbeidssituasjon": "$arbeidssituasjon"
        }
        """

        fun arbeidsforhold(
            orgnummer: String = ORGNUMMER,
            type: String = "ORDINÆRT",
            ansattSiden: String,
            ansattTil: String? = null
        ) = """
        {
          "orgnummer": "$orgnummer",
          "type": "$type",
          "ansattSiden": "$ansattSiden",
          "ansattTil": ${ansattTil?.let { "\"$it\"" } ?: "null"}
        }
        """.trimIndent()

        @Language("JSON")
        fun arbeidsforholdløsning(behovId: UUID, vararg arbeidsforhold: String, erFinal: Boolean = true) = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["ArbeidsforholdV2"],
          "@final": $erFinal,
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "opprinneligBehov": {
            "@event_name": "behov",
            "@id": "$behovId",
            "@behov": ["Opptjeningsvurdering"],
            "fødselsnummer": "$FØDSELSNUMMER",
            "skjæringstidspunkt": "2018-02-01",
            "arbeidssituasjon": "Arbeidstaker"
          },
          "@løsning": {
            "ArbeidsforholdV2": [${arbeidsforhold.joinToString()}]
          }
        }
        """

        @Language("JSON")
        fun opptjeningsvurderingResultatBehov(opptjeningsvurderingId: UUID) = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["OpptjeningsvurderingResultat"],
          "OpptjeningsvurderingResultat": {
            "opptjeningsvurderingId": "$opptjeningsvurderingId"
          }
        }
        """
    }
}
