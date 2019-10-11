package no.nav.helse.person

import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.UtenforOmfangException
import no.nav.helse.sakskompleks.SakskompleksProbe

internal class PersonMediator(private val personRepository: PersonRepository,
                              private val behovProducer: BehovProducer,
                              private val sakskompleksProbe: SakskompleksProbe = SakskompleksProbe()) : PersonObserver{

    override fun personEndret(person: Person) {
        personRepository.lagrePerson(person)
    }

    fun håndterNySøknad(sykepengesøknad: NySykepengesøknad) =
            try {
                (finnPerson(sykepengesøknad.aktørId) ?: nyPerson(sykepengesøknad.aktørId))
                        .also { person ->
                            person.addObserver(sakskompleksProbe)
                            person.addObserver(this)
                            person.håndterNySøknad(sykepengesøknad)
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, sykepengesøknad)
            }

    fun håndterSendtSøknad(sykepengesøknad: SendtSykepengesøknad) =
            try {
                (finnPerson(sykepengesøknad.aktørId) ?: nyPerson(sykepengesøknad.aktørId))
                        .also { person ->
                            person.addObserver(sakskompleksProbe)
                            person.håndterSendtSøknad(sykepengesøknad)
                            behovProducer.nyttBehov("sykepengeperioder", mapOf(
                                    "aktørId" to sykepengesøknad.aktørId
                            ))
                        }
            } catch (err: UtenforOmfangException) {
                sakskompleksProbe.utenforOmfang(err, sykepengesøknad)
            }

    fun håndterInntektsmelding(inntektsmelding: Inntektsmelding) =
            finnPerson(inntektsmelding.aktørId())?.also { person ->
                person.addObserver(sakskompleksProbe)
                person.håndterInntektsmelding(inntektsmelding)
            }.also {
                if (it == null) {
                    sakskompleksProbe.inntektmeldingManglerSakskompleks(inntektsmelding)
                }
            }

    private fun nyPerson(aktørId: String) = Person(aktørId = aktørId)

    private fun finnPerson(aktørId: String): Person? = null

}
