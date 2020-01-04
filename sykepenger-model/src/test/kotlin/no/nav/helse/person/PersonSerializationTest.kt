package no.nav.helse.person

import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.readResource
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PersonSerializationTest {
    @Test
    fun `restoring av lagret person gir samme objekt`() {
        val testObserver = TestObserver()

        val person = Person(aktørId = "id", fødselsnummer = "fnr")
        person.addObserver(testObserver)

        // trigger endring på person som gjør at vi kan få ut memento fra observer
        person.håndter(nySøknadHendelse())

        val json = testObserver.lastPersonEndretEvent!!.memento.state()

        assertDoesNotThrow {
            Person.restore(Person.Memento.fromString(json))
        }
    }

    @Test
    fun `deserialisering av en serialisert person med gammelt skjema gir feil`() {
        val json = "/serialisert_person_komplett_sak_med_gammel_versjon.json".readResource()
        assertThrows<PersonskjemaForGammelt> { Person.restore(Person.Memento.fromString(json)) }
    }

    @Test
    fun `deserialisering av en serialisert person uten skjemaversjon gir feil`() {
        val json = "/serialisert_person_komplett_sak_uten_versjon.json".readResource()
        assertThrows<PersonskjemaForGammelt> { Person.restore(Person.Memento.fromString(json)) }
    }

    private class TestObserver : PersonObserver {
        var lastPersonEndretEvent: PersonObserver.PersonEndretEvent? = null

        override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
            lastPersonEndretEvent = personEndretEvent
        }

        override fun vedtaksperiodeEndret(event: VedtaksperiodeObserver.StateChangeEvent) {

        }
    }
}
