package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONCompareMode

internal class V210EndreGamlePerioderMedKildeSykmeldingTest: MigrationTest(V210EndreGamlePerioderMedKildeSykmelding()) {

    @Test
    fun `endrer kilde fra sykmelding til søknad`() {
        assertMigration(
            expectedJson = "/migrations/210/expected-uten-sykmelding.json",
            originalJson = "/migrations/210/original-med-sykmelding.json",
            jsonCompareMode = JSONCompareMode.STRICT
        )
    }

}