package no.nav.helse.unit.person

import no.nav.helse.TestConstants.søknad
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver
import no.nav.syfo.kafka.sykepengesoknad.dto.SoknadsstatusDTO
import org.junit.jupiter.api.Test

internal class PersonTest {

    @Test internal fun `ny søknad fører til at sakskompleks trigger en sakskompleks endret hendelse`() {
        val observer = TestObserver()
        Person().also {
            it.addObserver(observer)
            it.add(søknad(status = SoknadsstatusDTO.NY))
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
