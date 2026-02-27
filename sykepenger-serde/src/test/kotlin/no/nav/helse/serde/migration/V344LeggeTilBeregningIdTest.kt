package no.nav.helse.serde.migration

import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant
import no.nav.helse.serde.migration.V344LeggeTilBeregningId.Companion.localDateTime
import no.nav.helse.serde.migration.V344LeggeTilBeregningId.Companion.toKotlinInstant
import no.nav.helse.serde.serdeObjectMapper
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

internal class SekvensiellUUidGenerator(): UuidGenerator {
    private var i = 0
    override fun generate(tidsstempel: Instant): UUID {
        i++
        require(i <= 99)
        return UUID.fromString("00000000-0000-0000-0000-0000000000${i.toString().padStart(2, '0')}")    }
}

internal class V344LeggeTilBeregningIdTest : MigrationTest(V344LeggeTilBeregningId(SekvensiellUUidGenerator())) {

    @Test
    fun `Legger på beregningId`() {
        assertMigration("/migrations/344/expected.json", "/migrations/344/original.json")
    }

    @Test
    fun `Uuid v7 basert på tidsstempel`(){
        assertDoesNotThrow { UuidGenerator.UuidV7BasertPåTidsstempelGenerator.generate(Clock.System.now()) }
        assertDoesNotThrow { UuidGenerator.UuidV7BasertPåTidsstempelGenerator.generate(LocalDateTime.parse("2020-02-26T15:00:29.191576").toKotlinInstant()) }
        assertDoesNotThrow { UuidGenerator.UuidV7BasertPåTidsstempelGenerator.generate(LocalDateTime.parse("2026-02-26T16:00:29.191576").toKotlinInstant()) }
    }

    @Test
    fun `Lese inn tidsstempler`() {
        @Language("JSON")
        val json = """{ "tidsstempel": [2026, 2, 27, 14, 1, 7, 421028000] }""".let { serdeObjectMapper.readTree(it) }
        assertEquals(LocalDateTime.parse("2026-02-27T14:01:07.421028"), json.path("tidsstempel").localDateTime())
    }
}
