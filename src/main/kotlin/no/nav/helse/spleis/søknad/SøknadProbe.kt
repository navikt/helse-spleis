package no.nav.helse.spleis.søknad

import io.prometheus.client.Counter
import no.nav.helse.person.hendelser.søknad.Sykepengesøknad
import org.slf4j.LoggerFactory

object SøknadProbe {

    private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

    private val søknadCounterName = "nye_soknader_totals"
    private val søknaderIgnorertCounterName = "soknader_ignorert_totals"

    private val søknadCounter = Counter.build(søknadCounterName, "Antall søknader mottatt")
            .labelNames("status")
            .register()

    private val søknadIgnorertCounter = Counter.build(søknaderIgnorertCounterName, "Antall søknader vi ignorerer")
            .register()

    fun mottattSøknad(søknad: Sykepengesøknad) {
        sikkerLogg.info(søknad.toJson().toString())
        søknadCounter.labels(søknad.status).inc()
    }

    fun søknadIgnorert(id: String, type: String, status: String) {
        søknadIgnorertCounter.inc()
    }
}
