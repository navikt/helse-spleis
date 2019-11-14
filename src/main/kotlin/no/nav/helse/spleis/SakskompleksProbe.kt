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

    private val utenforOmfangCounter = Counter.build("utenfor_omfang_totals", "Antall ganger en sak er utenfor omfang")
            .labelNames("dokumentType")
            .register()

    private val personMementoStørrelse = Summary.build("person_memento_size", "størrelse på person document i databasen").register()

    override fun sakskompleksTrengerLøsning(event: Behov) {
        behovCounter.labels(event.behovType()).inc()
    }

    override fun personEndret(personEndretEvent: PersonObserver.PersonEndretEvent) {
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
        utenforOmfangCounter.labels(nySøknadHendelse.javaClass.simpleName).inc()
    }

    fun utenforOmfang(err: UtenforOmfangException, sendtSøknadHendelse: SendtSøknadHendelse) {
        utenforOmfangCounter.labels(sendtSøknadHendelse.javaClass.simpleName).inc()
    }

    fun utenforOmfang(err: UtenforOmfangException, inntektsmeldingHendelse: InntektsmeldingHendelse) {
        utenforOmfangCounter.labels(inntektsmeldingHendelse.javaClass.simpleName).inc()
    }
}
