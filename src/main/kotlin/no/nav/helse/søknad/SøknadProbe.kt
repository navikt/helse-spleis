package no.nav.helse.søknad

import io.prometheus.client.Counter
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.slf4j.LoggerFactory

class SøknadProbe {

    companion object {
        private val log = LoggerFactory.getLogger(SøknadProbe::class.java)

        private val søknadCounter = Counter.build("soknader_totals", "Antall søknader mottatt")
                .register()
    }

    fun mottattSøknad(søknad: Sykepengesøknad) {
        log.info("mottok søknad med id=${søknad.id} for sykmelding=${søknad.sykmeldingId}")
        søknadCounter.inc()
    }
}
