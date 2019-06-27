package no.nav.helse.søknad

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

class SøknadProbe {

    companion object {
        private val log = LoggerFactory.getLogger(SøknadProbe::class.java)

        private val søknadCounter = Counter.build("soknader_totals", "Antall søknader mottatt")
                .register()
    }

    fun mottattSøknad(key: String, søknad: JsonNode) {
        log.info("mottok søknad med key=$key")
        søknadCounter.inc()
    }
}
