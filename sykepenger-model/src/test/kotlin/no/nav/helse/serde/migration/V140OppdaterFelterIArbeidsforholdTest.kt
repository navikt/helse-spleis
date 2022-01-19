package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V140OppdaterFelterIArbeidsforholdTest : MigrationTest(V140OppdaterFelterIArbeidsforhold()) {
    @Test
    fun `migrering av arbeidsforhold`() {
        assertMigration(
            expectedJson = "/migrations/140/expected.json",
            originalJson = "/migrations/140/original.json"
        )
    }

    @Test
    fun `migrering av arbeidsforhold ved tom liste`() {
        assertMigration(
            expectedJson = "/migrations/140/expectedTomArbeidsforholdhistorikk.json",
            originalJson = "/migrations/140/originalTomArbeidsforholdhistorikk.json"
        )
    }

    @Test
    fun `migrering av arbeidsforhold ved flere innslag i historikken`() {
        assertMigration(
            expectedJson = "/migrations/140/expectedFlereArbeidsforholdhistorikkInnslag.json",
            originalJson = "/migrations/140/originalFlereArbeidsforholdhistorikkInnslag.json"
        )
    }
}
