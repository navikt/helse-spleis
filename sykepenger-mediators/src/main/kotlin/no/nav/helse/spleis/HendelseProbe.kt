package no.nav.helse.spleis

import io.prometheus.client.Counter
import no.nav.helse.hendelser.Påminnelse

object HendelseProbe {
    private val hendelseCounter = Counter.build("hendelser_totals", "Antall hendelser mottatt")
        .labelNames("hendelse")
        .register()

    private val påminnetCounter =
        Counter.build("paminnet_totals", "Antall ganger vi har mottatt en påminnelse")
            .labelNames("tilstand")
            .register()

    fun onPåminnelse(påminnelse: Påminnelse) {
        påminnetCounter
            .labels(påminnelse.tilstand().toString())
            .inc()
        tell("Påminnelse")
    }

    fun onYtelser() {
        tell("Ytelser")
    }

    fun onUtbetaling() {
        tell("Utbetaling")
    }

    fun onManuellSaksbehandling() {
        tell("Godkjenning")
    }

    fun onVilkårsgrunnlag() {
        tell("Vilkårsgrunnlag")
    }

    fun onInntektsmelding() {
        tell("Inntektsmelding")
    }

    fun onSykmelding() {
        tell("Sykmelding")
    }

    fun onSøknad() {
        tell("Søknad")
    }

    private fun tell(navn: String) {
        hendelseCounter.labels(navn).inc()
    }
}
