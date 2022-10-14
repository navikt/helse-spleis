package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V183UtbetalingerOgVilkårsgrunnlagTest : MigrationTest(V183UtbetalingerOgVilkårsgrunnlag()) {

    @Test
    fun test() {
        assertMigration(
            expectedJson = "/migrations/183/expected.json",
            originalJson = "/migrations/183/original.json"
        )
    }
}