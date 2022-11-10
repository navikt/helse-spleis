package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V195RefusjonsopplysningerIVilkårsgrunnlagPreppTest : MigrationTest(V195RefusjonsopplysningerIVilkårsgrunnlagPrepp()) {

    @Test
    fun `Migrerer inn tomme refusjonsopplysninger`() {
        assertMigration(
            "/migrations/195/expected.json",
            "/migrations/195/original.json"
        )
    }
}