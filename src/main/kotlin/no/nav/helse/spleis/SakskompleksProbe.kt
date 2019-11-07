package no.nav.helse.spleis

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import no.nav.helse.behov.Behov
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonskjemaForGammelt
import no.nav.helse.person.SakskompleksObserver.StateChangeEvent
import no.nav.helse.person.UtenforOmfangException
import no.nav.helse.person.hendelser.SykdomshendelseType
import no.nav.helse.person.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.person.hendelser.søknad.NySøknadHendelse
import no.nav.helse.person.hendelser.søknad.SendtSøknadHendelse
import org.slf4j.LoggerFactory

object SakskompleksProbe : PersonObserver {

    private val log = LoggerFactory.getLogger(SakskompleksProbe::class.java)

    private val behovCounter = Counter.build("behov_totals", "Antall behov opprettet")
            .labelNames("behovType")
            .register()

    private val dokumenterKobletTilSakCounter = Counter.build("dokumenter_koblet_til_sak_totals", "Antall inntektsmeldinger vi har mottatt som ble koblet til et sakskompleks")
            .labelNames("dokumentType")
            .register()

    private val tilstandCounter = Counter.build("sakskompleks_tilstander_totals", "Fordeling av tilstandene sakene er i, og hvilken tilstand de kom fra")
            .labelNames("forrigeTilstand", "tilstand", "hendelse")
            .register()

    private val personMementoStørrelse = Summary.build("personMementoSize", "størrelse på person document i databasen")
            .quantile(0.5, 0.05)
            .quantile(0.75, 0.1)
            .quantile(0.9, 0.01)
            .quantile(0.99, 0.001).register()

    override fun sakskompleksTrengerLøsning(event: Behov) {
        behovCounter.labels(event.behovType()).inc()
    }

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
        log.info("lagret person med størrelse ${personEndretEvent.memento.toString().length} bytes")
        personMementoStørrelse.observe(personEndretEvent.memento.toString().length.toDouble())
    }

    fun forGammelSkjemaversjon(err: PersonskjemaForGammelt) {
        log.info(err.message)
    }

    override fun sakskompleksEndret(event: StateChangeEvent) {
        tilstandCounter.labels(event.previousState.name, event.currentState.name, event.sykdomshendelse.javaClass.simpleName).inc()

        log.info("sakskompleks=${event.id} event=${event.sykdomshendelse.javaClass.simpleName} state=${event.currentState} previousState=${event.previousState}")

        when (event.sykdomshendelse) {
            is InntektsmeldingHendelse -> {
                dokumenterKobletTilSakCounter.labels(SykdomshendelseType.InntektsmeldingMottatt.name).inc()
            }
            is NySøknadHendelse -> {
                dokumenterKobletTilSakCounter.labels(SykdomshendelseType.NySøknadMottatt.name).inc()
            }
            is SendtSøknadHendelse -> {
                dokumenterKobletTilSakCounter.labels(SykdomshendelseType.SendtSøknadMottatt.name).inc()
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
