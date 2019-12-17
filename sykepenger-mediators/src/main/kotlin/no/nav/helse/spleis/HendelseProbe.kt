package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.SendtSøknad
import org.slf4j.LoggerFactory

class HendelseProbe: HendelseListener {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        private val mottattBehovCounter = Counter.build("mottatt_behov_totals", "Antall behov mottatt")
            .labelNames("type")
            .register()

        private val innteksmeldingerMottattCounterName = "inntektsmeldinger_mottatt_totals"

        private val inntektsmeldingMottattCounter =
            Counter.build(innteksmeldingerMottattCounterName, "Antall inntektsmeldinger mottatt")
                .register()

        private val søknadCounterName = "nye_soknader_totals"

        private val søknadCounter = Counter.build(søknadCounterName, "Antall søknader mottatt")
            .labelNames("status")
            .register()

        private val påminnetCounter =
            Counter.build("paminnet_totals", "Antall ganger vi har mottatt en påminnelse")
                .labelNames("tilstand")
                .register()
    }

    override fun onPåminnelse(påminnelse: Påminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand.toString())
            .inc()
    }

    override fun onLøstBehov(behov: Behov) {
        sikkerLogg.info(behov.toJson())
        behov.behovType().forEach {
            mottattBehovCounter.labels(it).inc()
        }
    }

    override fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        sikkerLogg.info(inntektsmelding.toJson())
        inntektsmeldingMottattCounter.inc()
    }

    override fun onNySøknad(søknad: NySøknad) {
        sikkerLogg.info(søknad.toJson())
        søknadCounter.labels("NY").inc()
    }

    override fun onSendtSøknad(søknad: SendtSøknad) {
        sikkerLogg.info(søknad.toJson())
        søknadCounter.labels("SENDT").inc()
    }
}
