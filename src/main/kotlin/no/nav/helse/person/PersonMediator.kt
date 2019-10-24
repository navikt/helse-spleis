package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelse.InntektsmeldingHendelse
import no.nav.helse.hendelse.NySøknadHendelse
import no.nav.helse.hendelse.SendtSøknadHendelse
import no.nav.helse.hendelse.SykepengehistorikkHendelse
import no.nav.helse.oppgave.GosysOppgaveProducer
import no.nav.helse.person.domain.*
import no.nav.helse.person.domain.Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD
import no.nav.helse.sakskompleks.SakskompleksProbe

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
                finnPerson(nySøknadHendelse.aktørId())
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
                finnPerson(sendtSøknadHendelse.aktørId())
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
                finnPerson(inntektsmeldingHendelse.aktørId()).also { person ->
                    person.håndterInntektsmelding(inntektsmeldingHendelse)
                }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, inntektsmeldingHendelse)
            } catch (err: PersonskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
            }

    fun håndterSykepengehistorikk(sykepengehistorikkHendelse: SykepengehistorikkHendelse) {
        try {
            finnPerson(sykepengehistorikkHendelse.aktørId()).also { person ->
                person.håndterSykepengehistorikk(sykepengehistorikkHendelse)
            }
        } catch (err: PersonskjemaForGammelt) {
            sakskompleksProbe.forGammelSkjemaversjon(err)
        }
    }

    override fun sakskompleksEndret(event: SakskompleksObserver.StateChangeEvent) {
        if (event.currentState == SKAL_TIL_INFOTRYGD) {
            gosysOppgaveProducer.opprettOppgave(event.aktørId)
        }
    }

    private fun finnPerson(aktørId: String) =
            (personRepository.hentPerson(aktørId) ?: Person(aktørId = aktørId)).also {
                it.addObserver(this)
                it.addObserver(lagrePersonDao)
                it.addObserver(sakskompleksProbe)
            }

}
