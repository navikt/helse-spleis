package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.readResource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER

internal class V177ForkastOgFlyttVilkårsgrunnlagTest : MigrationTest(V177ForkastOgFlyttVilkårsgrunnlag()) {

    @Test
    fun `Ping-Pong, AUU - IT - AVSLUTTET`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/ping-pong-auu-it-avsluttet_original.json",
            expectedJson = "/migrations/177/ping-pong-auu-it-avsluttet_expected.json"
        )
    }

    @Test
    fun `Kun vilkårsgrunnlag på skjæringstidspunkt skal ikke gi nytt innslag`() {
        val json = toNode("/migrations/177/kun-vilkårsgrunnlag-på-skjæringstidspunkt.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 176)}",
            expectedJson = "${json.put("skjemaVersjon", 177)}"
        )
    }

    @Test
    fun `Tom vilkårsgrunnlaghistorikk`() {
        val json = toNode("/migrations/177/tom-vilkårsgrunnlaghistorikk.json".readResource()) as ObjectNode
        assertMigrationRaw(
            originalJson = "${json.put("skjemaVersjon", 176)}",
            expectedJson = "${json.put("skjemaVersjon", 177)}"
        )
    }

    @Test
    fun `Revurdering fører til nytt skjæringstidspunkt bakover`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/revurder-skjæringstidspunkt-bakover_original.json",
            expectedJson = "/migrations/177/revurder-skjæringstidspunkt-bakover_expected.json"
        )
    }

    @Test
    fun `Et spleis vilkårsgrunnlag på skjæringstidspunktet og flere vilkårsgrunnlag fra infotrygd for samme sykefraværstilfelle - velger siste vilkårsgrunnlag fra infotrygd`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/177/flere-vilkårsgrunnlag-fra-it_original.json",
            expectedJson = "/migrations/177/flere-vilkårsgrunnlag-fra-it_expected.json"
        )
    }

    private fun assertForkastetVilkårsgrunnlag(originalJson: String, expectedJson: String) =
        assertMigration(expectedJson, originalJson, STRICT_ORDER).assertIdOgOpprettet()

    private fun JsonNode.assertIdOgOpprettet() {
        path("vilkårsgrunnlagHistorikk").forEach { innslag ->
            assertDoesNotThrow { UUID.fromString(innslag.path("id").asText())}
            assertDoesNotThrow { LocalDateTime.parse(innslag.path("opprettet").asText()) }
        }
    }
}