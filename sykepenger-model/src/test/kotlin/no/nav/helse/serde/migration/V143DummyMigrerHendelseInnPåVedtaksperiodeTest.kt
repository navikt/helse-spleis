package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V143DummyMigrerHendelseInnPåVedtaksperiodeTest: MigrationTest(V143DummyMigrerHendelseInnPåVedtaksperiode()) {

    @Test
    fun `migrering endrer ikke på noe`() {
        assertMigration("/migrations/143/expected.json", "/migrations/143/original.json")
    }
}
