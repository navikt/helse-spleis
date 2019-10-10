package no.nav.helse.unit.person

import no.nav.helse.person.domain.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PersonSerializationTest {
    @Test
    fun `restoring av lagret person gir samme objekt`() {
        val person = Person(aktørId = "id")
        val json = person.toJson()
        val restored = Person.fromJson(json)
        assertEquals(person, restored)
    }
}