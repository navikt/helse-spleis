package no.nav.helse.sakskompleks

import io.prometheus.client.Counter
import no.nav.helse.sakskompleks.domain.Sakskompleks
import org.slf4j.LoggerFactory

class SakskompleksProbe {

    companion object {
        private val log = LoggerFactory.getLogger(SakskompleksProbe::class.java)

        val sakskompleksTotalsCounterName = "sakskompleks_totals"

        private val sakskompleksCounter = Counter.build(sakskompleksTotalsCounterName, "Antall sakskompleks opprettet")
                .register()
    }

    fun opprettetNyttSakskompleks(sakskompleks: Sakskompleks) {
        log.info("Opprettet sakskompleks med id=${sakskompleks.id} " +
                "for arbeidstaker med aktørId = ${sakskompleks.aktørId} ")
        sakskompleksCounter.inc()
    }

}