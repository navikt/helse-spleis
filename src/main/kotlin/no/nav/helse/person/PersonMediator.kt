package no.nav.helse.person

import no.nav.helse.behov.Behov
import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelse.InntektsmeldingMottatt
import no.nav.helse.hendelse.NySøknadOpprettet
import no.nav.helse.hendelse.SendtSøknadMottatt
import no.nav.helse.hendelse.Sykepengehistorikk
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

    fun håndterNySøknad(sykepengesøknad: NySøknadOpprettet) =
            try {
                finnPerson(sykepengesøknad.aktørId)
                        .also { person ->
                            person.håndterNySøknad(sykepengesøknad)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, sykepengesøknad)
            } catch (err: PersonskjemaForGammelt) {
                sakskompleksProbe.forGammelSkjemaversjon(err)
}

    fun håndterSendtSøknad(sykepengesøknad: SendtSøknadMottatt) =
            try {
                finnPerson(sykepengesøknad.aktørId)
                        .also { person ->
                            person.håndterSendtSøknad(sykepengesøknad)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, sykepengesøknad)
            }

    fun håndterInntektsmelding(inntektsmelding: InntektsmeldingMottatt) =
            try {
                finnPerson(inntektsmelding.aktørId()).also { person ->
                    person.håndterInntektsmelding(inntektsmelding)
                }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, inntektsmelding)
            }

    fun håndterSykepengehistorikk(sykepengehistorikk: Sykepengehistorikk) {
        finnPerson(sykepengehistorikk.aktørId).also { person ->
            person.håndterSykepengehistorikk(sykepengehistorikk)
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
