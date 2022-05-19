package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V158SletterDuplikaterFraInntektshistorikkenTest : MigrationTest(V158SletterDuplikaterFraInntektshistorikken()) {
    @Test
    fun `sletter duplikate innslag i inntektshistorikken`() {
        assertMigration("/migrations/158/expected.json", "/migrations/158/original.json")
    }
}
