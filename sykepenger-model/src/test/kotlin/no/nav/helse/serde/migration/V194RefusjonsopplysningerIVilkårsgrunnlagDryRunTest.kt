package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.readResource
import no.nav.helse.serde.serdeObjectMapper
import org.junit.jupiter.api.Test

internal class V194RefusjonsopplysningerIVilkårsgrunnlagDryRunTest: MigrationTest(V194RefusjonsopplysningerIVilkårsgrunnlagDryRun()) {

    @Test
    fun `ingen endringer ved kjøring av dry run`() {
        val original = "/migrations/196/original.json".readResource()
        val expected = (serdeObjectMapper.readTree(original) as ObjectNode).put("skjemaVersjon", 194)
        assertMigrationRaw("$expected", original)
    }

}