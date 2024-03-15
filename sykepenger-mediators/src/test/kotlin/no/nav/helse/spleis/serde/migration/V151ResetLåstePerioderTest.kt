package no.nav.helse.spleis.serde.migration

import org.junit.jupiter.api.Test

internal class V151ResetLåstePerioderTest: MigrationTest(V151ResetLåstePerioder()) {

    @Test
    fun `fjern alle eksisterende låser og legg inn igjen på nytt for alle avsluttede perioder`() {
        assertMigration(
            "/migrations/151/expected.json",
            "/migrations/151/original.json"
        )
    }
}