package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.hendelser.*

class HendelseProbe {
    private companion object {
        private val hendelseCounter = Counter.build("hendelser_totals", "Antall hendelser mottatt")
            .labelNames("hendelse")
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

    fun onUtbetaling(utbetaling: Utbetaling) {
        tell("Utbetaling")
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

    fun onSykmelding(sykmelding: Sykmelding) {
        tell("Sykmelding")
    }

    fun onSøknad(søknad: Søknad) {
        tell("Søknad")
    }

    private fun tell(navn: String) {
        hendelseCounter.labels(navn).inc()
    }
}
