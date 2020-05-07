package no.nav.helse.spleis

import no.nav.helse.hendelser.*
import no.nav.helse.spleis.meldinger.model.*

internal class TestHendelseMediator() : IHendelseMediator {

    internal var lestNySøknad = false
        private set
    internal var lestSendtSøknadArbeidsgiver = false
        private set
    internal var lestSendtSøknad = false
        private set
    internal var lestInntektsmelding = false
        private set
    internal var lestPåminnelse = false
        private set
    internal var lestUtbetalingshistorikk = false
        private set
    internal var lestYtelser = false
        private set
    internal var lestVilkårsgrunnlag = false
        private set
    internal var lestSimulering = false
        private set
    internal var lestUtbetalingsgodkjenning = false
        private set
    internal var lestUtbetalingOverført = false
        private set
    internal var lestUtbetaling = false
        private set
    internal var lestKansellerUtbetaling = false
        private set

    fun reset() {
        lestNySøknad = false
        lestSendtSøknadArbeidsgiver = false
        lestSendtSøknad = false
        lestInntektsmelding = false
        lestPåminnelse = false
        lestUtbetalingshistorikk = false
        lestYtelser = false
        lestVilkårsgrunnlag = false
        lestSimulering = false
        lestUtbetalingsgodkjenning = false
        lestUtbetalingOverført = false
        lestUtbetaling = false
        lestKansellerUtbetaling = false
    }

    override fun behandle(message: HendelseMessage) {
        message.behandle(this)
    }

    override fun behandle(message: NySøknadMessage, sykmelding: Sykmelding) {
        lestNySøknad = true
    }

    override fun behandle(message: SendtSøknadArbeidsgiverMessage, søknad: SøknadArbeidsgiver) {
        lestSendtSøknadArbeidsgiver = true
    }

    override fun behandle(message: SendtSøknadNavMessage, søknad: Søknad) {
        lestSendtSøknad = true
    }

    override fun behandle(message: InntektsmeldingMessage, inntektsmelding: Inntektsmelding) {
        lestInntektsmelding = true
    }

    override fun behandle(message: PåminnelseMessage, påminnelse: Påminnelse) {
        lestPåminnelse = true
    }

    override fun behandle(message: UtbetalingshistorikkMessage, utbetalingshistorikk: Utbetalingshistorikk) {
        lestUtbetalingshistorikk = true
    }

    override fun behandle(message: YtelserMessage, ytelser: Ytelser) {
        lestYtelser = true
    }

    override fun behandle(message: VilkårsgrunnlagMessage, vilkårsgrunnlag: Vilkårsgrunnlag) {
        lestVilkårsgrunnlag = true
    }

    override fun behandle(message: UtbetalingsgodkjenningMessage, utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        lestUtbetalingsgodkjenning = true
    }

    override fun behandle(message: UtbetalingOverførtMessage, utbetaling: UtbetalingOverført) {
        lestUtbetalingOverført = true
    }

    override fun behandle(message: UtbetalingMessage, utbetaling: UtbetalingHendelse) {
        lestUtbetaling = true
    }

    override fun behandle(message: SimuleringMessage, simulering: Simulering) {
        lestSimulering = true
    }

    override fun behandle(message: KansellerUtbetalingMessage, kansellerUtbetaling: KansellerUtbetaling) {
        lestKansellerUtbetaling = true
    }
}
