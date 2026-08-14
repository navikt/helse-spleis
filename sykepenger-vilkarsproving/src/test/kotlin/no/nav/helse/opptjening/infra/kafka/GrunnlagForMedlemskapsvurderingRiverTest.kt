package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import no.nav.helse.februar
import no.nav.helse.opptjening.application.InMemoryVilkårsprøvingRepository
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.MedlemskapService
import no.nav.helse.opptjening.domain.Kodeverkkode.IKKE_MEDLEM_I_FOLKETRYGDEN
import no.nav.helse.opptjening.domain.Kodeverkkode.MEDLEM_I_FOLKETRYGDEN
import no.nav.helse.opptjening.domain.Medlemskapsprøving
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.VurderingId
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GrunnlagForMedlemskapsvurderingRiverTest {

    private val vurderinger = InMemoryVilkårsvurderingRepository()
    private val prøvinger = InMemoryVilkårsprøvingRepository()
    private val rapid = TestRapid().apply {
        GrunnlagForMedlemskapsvurderingRiver(this, MedlemskapService(vurderinger, prøvinger))
    }

    // Normalflyten: løsningen fullfører den pågående prøvingen og besvarer det opprinnelige behovet
    @Test
    fun `ja fullfører prøvingen og besvarer opprinnelig behov`() {
        val behovId = UUID.randomUUID()
        påbegyntPrøving()

        rapid.sendTestMessage(medlemskapløsning(behovId, "JA"), "123")

        assertEquals(1, rapid.inspektør.size)
        val svar = rapid.inspektør.message(0)
        assertEquals("123", rapid.inspektør.key(0))
        assertEquals(behovId.toString(), svar.path("@id").asText())
        assertEquals(listOf("Medlemskapsvurdering"), svar.path("@behov").map { it.asText() })

        val vurderingId = VurderingId(UUID.fromString(svar.path("@løsning").path("Medlemskapsvurdering").path("id").asText()))
        assertEquals(MEDLEM_I_FOLKETRYGDEN, vurderinger.finn(Vilkår.Medlemskap, vurderingId)!!.kodeverkkode)
        assertTrue(prøvinger.alleProvinger.single().erAvsluttet)
    }

    @Test
    fun `nei gir en vurdering som ikke er oppfylt`() {
        påbegyntPrøving()

        rapid.sendTestMessage(medlemskapløsning(UUID.randomUUID(), "NEI"))

        assertEquals(IKKE_MEDLEM_I_FOLKETRYGDEN, vurderinger.alleVurderinger.single().kodeverkkode)
    }

    // Uten en pågående prøving har vi ingenting å fullføre, og skal ikke svare
    @Test
    fun `løsning uten påbegynt prøving gir ingen svar`() {
        rapid.sendTestMessage(medlemskapløsning(UUID.randomUUID(), "JA"))

        assertEquals(0, rapid.inspektør.size)
        assertEquals(0, vurderinger.antallLagringer)
    }

    // Duplikate svar på behovet skal ikke gi en ny vurdering
    @Test
    fun `duplikat løsning gir ikke ny vurdering`() {
        påbegyntPrøving()
        val løsning = medlemskapløsning(UUID.randomUUID(), "JA")

        rapid.sendTestMessage(løsning)
        rapid.sendTestMessage(løsning)

        assertEquals(1, rapid.inspektør.size)
        assertEquals(1, vurderinger.antallLagringer)
    }

    // Vi svarer kun på et entydig JA/NEI; alt annet må avklares før det kan bli en vurdering
    @Test
    fun `uavklart svar plukkes ikke opp`() {
        påbegyntPrøving()

        rapid.sendTestMessage(medlemskapløsning(UUID.randomUUID(), "UAVKLART_MED_BRUKERSPORSMAAL"))

        assertEquals(0, rapid.inspektør.size)
        assertEquals(0, vurderinger.antallLagringer)
    }

    // Delvise svar skal ikke fullføre prøvingen
    @Test
    fun `løsning som ikke er final ignoreres`() {
        påbegyntPrøving()

        rapid.sendTestMessage(medlemskapløsning(UUID.randomUUID(), "JA", erFinal = false))

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `løsning uten opprinneligBehov ignoreres`() {
        påbegyntPrøving()

        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Medlemskap"],
          "@final": true,
          "fødselsnummer": "$FØDSELSNUMMER",
          "skjæringstidspunkt": "2018-02-01",
          "@løsning": { "Medlemskap": { "resultat": { "svar": "JA" } } }
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `andre behov ignoreres`() {
        påbegyntPrøving()

        rapid.sendTestMessage(medlemskapløsning(UUID.randomUUID(), "JA").replace("\"Medlemskap\"]", "\"EtHeltAnnetBehov\"]"))

        assertEquals(0, rapid.inspektør.size)
    }

    private fun påbegyntPrøving() {
        prøvinger.opprett(Medlemskapsprøving.start(FØDSELSNUMMER, 1.februar).prøving)
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

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
    }
}
