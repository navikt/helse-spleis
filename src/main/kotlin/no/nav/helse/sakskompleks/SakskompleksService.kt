package no.nav.helse.sakskompleks

import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.sykmelding.domain.gjelderFra
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.lang.Integer.max
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class SakskompleksService(private val sakskompleksDao: SakskompleksDao) {

    private val sakskompleksProbe = SakskompleksProbe()

    fun finnEllerOpprettSak(sykmelding: Sykmelding) =
            (finnSak(sykmelding) ?: sakskompleksDao.opprettSak(sykmelding.aktørId))
                    .also { sakskompleks ->
                        sakskompleks.addObserver(sakskompleksProbe)
                        sakskompleks.leggTil(sykmelding)
                    }

    fun knyttSøknadTilSak(sykepengesøknad: Sykepengesøknad) =
            finnSak(sykepengesøknad)?.also { sakskompleks ->
                sakskompleks.addObserver(sakskompleksProbe)
                leggSøknadPåSak(sakskompleks, sykepengesøknad)
            }.also {
                if (it == null) {
                    sakskompleksProbe.søknadManglerSakskompleks(sykepengesøknad)
                }
            }

    fun knyttInntektsmeldingTilSak(inntektsmelding: Inntektsmelding) =
            finnSak(inntektsmelding)?.also { sakskompleks ->
                sakskompleks.addObserver(sakskompleksProbe)
                leggInntektsmeldingPåSak(sakskompleks, inntektsmelding)
            }.also {
                if (it == null) {
                    sakskompleksProbe.inntektmeldingManglerSakskompleks(inntektsmelding)
                }
            }

    private fun finnSak(sykepengesøknad: Sykepengesøknad) =
        sakskompleksDao.finnSaker(sykepengesøknad.aktørId)
            .finnSak(sykepengesøknad)

    private fun finnSak(inntektsmelding: Inntektsmelding) =
            sakskompleksDao.finnSaker(inntektsmelding.arbeidstakerAktorId)
                    .finnSak(inntektsmelding)

    private fun finnSak(sykmelding: Sykmelding) =
        sakskompleksDao.finnSaker(sykmelding.aktørId)
            .finnSak(sykmelding)

    private fun leggSøknadPåSak(sak: Sakskompleks, søknad: Sykepengesøknad) {
        sak.leggTil(søknad)
    }

    private fun leggInntektsmeldingPåSak(sak: Sakskompleks, inntektsmelding: Inntektsmelding) {
        sak.leggTil(inntektsmelding)
    }

    private fun List<Sakskompleks>.finnSak(sykepengesøknad: Sykepengesøknad) =
        firstOrNull { sakskompleks ->
            sakskompleks.hørerSammenMed(sykepengesøknad)
        }

    private fun List<Sakskompleks>.finnSak(sykmelding: Sykmelding) =
        firstOrNull { sakskompleks ->
            sykmelding.hørerSammenMed(sakskompleks)
        }

    private fun List<Sakskompleks>.finnSak(inntektsmelding: Inntektsmelding) =
        firstOrNull { sakskompleks ->
            inntektsmelding.hørerSammenMed(sakskompleks)
        }

    private fun Sykmelding.hørerSammenMed(sakskompleks: Sakskompleks) =
        kalenderdagerMellomMinusHelg(sakskompleks.tom(), gjelderFra()) < 16

    private fun Inntektsmelding.hørerSammenMed(sakskompleks: Sakskompleks): Boolean {
        val saksPeriode = sakskompleks.fom()?.rangeTo(sakskompleks.tom())
        return sisteDagIArbeidsgiverPeriode?.let { saksPeriode?.contains(it) } ?: false
    }
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
