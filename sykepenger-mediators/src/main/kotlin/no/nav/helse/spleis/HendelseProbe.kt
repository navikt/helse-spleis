package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.hendelser.*
import no.nav.helse.person.ArbeidstakerHendelse
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

    fun onPåminnelse(påminnelse: ModelPåminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand().toString())
            .inc()
        påminnelse.tell()
    }

    fun onYtelser(ytelser: ModelYtelser) {
        ytelser.tell()
    }

    fun onManuellSaksbehandling(manuellSaksbehandling: ModelManuellSaksbehandling) {
        manuellSaksbehandling.tell()
    }

    fun onVilkårsgrunnlag(vilkårsgrunnlag: ModelVilkårsgrunnlag) {
        vilkårsgrunnlag.tell()
    }

    fun onInntektsmelding(inntektsmelding: ModelInntektsmelding) {
        inntektsmelding.tell()
    }

    fun onNySøknad(søknad: ModelNySøknad) {
        søknad.tell()
    }

    fun onSendtSøknad(søknad: ModelSendtSøknad) {
        søknad.tell()
    }

    private fun ArbeidstakerHendelse.tell() {
        hendelseCounter.labels(this.hendelsetype().name).inc()
    }
}
