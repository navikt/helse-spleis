package no.nav.helse.sakskompleks

import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.søknad.domain.Sykepengesøknad

class SakskompleksService(private val sakskompleksDao: SakskompleksDao) {

    fun finnSak(sykepengesøknad: Sykepengesøknad) =
            sakskompleksDao.finnSaker(sykepengesøknad.aktørId)
                    .finnSak(sykepengesøknad)

    fun finnSak(sykmelding: Sykmelding) =
            sakskompleksDao.finnSaker(sykmelding.aktørId)
                    .finnSak(sykmelding)

    private fun List<Sakskompleks>.finnSak(sykepengesøknad: Sykepengesøknad) =
            firstOrNull { sakskompleks ->
                sykepengesøknad.hørerSammenMed(sakskompleks)
            }

    private fun List<Sakskompleks>.finnSak(sykmelding: Sykmelding) =
            firstOrNull { sakskompleks ->
                sykmelding.hørerSammenMed(sakskompleks)
            }

    private fun Sykepengesøknad.hørerSammenMed(sakskompleks: Sakskompleks) =
            sakskompleks.søknader.any { søknad ->
                søknad.id == id
            }

    private fun Sykmelding.hørerSammenMed(sakskompleks: Sakskompleks) =
            sakskompleks.sykmeldinger.any { sykmelding ->
                sykmelding.id == id
            }
}
