package no.nav.helse.unit.person

import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.helse.TestConstants.nySøknad
import no.nav.helse.TestConstants.sendtSøknad
import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.oppgave.OppgaveProducer
import no.nav.helse.person.PersonMediator
import no.nav.helse.person.PersonRepository
import no.nav.helse.person.domain.Person
import no.nav.helse.sakskompleks.SakskompleksProbe
import org.junit.jupiter.api.Test

internal class PersonMediatorTest {

    val probe = mockk<SakskompleksProbe>(relaxed = true)
    val oppgaveProducer = spyk(OppgaveProducer())
    val behovProducer = mockk<BehovProducer>()
    val personMediator = PersonMediator(
        oppgaveProducer = oppgaveProducer,
        sakskompleksProbe = probe,
        personRepository = HashmapPersonRepository(),
        behovProducer = behovProducer)
    val sykepengesøknad = sendtSøknad()

    @Test
    fun `skal håndtere feil ved søknad uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterNySøknad(nySøknad(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<Sykepengesøknad>())
        }
    }

    @Test
    fun `skal lage gosys-oppgave når saken trenger manuell håndtering`() {
        beInTrengerManuellSaksbehandlingState()

        verify(exactly = 1) { oppgaveProducer.opprettOppgave(any()) }
    }

    fun beInTrengerManuellSaksbehandlingState() {
        personMediator.håndterSendtSøknad(sykepengesøknad)
    }
}
