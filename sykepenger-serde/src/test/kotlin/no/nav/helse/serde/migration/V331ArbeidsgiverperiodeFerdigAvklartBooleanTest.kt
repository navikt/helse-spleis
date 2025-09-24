package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V331ArbeidsgiverperiodeFerdigAvklartBooleanTest: MigrationTest(V331ArbeidsgiverperiodeFerdigAvklartBoolean()) {

    @Test
    fun `migrerer arbeidsgiverperiode`() {
        assertMigration("/migrations/331/expected.json", "/migrations/331/original.json", JSONCompareMode.STRICT_ORDER)
    }
}
