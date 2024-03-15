package no.nav.helse.serde.migration

import java.time.LocalDateTime
import org.junit.jupiter.api.Test

internal class V285LoggeRareAnnulleringerTest: MigrationTest(V285LoggeRareAnnulleringer(
    forkast = setOf("52fc8116-1757-45bc-8ca1-d333b33d3496"),
    forkastetTidspunkt = { LocalDateTime.parse("2024-01-23T08:15:00.204957") }
)) {

    @Test
    fun `forkaster annulleringer som ikke er sendt til oppdrag`() {
        assertMigration("/migrations/285/expected.json", "/migrations/285/original.json")
    }
}