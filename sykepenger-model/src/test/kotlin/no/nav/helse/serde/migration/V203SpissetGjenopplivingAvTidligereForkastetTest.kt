package no.nav.helse.serde.migration

import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V203SpissetGjenopplivingAvTidligereForkastetTest : MigrationTest(V203SpissetGjenopplivingAvTidligereForkastet()) {
    @Test
    fun `gjenoppliver vedtaksperiode - arbeidsgiver har historikk fra før`() {
        val result = assertMigration(
            expectedJson = "/migrations/203/expected-med-historikk.json",
            originalJson = "/migrations/203/original-med-historikk.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )

        assertDoesNotThrow {
            UUID.fromString(result.path("arbeidsgivere").single().path("sykdomshistorikk")[0].path("id").asText())
        }
        assertDoesNotThrow {
            LocalDateTime.parse(result.path("arbeidsgivere").single().path("sykdomshistorikk")[0].path("tidsstempel").asText())
        }
    }

    @Test
    fun `gjenoppliver vedtaksperiode - arbeidsgiver har ingen historikk fra før`() {
        val result = assertMigration(
            expectedJson = "/migrations/203/expected-uten-historikk.json",
            originalJson = "/migrations/203/original-uten-historikk.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
        assertDoesNotThrow {
            UUID.fromString(result.path("arbeidsgivere").single().path("sykdomshistorikk")[0].path("id").asText())
        }
        assertDoesNotThrow {
            LocalDateTime.parse(result.path("arbeidsgivere").single().path("sykdomshistorikk")[0].path("tidsstempel").asText())
        }
    }

    @Test
    fun `gjenoppliver vedtaksperiode - arbeidsgiver har overlappende historikk fra før`() {
        val result = assertMigration(
            expectedJson = "/migrations/203/expected-med-overlappende-historikk.json",
            originalJson = "/migrations/203/original-med-overlappende-historikk.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
        assertDoesNotThrow {
            UUID.fromString(result.path("arbeidsgivere").single().path("sykdomshistorikk")[0].path("id").asText())
        }
        assertDoesNotThrow {
            LocalDateTime.parse(result.path("arbeidsgivere").single().path("sykdomshistorikk")[0].path("tidsstempel").asText())
        }
    }
}