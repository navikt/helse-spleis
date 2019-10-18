package no.nav.helse.sakskompleks

import io.prometheus.client.Counter
import no.nav.helse.hendelse.Inntektsmelding
import no.nav.helse.hendelse.NySykepengesøknad
import no.nav.helse.hendelse.SendtSykepengesøknad
import no.nav.helse.hendelse.Sykepengesøknad
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.Sakskompleks
import no.nav.helse.person.domain.SakskompleksObserver.StateChangeEvent
import no.nav.helse.person.domain.UtenforOmfangException
import org.slf4j.LoggerFactory
import java.util.*

class SakskompleksProbe : PersonObserver {

    companion object {
        private val log = LoggerFactory.getLogger(SakskompleksProbe::class.java)

        val sakskompleksTotalsCounterName = "sakskompleks_totals"
        val dokumenterKobletTilSakCounterName = "dokumenter_koblet_til_sak_totals"
        val manglendeSakskompleksForInntektsmeldingCounterName = "manglende_sakskompleks_for_inntektsmelding_totals"

        private val sakskompleksCounter = Counter.build(sakskompleksTotalsCounterName, "Antall sakskompleks opprettet")
                .register()

        private val dokumenterKobletTilSakCounter = Counter.build(dokumenterKobletTilSakCounterName, "Antall inntektsmeldinger vi har mottatt som ble koblet til et sakskompleks")
                .labelNames("dokumentType")
                .register()

        private val manglendeSakskompleksForInntektsmeldingCounter = Counter.build(manglendeSakskompleksForInntektsmeldingCounterName, "Antall inntektsmeldinger vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()
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

    override fun sakskompleksEndret(event: StateChangeEvent) {
        log.info("sakskompleks=${event.id} event=${event.sykdomshendelse.hendelsetype().name} state=${event.currentState} previousState=${event.previousState}")

        dokumenterKobletTilSakCounter.labels(event.sykdomshendelse.hendelsetype().name).inc()

        when (event.sykdomshendelse) {
            is Inntektsmelding -> {
                inntektsmeldingKobletTilSakskompleks(event.id)
            }
            is NySykepengesøknad -> {
                if (event.previousState == Sakskompleks.TilstandType.START) {
                    opprettetNyttSakskompleks(event.id, event.aktørId)
                }

                sykmeldingKobletTilSakskompleks(event.id)
            }
            is SendtSykepengesøknad -> {
                søknadKobletTilSakskompleks(event.id)
            }
        }

        when (event.previousState) {
            Sakskompleks.TilstandType.KOMPLETT_SAK -> {
                log.info("sakskompleks med id ${event.id} er regnet som en komplett sak")
            }
            Sakskompleks.TilstandType.SKAL_TIL_INFOTRYGD -> {
                log.info("sakskompleks med id ${event.id} må behandles i infotrygd")
            }
        }
    }

    fun utenforOmfang(err: UtenforOmfangException, sykepengesøknad: Sykepengesøknad) {
        log.info("Utenfor omfang: ${err.message} for søknad med id: ${sykepengesøknad.id}.")
    }

    fun utenforOmfang(err: UtenforOmfangException, inntektsmelding: Inntektsmelding) {
        log.info("Utenfor omfang: ${err.message} for inntektsmelding med id: ${inntektsmelding.inntektsmeldingId}.")
    }
}
