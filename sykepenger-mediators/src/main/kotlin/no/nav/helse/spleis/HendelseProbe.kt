package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.behov.Behov
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.NySøknad
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.SendtSøknad
import no.nav.helse.sak.ArbeidstakerHendelse
import org.slf4j.LoggerFactory

class HendelseProbe: HendelseListener {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")

        private val mottattBehovCounter = Counter.build("mottatt_behov_totals", "Antall behov mottatt")
            .labelNames("type")
            .register()

        private val hendelseCounter = Counter.build("hendelser_totals", "Antall hendelser mottatt")
            .labelNames("type")
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
        påminnelse.tell()
    }

    override fun onLøstBehov(behov: Behov) {
        sikkerLogg.info(behov.toJson())
        behov.behovType().forEach {
            mottattBehovCounter.labels(it).inc()
        }
    }

    override fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmelding.tell()
    }

    override fun onNySøknad(søknad: NySøknad) {
        søknad.tell()
    }

    override fun onSendtSøknad(søknad: SendtSøknad) {
        søknad.tell()
    }

    private fun ArbeidstakerHendelse.tell() {
        sikkerLogg.info(this.toJson())
        hendelseCounter.labels(this.hendelsetype().name).inc()
    }
}
