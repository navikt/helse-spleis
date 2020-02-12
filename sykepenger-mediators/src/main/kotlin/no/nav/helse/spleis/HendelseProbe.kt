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

    fun onPåminnelse(påminnelse: Påminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand().toString())
            .inc()
        påminnelse.tell()
    }

    fun onYtelser(ytelser: Ytelser) {
        ytelser.tell()
    }

    fun onManuellSaksbehandling(manuellSaksbehandling: ManuellSaksbehandling) {
        manuellSaksbehandling.tell()
    }

    fun onVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag) {
        vilkårsgrunnlag.tell()
    }

    fun onInntektsmelding(inntektsmelding: Inntektsmelding) {
        inntektsmelding.tell()
    }

    fun onNySøknad(søknad: NySøknad) {
        søknad.tell()
    }

    fun onSendtSøknad(søknad: SendtSøknad) {
        søknad.tell()
    }

    private fun ArbeidstakerHendelse.tell() {
        hendelseCounter.labels(this.hendelsestype().name).inc()
    }
}
