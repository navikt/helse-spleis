package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V165SletterDuplikaterFraInntektshistorikkenTest : MigrationTest(V165SletterDuplikaterFraInntektshistorikken()) {
    @Test
    fun `sletter duplikate innslag i inntektshistorikken`() {
        assertMigration("/migrations/crazyloop/expected.json", "/migrations/crazyloop/original.json")
    }

    @Test
    fun `sletter duplikate innslag i inntektshistorikken fra forkastet vedtaksperiode som har gått i loop`() {
        assertMigration("/migrations/crazyloop/forkastetExpected.json", "/migrations/crazyloop/forkastetOriginal.json")
    }
}
