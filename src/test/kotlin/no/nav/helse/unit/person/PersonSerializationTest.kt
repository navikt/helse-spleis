package no.nav.helse.unit.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PersonSerializationTest {
    companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    fun `restoring av lagret person gir samme objekt`() {
        val person = Person(aktørId = "id")
        val json = person.memento().toString()
        val restored = Person.fromJson(json)
        assertEquals(person.aktørId, restored.aktørId)
    }

    @Test
    fun `deserialisering av en serialisert person gir lik json`() {
        val personJson = "/serialisert_person_komplett_sak.json".readResource()

        val restoredPerson = Person.fromJson(personJson)

        val serializedPerson = restoredPerson.memento().toString()

        assertEquals(objectMapper.readTree(personJson), objectMapper.readTree(serializedPerson))
    }

    @Test
    fun `restoring adds the sakskompleks observer for the person`() {
        val initialPerson = Person("abde")
        initialPerson.håndterNySøknad(TestConstants.nySøknad())
        val personJson = initialPerson.memento().toString()

        val testObserver = TestObserver()
        val restoredPerson = Person.fromJson(personJson)
        restoredPerson.addObserver(testObserver)
        restoredPerson.håndterSendtSøknad(TestConstants.sendtSøknad())
        assertEquals(1, testObserver.personUpdates)
    }

    class TestObserver : PersonObserver {
        var personUpdates: Int = 0

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            personUpdates++
        }

        override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {

        }
    }
}
