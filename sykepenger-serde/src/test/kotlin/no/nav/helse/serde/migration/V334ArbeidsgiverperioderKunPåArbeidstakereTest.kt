package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V334ArbeidsgiverperioderKunPåArbeidstakereTest: MigrationTest(V334ArbeidsgiverperioderKunPåArbeidstakere()) {

    @Test
    fun `migrerer arbeidsgiverperiode`() {
        assertMigration("/migrations/334/expected.json", "/migrations/334/original.json")
    }
}

