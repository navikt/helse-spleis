package no.nav.helse.serde.migration

import org.junit.jupiter.api.Test

internal class V149ResetLåstePerioderTest: MigrationTest(V149ResetLåstePerioder()) {

    @Test
    fun `fjern alle eksisterende låser og legg inn igjen på nytt for alle avsluttede perioder`() {
        assertMigration(
            "/migrations/149/expected.json",
            "/migrations/149/original.json"
        )
    }
}