package no.nav.helse.sakskompleks

import io.prometheus.client.Counter
import no.nav.helse.Event
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.domain.SakskompleksObserver
import no.nav.helse.sakskompleks.domain.SakskompleksObserver.StateChangeEvent
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.slf4j.LoggerFactory
import java.util.*

class SakskompleksProbe: SakskompleksObserver {

    companion object {
        private val log = LoggerFactory.getLogger(SakskompleksProbe::class.java)

        val sakskompleksTotalsCounterName = "sakskompleks_totals"
        val søknadUtenSakskompleksCounterName = "manglende_sakskompleks_totals"
        val dokumenterKobletTilSakCounterName = "dokumenter_koblet_til_sak_totals"
        val manglendeSakskompleksForInntektsmeldingCounterName = "manglende_sakskompleks_for_inntektsmelding_totals"

        private val sakskompleksCounter = Counter.build(sakskompleksTotalsCounterName, "Antall sakskompleks opprettet")
                .register()

        private val manglendeSakskompleksCounter = Counter.build(søknadUtenSakskompleksCounterName, "Antall søknader vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()

        private val dokumenterKobletTilSakCounter = Counter.build(dokumenterKobletTilSakCounterName, "Antall inntektsmeldinger vi har mottatt som ble koblet til et sakskompleks")
                .labelNames("dokumentType")
                .register()

        private val manglendeSakskompleksForInntektsmeldingCounter = Counter.build(manglendeSakskompleksForInntektsmeldingCounterName, "Antall inntektsmeldinger vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()
    }

    fun søknadManglerSakskompleks(søknad: Sykepengesøknad) {
        log.error("mottok søknad med id=${søknad.id}, men vi har ikke et eksisterende sakskompleks. Vi burde ha et sakskompleks som inneholder en sykmeldingsId=${søknad.sykmeldingId}")
        manglendeSakskompleksCounter.inc()
    }

    fun inntektmeldingManglerSakskompleks(inntektsmelding: Inntektsmelding) {
        log.error("Mottok inntektsmelding med id ${inntektsmelding.inntektsmeldingId}, men klarte ikke finne et et tilhørende sakskompleks :(")
        manglendeSakskompleksForInntektsmeldingCounter.inc()
    }

    private fun opprettetNyttSakskompleks(sakskompleksId: UUID, aktørId: String) {
        log.info("Opprettet sakskompleks med id=$sakskompleksId " +
                "for arbeidstaker med aktørId = $aktørId ")
        sakskompleksCounter.inc()
    }

    private fun søknadKobletTilSakskompleks(sakskompleksId: UUID) {
        log.info("sakskompleks med id $sakskompleksId har blitt oppdatert med en søknad")
    }

    private fun sykmeldingKobletTilSakskompleks(sakskompleksId: UUID) {
        log.info("sakskompleks med id $sakskompleksId har blitt oppdatert med en sykmelding")
    }

    private fun inntektsmeldingKobletTilSakskompleks(sakskompleksId: UUID) {
        log.info("sakskompleks med id $sakskompleksId har blitt oppdatert med en inntektsmelding")
    }

    override fun sakskompleksChanged(event: StateChangeEvent) {
        log.info("sakskompleks=${event.id} event=${event.eventName} state=${event.currentState} previousState=${event.previousState}")

        dokumenterKobletTilSakCounter.labels(event.eventName.name).inc()

        when (event.eventName) {
            Event.Type.Inntektsmelding -> {
                inntektsmeldingKobletTilSakskompleks(event.id)
            }
            Event.Type.Sykmelding -> {
                if (event.previousState == "StartTilstand") {
                    opprettetNyttSakskompleks(event.id, event.aktørId)
                }

                sykmeldingKobletTilSakskompleks(event.id)
            }
            Event.Type.Sykepengesøknad -> {
                søknadKobletTilSakskompleks(event.id)
            }
        }

        when (event.previousState) {
            "KomplettSakTilstand" -> {
                log.info("sakskompleks med id ${event.id} er regnet som en komplett sak")
            }
            "TrengerManuellHåndteringTilstand" -> {
                log.info("sakskompleks med id ${event.id} trenger manuell behandling")
            }
        }
    }
}
