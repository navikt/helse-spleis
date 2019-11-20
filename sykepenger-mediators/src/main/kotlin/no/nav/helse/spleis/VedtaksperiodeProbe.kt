package no.nav.helse.spleis

import io.prometheus.client.Counter
import io.prometheus.client.Summary
import no.nav.helse.behov.Behov
import no.nav.helse.sak.SakObserver
import no.nav.helse.sak.SakskjemaForGammelt
import no.nav.helse.sak.VedtaksperiodeObserver.StateChangeEvent
import no.nav.helse.hendelser.SykdomshendelseType
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import org.slf4j.LoggerFactory

object VedtaksperiodeProbe : SakObserver {

    private val log = LoggerFactory.getLogger(VedtaksperiodeProbe::class.java)

    private val behovCounter = Counter.build("behov_totals", "Antall behov opprettet")
            .labelNames("behovType")
            .register()

    private val dokumenterKobletTilSakCounter = Counter.build("dokumenter_koblet_til_sak_totals", "Antall inntektsmeldinger vi har mottatt som ble koblet til et vedtaksperiode")
            .labelNames("dokumentType")
            .register()

    private val tilstandCounter = Counter.build("vedtaksperiode_tilstander_totals", "Fordeling av tilstandene sakene er i, og hvilken tilstand de kom fra")
            .labelNames("forrigeTilstand", "tilstand", "hendelse")
            .register()

    private val utenforOmfangCounter = Counter.build("utenfor_omfang_totals", "Antall ganger en sak er utenfor omfang")
            .labelNames("dokumentType")
            .register()

    private val sakMementoStørrelse = Summary.build("sak_memento_size", "størrelse på sak document i databasen").register()

    override fun vedtaksperiodeTrengerLøsning(event: Behov) {
        behovCounter.labels(event.behovType()).inc()
    }

    override fun sakEndret(sakEndretEvent: SakObserver.SakEndretEvent) {
        sakMementoStørrelse.observe(sakEndretEvent.memento.toString().length.toDouble())
    }

    fun forGammelSkjemaversjon(err: SakskjemaForGammelt) {
        log.info(err.message)
    }

    override fun vedtaksperiodeEndret(event: StateChangeEvent) {
        tilstandCounter.labels(event.previousState.name, event.currentState.name, event.sykdomshendelse.javaClass.simpleName).inc()

        log.info("vedtaksperiode=${event.id} event=${event.sykdomshendelse.javaClass.simpleName} state=${event.currentState} previousState=${event.previousState}")

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

    fun<T: SykdomstidslinjeHendelse> utenforOmfang(hendelse: T) {
        utenforOmfangCounter.labels(hendelse.javaClass.simpleName).inc()
    }

}
