package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.hendelser.*
import no.nav.helse.person.Aktivitetslogger
import no.nav.helse.person.ArbeidstakerHendelse
import org.slf4j.LoggerFactory

class HendelseProbe: HendelseListener {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("sikkerLogg")
        private val log = LoggerFactory.getLogger(HendelseProbe::class.java)

        private val hendelseCounter = Counter.build("hendelser_totals", "Antall hendelser mottatt")
            .labelNames("type")
            .register()

        private val påminnetCounter =
            Counter.build("paminnet_totals", "Antall ganger vi har mottatt en påminnelse")
                .labelNames("tilstand")
                .register()
    }

    override fun onPåminnelse(påminnelse: ModelPåminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand().toString())
            .inc()
        påminnelse.tell()
    }

    override fun onYtelser(ytelser: ModelYtelser) {
        ytelser.tell()
    }

    override fun onManuellSaksbehandling(manuellSaksbehandling: ModelManuellSaksbehandling) {
        manuellSaksbehandling.tell()
    }

    override fun onVilkårsgrunnlag(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        vilkårsgrunnlag.tell()
    }

    override fun onInntektsmelding(inntektsmelding: ModelInntektsmelding) {
        inntektsmelding.tell()
    }

    override fun onNySøknad(søknad: ModelNySøknad, aktivitetslogger: Aktivitetslogger) {
        søknad.tell()
    }

    override fun onSendtSøknad(søknad: ModelSendtSøknad) {
        søknad.tell()
    }

    override fun onUnprocessedMessage(message: String) {
        sikkerLogg.info("uhåndtert melding $message")
    }

    private fun ArbeidstakerHendelse.tell() {
        hendelseCounter.labels(this.hendelsetype().name).inc()
    }
}
