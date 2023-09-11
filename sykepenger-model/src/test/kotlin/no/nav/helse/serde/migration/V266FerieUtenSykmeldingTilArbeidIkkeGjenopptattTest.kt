package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V266FerieUtenSykmeldingTilArbeidIkkeGjenopptattTest: MigrationTest(V266FerieUtenSykmeldingTilArbeidIkkeGjenopptatt()) {

    @Test
    fun `renamer ferie uten sykmelding til arbeid ikke gjenopptatt`() {
        assertMigration("/migrations/266/expected.json", "/migrations/266/original.json")
    }

}