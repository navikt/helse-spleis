package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.inntektsmelding.InntektsmeldingHendelse
import no.nav.helse.hendelser.påminnelse.Påminnelse
import no.nav.helse.hendelser.søknad.NySøknadHendelse
import no.nav.helse.hendelser.søknad.SendtSøknadHendelse
import no.nav.helse.hendelser.søknad.SøknadHendelse
import org.slf4j.LoggerFactory

class HendelseProbe: HendelseConsumer.MessageListener {
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

    override fun onInntektsmelding(inntektsmelding: InntektsmeldingHendelse) {
        sikkerLogg.info("${inntektsmelding.toJson()}")
        inntektsmeldingMottattCounter.inc()
    }

    override fun onNySøknad(søknad: NySøknadHendelse) {
        søknad(søknad)
    }

    override fun onSendtSøknad(søknad: SendtSøknadHendelse) {
        søknad(søknad)
    }

    private fun søknad(søknad: SøknadHendelse) {
        sikkerLogg.info(søknad.toJson().toString())
        søknadCounter.labels(søknad.status).inc()
    }
}
