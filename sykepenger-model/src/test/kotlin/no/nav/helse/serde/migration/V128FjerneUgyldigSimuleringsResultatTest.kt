package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V128FjerneUgyldigSimuleringsResultatTest : MigrationTest(V128FjerneUgyldigSimuleringsResultat()) {

    @Test
    fun `fjerner simuleringer som er lagret på oppdragsnivå`() {
        assertMigration(
            expectedJson = "/migrations/128/expected.json",
            originalJson = "/migrations/128/original.json"
        )
    }
}
