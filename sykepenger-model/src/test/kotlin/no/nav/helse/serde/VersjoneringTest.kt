package no.nav.helse.serde

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class VersjoneringTest {

    @Test
    fun `kan lese person med skjemaversjon 0`() {
        val person = SerialisertPerson(lesPersonJson("person_v0.json")).deserialize()
        assertNotNull(person)
    }

    private fun lesPersonJson(filnavn: String) =
        VersjoneringTest::class.java.getResource("/versjonerte_personer/$filnavn").readText()
}
