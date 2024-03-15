package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V257FikseOpptjeningsperiodeTest: MigrationTest(V257FikseOpptjeningsperiode()) {

    @Test
    fun `erstatter mottaker og fagsystemId på arbeidsgiveroppdragene`() {
        assertMigration(
            expectedJson = "/migrations/257/expected.json",
            originalJson = "/migrations/257/original.json"
        )
    }
}