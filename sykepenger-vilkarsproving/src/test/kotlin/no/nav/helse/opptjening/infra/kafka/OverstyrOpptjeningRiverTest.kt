package no.nav.helse.opptjening.infra.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import java.util.UUID
import no.nav.helse.opptjening.application.InMemoryVilkårsvurderingRepository
import no.nav.helse.opptjening.application.OpptjeningService
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class OverstyrOpptjeningRiverTest {

    private val repository = InMemoryVilkårsvurderingRepository()
    private val rapid = TestRapid().apply {
        OverstyrOpptjeningRiver(this, OpptjeningService(repository))
    }

    @BeforeEach
    fun setUp() { rapid.reset() }

    @Test
    fun `overstyring publiserer opptjeningsvurdering_manuelt_overstyrt`() {
        rapid.sendTestMessage(overstyring(), FØDSELSNUMMER)

        assertEquals(1, rapid.inspektør.size)
        val melding = rapid.inspektør.message(0)
        assertEquals("opptjeningsvurdering_manuelt_overstyrt", melding.path("@event_name").asText())
        assertTrue(melding.hasNonNull("opptjeningsvurderingId"))
        assertTrue(melding.path("ok").asBoolean())
        assertEquals(FØDSELSNUMMER, melding.path("fødselsnummer").asText())
        assertEquals("2018-01-01", melding.path("skjæringstidspunkt").asText())
    }

    @Test
    fun `partisjonsnøkkel er fødselsnummer`() {
        rapid.sendTestMessage(overstyring(), FØDSELSNUMMER)
        assertEquals(FØDSELSNUMMER, rapid.inspektør.key(0))
    }

    @Test
    fun `overstyring lagrer vurdering i repository`() {
        rapid.sendTestMessage(overstyring(), FØDSELSNUMMER)
        assertEquals(1, repository.antallLagringer)
    }

    @Test
    fun `to overstyringer for samme person gir to vurderinger`() {
        rapid.sendTestMessage(overstyring(), FØDSELSNUMMER)
        rapid.sendTestMessage(overstyring(), FØDSELSNUMMER)
        assertEquals(2, rapid.inspektør.size)
        assertEquals(2, repository.antallLagringer)
    }

    @Test
    fun `melding uten påkrevde felter ignoreres`() {
        rapid.sendTestMessage("""{ "@event_name": "saksbehandler_opptjeningsoverstyring" }""", FØDSELSNUMMER)
        assertEquals(0, rapid.inspektør.size)
    }

    @Test
    fun `annen event_name ignoreres`() {
        rapid.sendTestMessage(overstyring().replace("saksbehandler_opptjeningsoverstyring", "noe_annet"), FØDSELSNUMMER)
        assertEquals(0, rapid.inspektør.size)
    }

    private companion object {
        const val FØDSELSNUMMER = "12029240045"

        @Language("JSON")
        fun overstyring(
            fødselsnummer: String = FØDSELSNUMMER,
            skjæringstidspunkt: String = "2018-01-01",
            saksbehandlerIdent: String = "A123456",
            begrunnelse: String = "Søker dokumenterte opptjening via annen kilde"
        ) = """
        {
          "@event_name": "saksbehandler_opptjeningsoverstyring",
          "@id": "${UUID.randomUUID()}",
          "fødselsnummer": "$fødselsnummer",
          "skjæringstidspunkt": "$skjæringstidspunkt",
          "saksbehandlerIdent": "$saksbehandlerIdent",
          "begrunnelse": "$begrunnelse"
        }
        """
    }
}
