package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.readResource
import org.junit.jupiter.api.Test

internal class V250FikseAvvisteFeriepengeoppdragTest: MigrationTest(V250FikseAvvisteFeriepengeoppdrag()) {

    @Test
    fun `erstatter mottaker og fagsystemId på arbeidsgiveroppdragene`() {
        assertFiksetFeriepengeoppdrag(
            expectedJson = "/migrations/250/expected.json",
            originalJson = "/migrations/250/original.json"
        )
    }

    private fun assertFiksetFeriepengeoppdrag(originalJson: String, expectedJson: String) {
        val migrert = migrer(originalJson.readResource())
        val fagsystemId1 = migrert.fagsystemId(1)
        val fagsystemId2 = migrert.fagsystemId(2)
        val expected = expectedJson.readResource()
            .replace("{{fagsystemId1}}", fagsystemId1)
            .replace("{{fagsystemId2}}", fagsystemId2)
        assertJson(migrert.toString(), expected)
    }

    private fun JsonNode.fagsystemId(index: Int) =
        path("arbeidsgivere")[index]
        .path("feriepengeutbetalinger")
        .first { it.path("opptjeningsår").asText() == "2022" }
        .path("oppdrag")
        .path("fagsystemId").asText()
}