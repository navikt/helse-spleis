package no.nav.helse.sakskompleks

import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.søknad.domain.Sykepengesøknad

class SakskompleksService(private val sakskompleksDao: SakskompleksDao) {

    fun finnSak(sykepengesøknad: Sykepengesøknad) =
            sakskompleksDao.finnSaker(sykepengesøknad.aktørId)
                    .finnSak(sykepengesøknad)

    private fun List<Sakskompleks>.finnSak(sykepengesøknad: Sykepengesøknad) =
            firstOrNull { sakskompleks ->
                sykepengesøknad.hørerSammenMed(sakskompleks)
            }

    private fun Sykepengesøknad.hørerSammenMed(sakskompleks: Sakskompleks) =
            sakskompleks.søknader.any { søknad ->
                søknad.id == id
            }
}
