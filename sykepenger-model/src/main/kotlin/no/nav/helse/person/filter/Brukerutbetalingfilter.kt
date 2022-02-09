package no.nav.helse.person.filter

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime

internal interface Featurefilter {
    fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean
}

internal class Brukerutbetalingfilter(
    private val fødselsnummer: Fødselsnummer,
    private val periodetype: Periodetype,
    private val utbetaling: Utbetaling,
    private val inntektskilde: Inntektskilde,
    private val inntektsmeldingtidsstempel: LocalDateTime
) : Featurefilter {

    override fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean {
        if (!Fødselsnummer.brukerutbetalingfilter(fødselsnummer)) return nei(aktivitetslogg, "Fødselsdag passer ikke")
        if (periodetype != Periodetype.FØRSTEGANGSBEHANDLING) return nei(aktivitetslogg, "Perioden er ikke førstegangsbehandling")
        if (utbetaling.harDelvisRefusjon()) return nei(aktivitetslogg, "Utbetalingen består av delvis refusjon")
        if (inntektskilde != Inntektskilde.EN_ARBEIDSGIVER) return nei(aktivitetslogg, "Inntektskilden er ikke for en arbeidsgiver")
        if (inntektsmeldingtidsstempel < LocalDateTime.now().minusHours(24)) return nei(aktivitetslogg, "Inntektsmelding ble mottatt for mer enn 24 timer siden")
        return true
    }

    private fun nei(aktivitetslogg: IAktivitetslogg, årsak: String) = false.also {
        aktivitetslogg.info("Ikke kandidat til brukerutbetaling fordi: $årsak")
    }

    class Builder(private val fødselsnummer: Fødselsnummer) {
        private lateinit var periodetype: Periodetype
        private lateinit var utbetaling: Utbetaling
        private lateinit var inntektskilde: Inntektskilde
        private var inntektsmeldingtidsstempel = LocalDateTime.MIN

        internal fun periodetype(periodetype: Periodetype) = this.apply {
            this.periodetype = periodetype
        }

        internal fun utbetaling(utbetaling: Utbetaling) = this.apply {
            this.utbetaling = utbetaling
        }

        internal fun inntektkilde(inntektskilde: Inntektskilde) = this.apply {
            this.inntektskilde = inntektskilde
        }

        internal fun inntektsmeldingtidsstempel(tidsstempel: LocalDateTime) {
            this.inntektsmeldingtidsstempel = tidsstempel
        }

        internal fun build() = Brukerutbetalingfilter(fødselsnummer, periodetype, utbetaling, inntektskilde, inntektsmeldingtidsstempel)
    }
}
