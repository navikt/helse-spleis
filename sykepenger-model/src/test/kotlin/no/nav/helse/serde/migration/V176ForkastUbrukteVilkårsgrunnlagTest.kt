package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER

internal class V176ForkastUbrukteVilkårsgrunnlagTest : MigrationTest(V176ForkastUbrukteVilkårsgrunnlag()) {

    @Test
    fun `Ping-Pong, AUU - IT - AVSLUTTET`() {
        assertForkastetVilkårsgrunnlag(
            originalJson = "/migrations/176/ping-pong-auu-it-avsluttet_original.json",
            expectedJson = "/migrations/176/ping-pong-auu-it-avsluttet_expected.json"
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