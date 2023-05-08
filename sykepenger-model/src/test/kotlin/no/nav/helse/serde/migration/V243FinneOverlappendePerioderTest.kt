package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V243FinneOverlappendePerioderTest: MigrationTest(V243FinneOverlappendePerioder()) {
    @Test
    fun `migrerer perioder`() {
        /*
            må teste:

            to perioder som overlapper helt (og består av én dag)
            to perioder som overlapper helt (og består av mer enn én dag)
            to perioder som overlapper delvis
         */
        assertMigration(
            expectedJson = "/migrations/243/expected.json",
            originalJson = "/migrations/243/original.json"
        )
    }
}