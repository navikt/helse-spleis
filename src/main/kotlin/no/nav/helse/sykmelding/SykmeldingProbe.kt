package no.nav.helse.sykmelding

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

class SykmeldingProbe {
    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingProbe::class.java)

        private val sykmeldingCounter = Counter.build("sykmeldinger_totals", "Antall sykmeldinger mottatt")
                .register()
    }

    fun mottattSykmelding(key: String, sykmelding: JsonNode) {
        log.info("mottok sykmelding med key=$key")
        sykmeldingCounter.inc()
    }
}
