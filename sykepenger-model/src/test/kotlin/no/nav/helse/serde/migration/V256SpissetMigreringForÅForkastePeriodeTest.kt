package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V256SpissetMigreringForÅForkastePeriodeTest : MigrationTest(V256SpissetMigreringForÅForkastePeriode()) {

    @Test
    fun `erstatter mottaker og fagsystemId på arbeidsgiveroppdragene`() {
        assertMigration(
            expectedJson = "/migrations/256/expected.json",
            originalJson = "/migrations/256/original.json"
        )
    }
}