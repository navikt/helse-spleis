package no.nav.helse.sakskompleks

import io.prometheus.client.Counter
import no.nav.helse.inntektsmelding.domain.Inntektsmelding
import no.nav.helse.sakskompleks.domain.Sakskompleks
import no.nav.helse.søknad.domain.Sykepengesøknad
import org.slf4j.LoggerFactory
import java.util.*

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
        inntektsmeldingKobletTilSakCounter.inc()
    }

    override fun stateChange(event: Sakskompleks.Observer.Event) {
        log.info("sakskompleks=${event.id} event=${event.eventName} state=${event.currentState} previousState=${event.previousState}")

        /*when (event.eventName) {
            "Inntektsmelding" -> {
                inntektsmeldingKobletTilSakskompleks(event.id)
            }
            "Sykmelding" -> {
                when (event.previousType) {
                    is Sakskompleks.StateName.StartTilstand -> {
                        opprettetNyttSakskompleks(event.id, event.aktørId)
                    }
                    else -> {
                        sykmeldingKobletTilSakskompleks(event.id)
                    }
                }
            }
            "Sykepengesøknad" -> {
                søknadKobletTilSakskompleks(event.id)
            }
        }

        is Sakskompleks.StateName.KomplettSak -> {
            log.info("sakskompleks med id ${event.id} er regnet som en komplett sak")
        }
        is Sakskompleks.StateName.TrengerManuellHåndtering -> {
            log.info("sakskompleks med id ${event.id} trenger manuell behandling")
        }*/
    }
}
