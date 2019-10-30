package no.nav.helse.unit.person

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.PersonskjemaForGammelt
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PersonSerializationTest {
    private companion object {
        private val objectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    @Test
    fun `restoring av lagret person gir samme objekt`() {
        val testObserver = TestObserver()

        val person = Person(aktørId = "id")
        person.addObserver(testObserver)

        // trigger endring på person som gjør at vi kan få ut memento fra observer
        person.håndterNySøknad(nySøknadHendelse())

        val json = testObserver.lastPersonEndretEvent!!.memento.toString()
        val restored = Person.fromJson(json)

        assertEquals(person.aktørId, restored.aktørId)
    }

    @Test
    fun `deserialisering av en serialisert person med gammelt skjema gir feil`() {
        val personJson = "/serialisert_person_komplett_sak_med_gammel_versjon.json".readResource()
        assertThrows<PersonskjemaForGammelt> { Person.fromJson(personJson) }
    }

    @Test
    fun `deserialisering av en serialisert person uten skjemaversjon gir feil`() {
        val personJson = "/serialisert_person_komplett_sak_uten_versjon.json".readResource()
        assertThrows<PersonskjemaForGammelt> { Person.fromJson(personJson) }
    }

    private class TestObserver : PersonObserver {
        var lastPersonEndretEvent: PersonObserver.PersonEndretEvent? = null

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            lastPersonEndretEvent = personEndretEvent
        }

        override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {

        }
    }
}
