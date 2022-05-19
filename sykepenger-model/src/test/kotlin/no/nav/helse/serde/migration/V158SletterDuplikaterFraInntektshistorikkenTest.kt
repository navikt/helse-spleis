package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V158SletterDuplikaterFraInntektshistorikkenTest : MigrationTest(V158SletterDuplikaterFraInntektshistorikken()) {
    @Test
    fun `sletter duplikate innslag i inntektshistorikken`() {
        assertMigration("/migrations/158/expected.json", "/migrations/158/original.json")
    }

    @Test
    fun `sletter duplikate innslag i inntektshistorikken fra forkastet vedtaksperiode som har gått i loop`() {
        assertMigration("/migrations/158/forkastetExpected.json", "/migrations/158/forkastetOriginal.json")
    }
}
