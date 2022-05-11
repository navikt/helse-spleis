package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.readResource
import no.nav.helse.serde.serdeObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.skyscreamer.jsonassert.JSONCompareMode.STRICT_ORDER

internal class V153FjerneSykmeldingsdagerTest: MigrationTest(V153FjerneSykmeldingsdager()) {

    @Test
    fun `fjerner dager fra sykdomstidslinjen med kilde Sykmelding`() {
        assertMigration(
            expectedJson = "/migrations/153/harSykmeldingsdagerExpected.json",
            originalJson = "/migrations/153/harSykmeldingsdagerOriginal.json",
            jsonCompareMode = STRICT_ORDER
        )

        val migrert = migrer("/migrations/153/harSykmeldingsdagerOriginal.json".readResource())
        assertDoesNotThrow { UUID.fromString(migrert.path("arbeidsgivere").first()["sykdomshistorikk"].first()["id"].asText()) }
        assertDoesNotThrow { LocalDateTime.parse(migrert.path("arbeidsgivere").first()["sykdomshistorikk"].first()["tidsstempel"].asText()) }
    }

    @Test
    fun `ingen endringer dersom det ikke finnes dager på sykdomstidlinjen med kilde Sykmelding`() {
        val original = "/migrations/153/harIkkeSykmeldingsdagerOriginal.json".readResource()
        val expected = (serdeObjectMapper.readTree(original) as ObjectNode).put("skjemaVersjon", 153)
        assertMigrationRaw("$expected", original)
    }
}