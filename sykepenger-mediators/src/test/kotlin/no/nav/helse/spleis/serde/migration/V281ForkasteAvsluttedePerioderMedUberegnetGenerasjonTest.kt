package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V281ForkasteAvsluttedePerioderMedUberegnetGenerasjonTest : MigrationTest(V281ForkasteAvsluttedePerioderMedUberegnetGenerasjon()) {

    @Test
    fun `forkaster vedtaksperioder som er UBEREGNET og AVSLUTTET`() {
        assertMigration("/migrations/281/expected.json", "/migrations/281/original.json")
    }
}