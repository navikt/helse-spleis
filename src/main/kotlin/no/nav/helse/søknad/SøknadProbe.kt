package no.nav.helse.søknad

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

class SøknadProbe {

    companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        private val søknadCounterName = "nye_soknader_totals"
        private val søknaderIgnorertCounterName = "soknader_ignorert_totals"

        private val søknadCounter = Counter.build(søknadCounterName, "Antall søknader mottatt")
                .labelNames("status")
                .register()

        private val søknadIgnorertCounter = Counter.build(søknaderIgnorertCounterName, "Antall søknader vi ignorerer")
                .register()
    }

    fun mottattSøknad(søknad: Sykepengesøknad) {
        sikkerLogg.info(søknad.toJson().toString())
        søknadCounter.labels(søknad.status).inc()
    }

    fun søknadIgnorert(id: String, type: String, status: String) {
        søknadIgnorertCounter.inc()
    }
}
