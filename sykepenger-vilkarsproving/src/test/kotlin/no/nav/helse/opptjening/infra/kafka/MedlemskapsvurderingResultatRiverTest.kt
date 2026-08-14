package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.time.Instant
import java.util.UUID
import java.util.stream.Stream
import no.nav.helse.februar
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.domain.Kodeverkkode
import no.nav.helse.opptjening.domain.Medlemskapsgrunnlag
import no.nav.helse.opptjening.domain.Medlemskapssvar
import no.nav.helse.opptjening.domain.PrøvingId
import no.nav.helse.opptjening.domain.Utfall
import no.nav.helse.opptjening.domain.Vilkår
import no.nav.helse.opptjening.domain.Vilkårsvurdering
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class MedlemskapsvurderingResultatRiverTest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val rapid = TestRapid().apply {
        MedlemskapsvurderingResultatRiver(this, repository)
    }

    @Test
    fun `oppfylt vurdering gir ok = true`() {
        val vurdering = manuellVurdering(Kodeverkkode.MEDLEM_I_FOLKETRYGDEN)

        rapid.sendTestMessage(medlemskapsvurderingResultatBehov(vurdering.id.value))

        assertEquals(1, rapid.inspektør.size)
        assertTrue(rapid.inspektør.message(0).path("@løsning").path("MedlemskapsvurderingResultat").path("ok").asBoolean())
    }

    @Test
    fun `ikke-oppfylt vurdering gir ok = false`() {
        val vurdering = manuellVurdering(Kodeverkkode.IKKE_MEDLEM_I_FOLKETRYGDEN)

        rapid.sendTestMessage(medlemskapsvurderingResultatBehov(vurdering.id.value))

        assertEquals(1, rapid.inspektør.size)
        assertFalse(rapid.inspektør.message(0).path("@løsning").path("MedlemskapsvurderingResultat").path("ok").asBoolean())
    }

    @ParameterizedTest
    @MethodSource("oppfylteKodeverkkoder")
    fun `alle oppfylte kodeverkkoder gir ok = true`(kodeverkkode: Kodeverkkode) {
        rapid.reset()
        val vurdering = manuellVurdering(kodeverkkode)

        rapid.sendTestMessage(medlemskapsvurderingResultatBehov(vurdering.id.value))

        assertTrue(rapid.inspektør.message(0).path("@løsning").path("MedlemskapsvurderingResultat").path("ok").asBoolean()) {
            "$kodeverkkode (${kodeverkkode.utfall}) skal gi ok=true"
        }
    }

    @ParameterizedTest
    @MethodSource("ikkeOppfylteKodeverkkoder")
    fun `alle ikke-oppfylte kodeverkkoder gir ok = false`(kodeverkkode: Kodeverkkode) {
        rapid.reset()
        val vurdering = manuellVurdering(kodeverkkode)

        rapid.sendTestMessage(medlemskapsvurderingResultatBehov(vurdering.id.value))

        assertFalse(rapid.inspektør.message(0).path("@løsning").path("MedlemskapsvurderingResultat").path("ok").asBoolean()) {
            "$kodeverkkode (${kodeverkkode.utfall}) skal gi ok=false"
        }
    }

    @Test
    fun `behov uten medlemskapsvurderingId ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["MedlemskapsvurderingResultat"]
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `annet behov ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["EtHeltAnnetBehov"],
          "MedlemskapsvurderingResultat": {
            "medlemskapsvurderingId": "${UUID.randomUUID()}"
          }
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `melding med feil event_name ignoreres`() {
        @Language("JSON")
        val melding = """
        {
          "@event_name": "løsning",
          "@id": "${UUID.randomUUID()}",
          "@behov": ["MedlemskapsvurderingResultat"],
          "MedlemskapsvurderingResultat": {
            "medlemskapsvurderingId": "${UUID.randomUUID()}"
          }
        }
        """
        rapid.sendTestMessage(melding)

        assertEquals(0, rapid.inspektør.size)
    }

    // Manuell vurdering er samme resultattype som automatisk – bare med en annen kilde
    private fun manuellVurdering(kodeverkkode: Kodeverkkode): Vilkårsvurdering =
        Vilkårsvurdering.manuell(
            prøvingId = PrøvingId.ny(),
            fødselsnummer = FØDSELSNUMMER,
            skjæringstidspunkt = 1.februar,
            grunnlag = Medlemskapsgrunnlag(Medlemskapssvar.Ja),
            kodeverkkode = kodeverkkode,
            saksbehandlerIdent = "Z999999",
            fritekstbegrunnelse = "",
            vurdertTidspunkt = Instant.parse("2018-02-01T09:00:00Z")
        ).also { repository.lagre(it) }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        @JvmStatic
        fun oppfylteKodeverkkoder(): Stream<Kodeverkkode> =
            Kodeverkkode.entries.filter { it.vilkår == Vilkår.Medlemskap && it.utfall == Utfall.Oppfylt }.stream()

        @JvmStatic
        fun ikkeOppfylteKodeverkkoder(): Stream<Kodeverkkode> =
            Kodeverkkode.entries.filter { it.vilkår == Vilkår.Medlemskap && it.utfall == Utfall.IkkeOppfylt }.stream()

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
