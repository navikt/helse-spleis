package no.nav.helse.sakskompleks

import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.sykmelding.domain.Sykmelding
import no.nav.helse.søknad.domain.Sykepengesøknad
import java.util.*

class SakskompleksService(private val sakskompleksDao: SakskompleksDao) {

    fun finnSak(sykepengesøknad: Sykepengesøknad) =
            sakskompleksDao.finnSaker(sykepengesøknad.aktørId)
                    .finnSak(sykepengesøknad)

    fun finnSak(sykmelding: Sykmelding) =
            sakskompleksDao.finnSaker(sykmelding.aktørId)
                    .finnSak(sykmelding)

    fun oppdaterSak(sakskompleks: Sakskompleks) {
        sakskompleksDao.oppdaterSak(sakskompleks)
    }

    fun finnEllerOpprettSak(sykmelding: Sykmelding) =
            finnSak(sykmelding)?.let { sak ->
                sak.copy(
                        sykmeldinger = sak.sykmeldinger + sykmelding
                )
            }?.also { sak ->
                sakskompleksDao.oppdaterSak(sak)
            } ?: nyttSakskompleks(sykmelding).also { sakskompleksDao.opprettSak(it) }

    private fun nyttSakskompleks(sykmelding: Sykmelding) =
            Sakskompleks(
                    id = UUID.randomUUID(),
                    aktørId = sykmelding.aktørId,
                    sykmeldinger = listOf(sykmelding),
                    søknader = emptyList()
            )

    private fun List<Sakskompleks>.finnSak(sykepengesøknad: Sykepengesøknad) =
            firstOrNull { sakskompleks ->
                sykepengesøknad.hørerSammenMed(sakskompleks)
            }

    private fun List<Sakskompleks>.finnSak(sykmelding: Sykmelding) =
            firstOrNull { sakskompleks ->
                sykmelding.hørerSammenMed(sakskompleks)
            }

    private fun Sykepengesøknad.hørerSammenMed(sakskompleks: Sakskompleks) =
            sakskompleks.sykmeldinger.any { sykmelding ->
                sykmelding.id == sykmeldingId
            }

    private fun Sykmelding.hørerSammenMed(sakskompleks: Sakskompleks) =
            sakskompleks.sykmeldinger.any { sykmelding ->
                sykmelding.id == id
            }
}
