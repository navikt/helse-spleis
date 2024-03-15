package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V137InntektsmeldingInfoHistorikkTest : MigrationTest(V137InntektsmeldingInfoHistorikk()) {

    @Test
    fun `migrerer inntektsmeldinginfo til arbeidsgivernivå`() {
        assertMigration(
            expectedJson = "/migrations/137/expected.json",
            originalJson = "/migrations/137/original.json"
        )
    }
}
