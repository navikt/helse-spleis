package no.nav.helse.sakskompleks

import no.nav.helse.behov.BehovProducer
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.person.domain.Person
import no.nav.helse.person.domain.UtenforOmfangException
import java.lang.Integer.max
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal class SakskompleksService(private val behovProducer: BehovProducer,
                                   private val sakskompleksDao: SakskompleksDao,
                                   private val sakskompleksProbe: SakskompleksProbe = SakskompleksProbe()) {

    fun håndterNySøknad(sykepengesøknad: NySykepengesøknad) =
            try {
                (finnPerson(sykepengesøknad.aktørId) ?: nyPerson(sykepengesøknad.aktørId))
                        .also { person ->
                            person.addObserver(sakskompleksProbe)
                            person.håndterNySøknad(sykepengesøknad)
                            behovProducer.nyttBehov("sykepengeperioder", mapOf(
                                    "aktørId" to sykepengesøknad.aktørId
                            ))
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

/* Siden lørdag og søndag er tradisjonelle fridager, regner vi at arbeidet ble gjenopptatt på mandag når vi
* teller kalenderdager mellom to sykmeldinger. Se rundskriv #8-19 4.ledd */
fun kalenderdagerMellomMinusHelg(fom: LocalDate, tom: LocalDate): Int {
    val antallDager = max(ChronoUnit.DAYS.between(fom, tom).toInt() - 1, 0)
    return when (fom.dayOfWeek) {
        DayOfWeek.FRIDAY -> max(antallDager - 2, 0)
        else -> antallDager
    }
}
