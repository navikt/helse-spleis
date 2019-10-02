package no.nav.helse.unit.person

import no.nav.helse.TestConstants.søknad
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sakskompleks.domain.SakskompleksObserver
import org.junit.jupiter.api.Test

internal class PersonTest {

    @Test internal fun `first event`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.add(søknad)
        }
        assert(observer.wasTriggered)
    }

    private class TestObserver: PersonObserver {
        internal var wasTriggered = false
        override fun sakskompleksChanged(event: SakskompleksObserver.StateChangeEvent) {
            wasTriggered = true
        }
    }

    private fun inntektsmeldingMottattTilstand() : Sakskompleks {
        TODO()
    }
}