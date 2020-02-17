package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.hendelser.*
import org.slf4j.LoggerFactory

class HendelseProbe {
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

    fun onPåminnelse(påminnelse: Påminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand().toString())
            .inc()
        tell("Påminnelse")
    }

    fun onYtelser(ytelser: Ytelser) {
        tell("Ytelser")
    }

    fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
        tell("Godkjenning")
    }

    fun onVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag) {
        tell("Vilkårsgrunnlag")
    }

    fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        tell("Inntektsmelding")
    }

    fun onNySøknad(søknad: NySøknad) {
        tell("NySøknad")
    }

    fun onSendtSøknad(søknad: SendtSøknad) {
        tell("SendtSøknad")
    }

    private fun tell(navn: String) {
        hendelseCounter.labels(navn).inc()
    }
}
