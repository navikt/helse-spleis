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

    fun onUtbetalingshistorikk() {
        tell("Utbetalingshistorikk")
    }

    fun onUtbetalingshistorikkForFeriepenger() {
        tell("UtbetalingshistorikkForFeriepenger")
    }

    fun onYtelser() {
        tell("Ytelser")
    }

    fun onUtbetaling() {
        tell("Utbetaling")
    }

    fun onUtbetalingsgodkjenning() {
        tell("Godkjenning")
    }

    fun onVilkårsgrunnlag() {
        tell("Vilkårsgrunnlag")
    }

    internal fun onUtbetalingsgrunnlag() {
        tell("Utbetalingsgrunnlag")
    }

    fun onSimulering() {
        tell("Simulering")
    }

    fun onInntektsmelding() {
        tell("Inntektsmelding")
    }

    fun onInntektsmeldingReplay() {
        tell("InntektsmeldingReplay")
    }

    fun onSykmelding() {
        tell("Sykmelding")
    }

    fun onSøknadNav() {
        tell("SøknadNav")
    }

    fun onSøknadArbeidsgiver() {
        tell("SøknadArbeidsgiver")
    }

    fun onAnnullerUtbetaling() {
        tell("KansellerUtbetaling")
    }

    fun onOverstyrTidslinje() {
        tell("OverstyrTidslinje")
    }

    private fun tell(navn: String) {
        hendelseCounter.labels(navn).inc()
    }
}
