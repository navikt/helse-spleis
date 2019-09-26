package no.nav.helse.søknad

import io.prometheus.client.Counter
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.slf4j.LoggerFactory

class SøknadProbe {

    companion object {
        private val log = LoggerFactory.getLogger(SøknadProbe::class.java)

        val søknadCounterName = "soknader_totals"
        val søknadUtenSakskompleksCounterName = "manglende_sakskompleks_totals"
        val søknaderIgnorertCounterName = "soknader_ignorert_totals"

        private val søknadCounter = Counter.build(søknadCounterName, "Antall søknader mottatt")
                .register()

        private val manglendeSakskompleksCounter = Counter.build(søknadUtenSakskompleksCounterName, "Antall søknader vi har mottatt som vi ikke klarer å koble til et sakskompleks")
            .register()

        private val søknadIgnorertCounter = Counter.build(søknaderIgnorertCounterName, "Antall søknader vi ignorerer")
            .register()
    }

    fun søknadKobletTilSakskompleks(søknad: Sykepengesøknad, sakskompleks: Sakskompleks) {
        log.info("søknad med id=${søknad.id} ble koblet til sakskompleks med id=${sakskompleks.id()}")
    }

    fun mottattSøknad(søknad: Sykepengesøknad) {
        log.info("mottok søknad med id=${søknad.id} for sykmelding=${søknad.sykmeldingId}")
        søknadCounter.inc()
    }

    fun søknadIgnorert(id: String, type: String, status: String) {
        log.info("mottok søknad med id=$id av type=$type med status=$status som vi ignorerer")
        søknadIgnorertCounter.inc()
    }

    fun søknadManglerSakskompleks(søknad: Sykepengesøknad) {
        log.error("mottok søknad med id=${søknad.id}, men vi har ikke et eksisterende sakskompleks. Vi burde ha et sakskompleks som inneholder en sykmeldingsId=${søknad.sykmeldingId}")
        manglendeSakskompleksCounter.inc()
    }
}
