package no.nav.helse.unit.spleis

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.TestConstants.inntektsmeldingHendelse
import no.nav.helse.TestConstants.nySøknadHendelse
import no.nav.helse.TestConstants.sendtSøknadHendelse
import no.nav.helse.behov.BehovProducer
import no.nav.helse.person.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.person.hendelser.søknad.NySøknadHendelse
import no.nav.helse.person.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.spleis.PersonMediator
import no.nav.helse.spleis.SakskompleksProbe
import no.nav.helse.spleis.oppgave.GosysOppgaveProducer
import org.junit.jupiter.api.Test

internal class PersonMediatorTest {

    val probe = mockk<SakskompleksProbe>(relaxed = true)
    val oppgaveProducer = mockk<GosysOppgaveProducer>(relaxed = true)
    val behovProducer = mockk<BehovProducer>()
    val repo = HashmapPersonRepository()
    val personMediator = PersonMediator(
            sakskompleksProbe = probe,
            personRepository = repo,
            lagrePersonDao = repo,
            behovProducer = behovProducer,
            gosysOppgaveProducer = oppgaveProducer)

    val sendtSøknadHendelse = sendtSøknadHendelse()

    @Test
    fun `skal håndtere feil ved ny søknad uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterNySøknad(nySøknadHendelse(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<NySøknadHendelse>())
        }
    }

    @Test
    fun `skal håndtere feil ved sendt søknad uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterSendtSøknad(sendtSøknadHendelse(arbeidsgiver = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<SendtSøknadHendelse>())
        }
    }

    @Test
    fun `skal håndtere feil ved inntektsmelding uten virksomhetsnummer for arbeidsgiver`() {
        personMediator.håndterInntektsmelding(inntektsmeldingHendelse(virksomhetsnummer = null))

        verify(exactly = 1) {
            probe.utenforOmfang(any(), any<InntektsmeldingHendelse>())
        }
    }

    @Test
    fun `skal lage gosys-oppgave når saken må behandles i infotrygd`() {
        beInMåBehandlesIInfotrygdState()

        verify(exactly = 1) {
            oppgaveProducer.opprettOppgave(aktørId = sendtSøknadHendelse.aktørId())
        }
    }

    fun beInMåBehandlesIInfotrygdState() {
        personMediator.håndterSendtSøknad(sendtSøknadHendelse)
    }
}
