package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test


internal class V162SletterDuplikaterFraInntektshistorikkenTest : MigrationTest(V162SletterDuplikaterFraInntektshistorikken()) {
    @Test
    fun `sletter duplikate innslag i inntektshistorikken`() {
        assertMigration("/migrations/162_real/expected.json", "/migrations/162_real/original.json")
    }

    @Test
    fun `sletter duplikate innslag i inntektshistorikken fra forkastet vedtaksperiode som har gått i loop`() {
        assertMigration("/migrations/162_real/forkastetExpected.json", "/migrations/162_real/forkastetOriginal.json")
    }
}
