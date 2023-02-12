package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class TrøbleteUtbetalingerTest : MigrationTest(MigrationForTestOnly()) {

    @Test
    fun `forkaster trøblete utbetalinger`() {
        assertMigration(
            expectedJson = "/migrations/trøblete_utbetalinger/expected.json",
            originalJson = "/migrations/trøblete_utbetalinger/original.json",
            jsonCompareMode = JSONCompareMode.STRICT_ORDER
        )
    }

    private class MigrationForTestOnly : JsonMigration(218) {
        override val description = "test"
        private val trøblete = TrøbleteUtbetalinger(setOf("42e15bdb-a176-4681-a17f-b897202b0042"))

        override fun doMigration(jsonNode: ObjectNode, meldingerSupplier: MeldingerSupplier) {
            trøblete.doMigration(jsonNode)
        }
    }
}