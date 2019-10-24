package no.nav.helse.sakskompleks

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import no.nav.helse.hendelse.InntektsmeldingHendelse
import no.nav.helse.hendelse.NySøknadHendelse
import no.nav.helse.hendelse.SendtSøknadHendelse
import no.nav.helse.person.domain.PersonObserver
import no.nav.helse.person.domain.PersonskjemaForGammelt
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
        val personMementoSize = "personMementoSize"


        private val sakskompleksCounter = Counter.build(sakskompleksTotalsCounterName, "Antall sakskompleks opprettet")
                .register()

        private val dokumenterKobletTilSakCounter = Counter.build(dokumenterKobletTilSakCounterName, "Antall inntektsmeldinger vi har mottatt som ble koblet til et sakskompleks")
                .labelNames("dokumentType")
                .register()

        private val manglendeSakskompleksForInntektsmeldingCounter = Counter.build(manglendeSakskompleksForInntektsmeldingCounterName, "Antall inntektsmeldinger vi har mottatt som vi ikke klarer å koble til et sakskompleks")
                .register()
        private val personMementoStørrelse = Summary.build(personMementoSize, "størrelse på person document i databasen")
                .quantile(0.5, 0.05)
                .quantile(0.75, 0.1)
                .quantile(0.9, 0.01)
                .quantile(0.99, 0.001).register()
    }

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        log.info("lagret person med størrelse ${personEndretEvent.memento.toString().length} bytes")
        personMementoStørrelse.observe(personEndretEvent.memento.toString().length.toDouble())
    }

    fun forGammelSkjemaversjon(err: PersonskjemaForGammelt) {
        log.info(err.message)
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
            is InntektsmeldingHendelse -> {
                inntektsmeldingKobletTilSakskompleks(event.id)
            }
            is NySøknadHendelse -> {
                if (event.previousState == Sakskompleks.TilstandType.START) {
                    opprettetNyttSakskompleks(event.id, event.aktørId)
                }

                sykmeldingKobletTilSakskompleks(event.id)
            }
            is SendtSøknadHendelse -> {
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

    fun utenforOmfang(err: UtenforOmfangException, nySøknadHendelse: NySøknadHendelse) {
        log.info("Utenfor omfang: ${err.message} for nySøknadHendelse med id: ${nySøknadHendelse.hendelseId()}.")
    }

    fun utenforOmfang(err: UtenforOmfangException, sendtSøknadHendelse: SendtSøknadHendelse) {
        log.info("Utenfor omfang: ${err.message} for sendtSøknadHendelse med id: ${sendtSøknadHendelse.hendelseId()}.")
    }

    fun utenforOmfang(err: UtenforOmfangException, inntektsmeldingHendelse: InntektsmeldingHendelse) {
        log.info("Utenfor omfang: ${err.message} for inntektsmeldingHendelse med id: ${inntektsmeldingHendelse.hendelseId()}.")
    }
}
