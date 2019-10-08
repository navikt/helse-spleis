package no.nav.helse.unit.person

import no.nav.helse.TestConstants.søknad
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PersonTest {

    @Test internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.håndterNySøknad(søknad(status = SoknadsstatusDTO.NY))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.NY_SØKNAD_MOTTATT, observer.sakskomplekstilstand)
    }

    @Test internal fun `sendt søknad uten sak trigger sakskompleks endret-hendelse`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.håndterSendtSøknad(søknad(status = SoknadsstatusDTO.SENDT))
        }
        assertTrue(observer.personEndret)
        assertTrue(observer.wasTriggered)
        assertEquals(Sakskompleks.TilstandType.TRENGER_MANUELL_HÅNDTERING, observer.sakskomplekstilstand)
    }

    private class TestObserver: PersonObserver {
        internal var wasTriggered = false
        internal var personEndret = false
        internal var sakskomplekstilstand: Sakskompleks.TilstandType? = null

        override fun personEndret(person: Person) {
            personEndret = true
        }

        override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
            wasTriggered = true
            sakskomplekstilstand = event.currentState
        }
    }

    private fun inntektsmeldingMottattTilstand() : Sakskompleks {
        TODO()
    }
}
