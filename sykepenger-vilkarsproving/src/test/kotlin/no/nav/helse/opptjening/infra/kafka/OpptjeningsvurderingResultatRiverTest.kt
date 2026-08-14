package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import java.util.stream.Stream
import no.nav.helse.februar
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import java.time.Instant
import no.nav.helse.opptjening.domain.Kodeverkkode
import no.nav.helse.opptjening.domain.Opptjeningsgrunnlag
import no.nav.helse.opptjening.domain.Opptjeningsvurdering
import no.nav.helse.opptjening.domain.PrøvingId
import no.nav.helse.opptjening.domain.Utfall
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class OpptjeningsvurderingResultatRiverTest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val rapid = TestRapid().apply {
        OpptjeningsvurderingResultatRiver(this, repository)
    }

    // Riveren skal svare med ok=true når vurderingen er oppfylt
    @Test
    fun `oppfylt vurdering gir ok = true`() {
        val vurdering = manuellVurdering(kodeverkkode = Kodeverkkode.OPPTJENING_MINST_4_UKER)

        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurdering.id.value))

        assertEquals(1, rapid.inspektør.size)
        val løsning = rapid.inspektør.message(0)
        assertTrue(løsning.path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean())
    }

    // Riveren skal svare med ok=false når vurderingen ikke er oppfylt
    @Test
    fun `ikke-oppfylt vurdering gir ok = false`() {
        val vurdering = manuellVurdering(kodeverkkode = Kodeverkkode.IKKE_OPPTJENING_ARBEID_ELLER_YTELSE)

        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurdering.id.value))

        assertEquals(1, rapid.inspektør.size)
        val løsning = rapid.inspektør.message(0)
        assertFalse(løsning.path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean())
    }

    // Alle kodeverkkoder med Utfall.Oppfylt skal gi ok=true
    @ParameterizedTest
    @MethodSource("oppfylteKodeverkkoder")
    fun `alle oppfylte kodeverkkoder gir ok = true`(kodeverkkode: Kodeverkkode) {
        rapid.reset()
        val vurdering = manuellVurdering(kodeverkkode = kodeverkkode)

        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurdering.id.value))

        val løsning = rapid.inspektør.message(0)
        assertTrue(løsning.path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean()) {
            "$kodeverkkode (${kodeverkkode.utfall}) skal gi ok=true"
        }
    }

    // Alle kodeverkkoder med Utfall.IkkeOppfylt skal gi ok=false
    @ParameterizedTest
    @MethodSource("ikkeOppfylteKodeverkkoder")
    fun `alle ikke-oppfylte kodeverkkoder gir ok = false`(kodeverkkode: Kodeverkkode) {
        rapid.reset()
        val vurdering = manuellVurdering(kodeverkkode = kodeverkkode)

        rapid.sendTestMessage(opptjeningsvurderingResultatBehov(vurdering.id.value))

        val løsning = rapid.inspektør.message(0)
        assertFalse(løsning.path("@løsning").path("OpptjeningsvurderingResultat").path("ok").asBoolean()) {
            "$kodeverkkode (${kodeverkkode.utfall}) skal gi ok=false"
        }
    }

    // Behov uten opptjeningsvurderingId skal ikke plukkes opp av riveren
    @Test
    fun `behov uten opptjeningsvurderingId ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["OpptjeningsvurderingResultat"],
          "OpptjeningsvurderingResultat": {}
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    // Behov av annen type skal ikke plukkes opp
    @Test
    fun `annet behov ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["EtHeltAnnetBehov"],
          "OpptjeningsvurderingResultat": {
            "opptjeningsvurderingId": "${UUID.randomUUID()}"
          }
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    // Meldinger som ikke er behov skal ikke behandles
    @Test
    fun `melding med feil event_name ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "løsning",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["OpptjeningsvurderingResultat"],
          "OpptjeningsvurderingResultat": {
            "opptjeningsvurderingId": "${UUID.randomUUID()}"
          }
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    // Manuell vurdering er samme resultattype som automatisk – bare med en annen kilde
    private fun manuellVurdering(kodeverkkode: Kodeverkkode): Opptjeningsvurdering {
        return Opptjeningsvurdering.manuell(
            prøvingId = PrøvingId.ny(),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar,
            grunnlag = Opptjeningsgrunnlag.Arbeidstaker(emptyList()),
            kodeverkkode = kodeverkkode,
            saksbehandlerIdent = "Z999999",
            fritekstbegrunnelse = "",
            vurdertTidspunkt = Instant.parse("2018-02-01T09:00:00Z")
        ).also { repository.lagre(it) }
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        @JvmStatic
        fun oppfylteKodeverkkoder(): Stream<Kodeverkkode> =
            Kodeverkkode.entries.filter { it.utfall == Utfall.Oppfylt }.stream()

        @JvmStatic
        fun ikkeOppfylteKodeverkkoder(): Stream<Kodeverkkode> =
            Kodeverkkode.entries.filter { it.utfall == Utfall.IkkeOppfylt }.stream()

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
