package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.inngangsvilkar.InngangsvilkårHendelse
import no.nav.helse.inntektshistorikk.InntektshistorikkHendelse
import no.nav.helse.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.oppgave.GosysOppgaveProducer
import no.nav.helse.person.domain.*
import no.nav.helse.person.domain.Sakskompleks.TilstandType.TIL_INFOTRYGD
import no.nav.helse.sakskompleks.SakskompleksProbe
import no.nav.helse.sykepengehistorikk.SykepengehistorikkHendelse
import no.nav.helse.søknad.NySøknadHendelse
import no.nav.helse.søknad.SendtSøknadHendelse
import no.nav.helse.søknad.Sykepengesøknad

internal class PersonMediator(private val personRepository: PersonRepository,
                              private val lagrePersonDao: PersonObserver,
                              private val sakskompleksProbe: SakskompleksProbe = SakskompleksProbe(),
                              private val behovProducer: BehovProducer,
                              private val gosysOppgaveProducer: GosysOppgaveProducer) : PersonObserver {

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {}

    override fun sakskompleksTrengerLøsning(event: Behov) {
        behovProducer.sendNyttBehov(event)
    }

    fun håndterNySøknad(nySøknadHendelse: NySøknadHendelse) =
            try {
                finnPerson(nySøknadHendelse)
                        .also { person ->
                            person.håndterNySøknad(nySøknadHendelse)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, nySøknadHendelse)
            } catch (err: PersonskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterSendtSøknad(sendtSøknadHendelse: SendtSøknadHendelse) =
            try {
                finnPerson(sendtSøknadHendelse)
                        .also { person ->
                            person.håndterSendtSøknad(sendtSøknadHendelse)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, sendtSøknadHendelse)
            } catch (err: PersonskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterInntektsmelding(inntektsmeldingHendelse: InntektsmeldingHendelse) =
            try {
                finnPerson(inntektsmeldingHendelse).also { person ->
                    person.håndterInntektsmelding(inntektsmeldingHendelse)
                }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, inntektsmeldingHendelse)
            } catch (err: PersonskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        try {
            finnPerson(sykepengehistorikkHendelse).also { person ->
                person.håndterSykepengehistorikk(sykepengehistorikkHendelse)
            }
        } catch (err: PersonskjemaForGammelt) {
            sakskompleksProbe.forGammelSkjemaversjon(err)
        }
    }

    fun håndterGenerellSendtSøknad(søknad: Sykepengesøknad) {
        gosysOppgaveProducer.opprettOppgave(søknad.aktørId)
    }

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        if (event.currentState == TIL_INFOTRYGD) {
            gosysOppgaveProducer.opprettOppgave(event.aktørId)
        }
    }

    private fun finnPerson(personHendelse: PersonHendelse) =
            (personRepository.hentPerson(personHendelse.aktørId()) ?: Person(aktørId = personHendelse.aktørId())).also {
                it.addObserver(this)
                it.addObserver(lagrePersonDao)
                it.addObserver(sakskompleksProbe)
            }

}
