package no.nav.helse.sykmelding

import io.prometheus.client.Counter
import no.nav.helse.sykmelding.domain.Sykmelding
import org.slf4j.LoggerFactory

class SykmeldingProbe {
    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingProbe::class.java)

        private val sykmeldingCounter = Counter.build("sykmeldinger_totals", "Antall sykmeldinger mottatt")
                .register()
    }

    fun mottattSykmelding(sykmelding: Sykmelding) {
        log.info("mottok sykmelding med id=${sykmelding.id}")
        sykmeldingCounter.inc()
    }
}
