package no.nav.helse.sakskompleks

import io.prometheus.client.Counter
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.slf4j.LoggerFactory

class SakskompleksProbe: Sakskompleks.Observer {

    companion object {
        private val log = LoggerFactory.getLogger(SakskompleksProbe::class.java)

        val sakskompleksTotalsCounterName = "sakskompleks_totals"
        val søknadUtenSakskompleksCounterName = "manglende_sakskompleks_totals"
        val innteksmeldingKobletTilSakCounterName = "inntektsmelding_koblet_til_sak_totals"
        val manglendeSakskompleksForInntektsmeldingCounterName = "manglende_sakskompleks_for_inntektsmelding_totals"

        private val sakskompleksCounter = Counter.build(sakskompleksTotalsCounterName, "Antall sakskompleks opprettet")
                .register()

        private val manglendeSakskompleksCounter = Counter.build(søknadUtenSakskompleksCounterName, "Antall søknader vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()

        private val inntektsmeldingKobletTilSakCounter = Counter.build(innteksmeldingKobletTilSakCounterName, "Antall inntektsmeldinger vi har mottatt som ble koblet til et sakskompleks")
                .register()

        private val manglendeSakskompleksForInntektsmeldingCounter = Counter.build(manglendeSakskompleksForInntektsmeldingCounterName, "Antall inntektsmeldinger vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()
    }

    fun opprettetNyttSakskompleks(sakskompleks: Sakskompleks) {
        log.info("Opprettet sakskompleks med id=${sakskompleks.id()} " +
                "for arbeidstaker med aktørId = ${sakskompleks.aktørId()} ")
        sakskompleksCounter.inc()
    }

    fun søknadKobletTilSakskompleks(søknad: Sykepengesøknad, sakskompleks: Sakskompleks) {
        log.info("søknad med id=${søknad.id} ble koblet til sakskompleks med id=${sakskompleks.id()}")
    }

    fun søknadManglerSakskompleks(søknad: Sykepengesøknad) {
        log.error("mottok søknad med id=${søknad.id}, men vi har ikke et eksisterende sakskompleks. Vi burde ha et sakskompleks som inneholder en sykmeldingsId=${søknad.sykmeldingId}")
        manglendeSakskompleksCounter.inc()
    }

    fun inntektsmeldingKobletTilSakskompleks(inntektsmelding: Inntektsmelding, sak: Sakskompleks) {
        log.info("Inntektsmelding med id ${inntektsmelding.inntektsmeldingId} ble koblet til sakskompleks med id ${sak.id()}")
        inntektsmeldingKobletTilSakCounter.inc()
    }

    fun inntektmeldingManglerSakskompleks(inntektsmelding: Inntektsmelding) {
        log.error("Mottok inntektsmelding med id ${inntektsmelding.inntektsmeldingId}, men klarte ikke finne et et tilhørende sakskompleks :(")
        manglendeSakskompleksForInntektsmeldingCounter.inc()
    }

    override fun stateChange(event: Sakskompleks.Observer.Event) {
        when (event.type) {
            is Sakskompleks.Observer.Event.Type.LeavingState -> {
                log.info("sakskompleks går ut av tilstand ${event.currentState.tilstand}")
            }
            is Sakskompleks.Observer.Event.Type.StateChange -> {
                log.info("sakskompleks går fra state=${event.oldState?.tilstand} til state=${event.currentState.tilstand}")
            }
            is Sakskompleks.Observer.Event.Type.EnteringState -> {
                log.info("sakskompleks gått inn i ny tilstand ${event.currentState.tilstand}")
            }
        }
    }
}
