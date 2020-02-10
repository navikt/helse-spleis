package no.nav.helse.serde

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

internal class VersjoneringTest {

    @Test
    fun `kan lese person med skjemaversjon 4`() {
        val person = parsePerson(lesPersonJson("person.json"))
        assertNotNull(person)
    }

    fun lesPersonJson(filnavn: String) =
        VersjoneringTest::class.java.getResource("/versjonerte_personer/$filnavn").readText()
}
