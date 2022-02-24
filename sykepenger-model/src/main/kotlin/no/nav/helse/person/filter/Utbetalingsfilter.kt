package no.nav.helse.person.filter

import no.nav.helse.Toggle
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Inntektskilde.EN_ARBEIDSGIVER
import no.nav.helse.person.Inntektskilde.FLERE_ARBEIDSGIVERE
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime

internal interface Featurefilter {
    fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean
}

internal class Utbetalingsfilter private constructor(
    private val utbetaling: Utbetaling,
    private val utbetalingstidslinjerHarBrukerutbetaling: Boolean,
    private val inntektskilde: Inntektskilde,
    private val inntektsmeldingtidsstempel: LocalDateTime
) : Featurefilter {

    internal fun kanIkkeUtbetales(aktivitetslogg: IAktivitetslogg) = !filtrer(aktivitetslogg)

    override fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean {

        if (Toggle.DelvisRefusjon.disabled && utbetaling.harDelvisRefusjon()) {
            aktivitetslogg.error("Utbetalingen har endringer i både arbeidsgiver- og personoppdrag")
        } else {
            when (inntektskilde) {
                EN_ARBEIDSGIVER -> enArbeidsgiver(aktivitetslogg)
                FLERE_ARBEIDSGIVERE -> flereArbeidsgivere(aktivitetslogg)
            }
        }
        return !aktivitetslogg.hasErrorsOrWorse()
    }

    private fun flereArbeidsgivere(aktivitetslogg: IAktivitetslogg) {
        if (utbetalingstidslinjerHarBrukerutbetaling) aktivitetslogg.error("Utbetalingstidslinje inneholder brukerutbetaling")
        else if (utbetaling.harBrukerutbetaling()) aktivitetslogg.error("Utbetaling inneholder brukerutbetaling (men ikke for den aktuelle vedtaksperioden)")
    }

    private fun inntektsmeldingForGammel() = inntektsmeldingtidsstempel < LocalDateTime.now().minusHours(24)

    private fun enArbeidsgiver(aktivitetslogg: IAktivitetslogg) {
        if (utbetalingstidslinjerHarBrukerutbetaling && inntektsmeldingForGammel()) aktivitetslogg.error("Ikke kandidat for brukerutbetaling ettersom inntektsmelding ble mottatt for mer enn 24 timer siden")
    }

    class Builder {
        private lateinit var utbetaling: Utbetaling
        private lateinit var inntektskilde: Inntektskilde
        private var inntektsmeldingtidsstempel: LocalDateTime = LocalDateTime.MIN
        private var utbetalingstidslinjerHarBrukerutbetaling: Boolean? = null

        internal fun utbetaling(utbetaling: Utbetaling) = this.apply {
            this.utbetaling = utbetaling
        }

        internal fun inntektsmeldingtidsstempel(tidsstempel: LocalDateTime) = this.apply {
            this.inntektsmeldingtidsstempel = tidsstempel
        }

        internal fun inntektkilde(inntektskilde: Inntektskilde) = this.apply {
            this.inntektskilde = inntektskilde
        }

        internal fun utbetalingstidslinjerHarBrukerutbetaling(utbetalingstidslinjerHarBrukerutbetaling: Boolean) = this.apply {
            this.utbetalingstidslinjerHarBrukerutbetaling = utbetalingstidslinjerHarBrukerutbetaling
        }

        internal fun build() = Utbetalingsfilter(utbetaling, utbetalingstidslinjerHarBrukerutbetaling!!, inntektskilde, inntektsmeldingtidsstempel)
    }
}
