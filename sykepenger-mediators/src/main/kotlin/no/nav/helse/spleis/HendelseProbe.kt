package no.nav.helse.spleis

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.helse.hendelser.Påminnelse

object HendelseProbe {
    private val metrics: MeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun onPåminnelse(påminnelse: Påminnelse) {
        Counter.builder("paminnet_totals")
            .description("Antall ganger vi har mottatt en påminnelse")
            .tag("tilstand", påminnelse.tilstand().toString())
            .register(metrics)
            .increment()
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

    fun onSøknadFrilans() {
        tell("SøknadFrilans")
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

    fun onOverstyrArbeidsgiveropplysninger() {
        tell("OverstyrArbeidsgiveropplysninger")
    }

    fun onOverstyrArbeidsforhold() {
        tell("OverstyrArbeidsforhold")
    }

    fun onInfotrygdendring() {
        tell("Infotrygdendring")
    }

    fun onUtbetalingshistorikkEtterInfotrygdendring() {
        tell("UtbetalingshistorikkEtterInfotrygdendring")
    }

    fun onForkastSykmeldingsperioder() {
        tell("ForkastSykmeldingsperioder")
    }

    fun onAvbruttSøknad() {
        tell("AvbruttSøknad")
    }

    private fun tell(navn: String) {
        Counter.builder("hendelser_totals")
            .description("Antall hendelser mottatt")
            .tag("hendelse", navn)
            .register(metrics)
            .increment()
    }
}
