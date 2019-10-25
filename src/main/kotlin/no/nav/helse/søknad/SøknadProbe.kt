package no.nav.helse.søknad

import io.prometheus.client.Counter
import org.slf4j.LoggerFactory

class SøknadProbe {

    companion object {
        private val log = LoggerFactory.getLogger(SøknadProbe::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        val søknadCounterName = "nye_soknader_totals"
        val søknaderIgnorertCounterName = "soknader_ignorert_totals"

        private val søknadCounter = Counter.build(søknadCounterName, "Antall søknader mottatt")
                .labelNames("status")
                .register()

        private val søknadIgnorertCounter = Counter.build(søknaderIgnorertCounterName, "Antall søknader vi ignorerer")
                .register()
    }

    fun mottattSøknad(søknad: Sykepengesøknad) {
        log.info("mottok søknad med id=${søknad.id} for sykmelding=${søknad.sykmeldingId}")
        sikkerLogg.info(søknad.toJson().toString())
        søknadCounter.labels(søknad.status).inc()
    }

    fun søknadIgnorert(id: String, type: String, status: String) {
        log.info("mottok søknad med id=$id av type=$type med status=$status som vi ignorerer")
        søknadIgnorertCounter.inc()
    }
}
