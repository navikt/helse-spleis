package no.nav.helse.sakskompleks

import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.søknad.domain.Sykepengesøknad

class SakskompleksService(private val sakskompleksDao: SakskompleksDao) {

    fun finnSakSøknad(sykepengesøknad: Sykepengesøknad) =
            sakskompleksDao.finnSaker(sykepengesøknad.aktørId)
                    .firstOrNull { sakskompleks ->
                        sykepengesøknad.hørerSammenMed(sakskompleks)
                    }

    private fun Sykepengesøknad.hørerSammenMed(sakskompleks: Sakskompleks) =
            sakskompleks.søknader.any { søknad ->
                søknad.id == id
            }
}
