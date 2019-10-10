package no.nav.helse.unit.person

import no.nav.helse.TestConstants
import no.nav.helse.person.domain.Person
import no.nav.helse.readResource
import no.nav.helse.sykdomstidslinje.objectMapper
import no.nav.syfo.kafka.sykepengesoknad.dto.ArbeidsgiverDTO
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

    @Test
    fun `deserialisering av en serialisert person gir lik json`() {
        val personJson = "/serialisert_person_komplett_sak.json".readResource()

        val restoredPerson = Person.fromJson(personJson)

        val serializedPerson = restoredPerson.toJson()

        assertEquals(objectMapper.readTree(personJson), objectMapper.readTree(serializedPerson))
    }

}