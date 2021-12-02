package no.nav.helse.person.filter

import no.nav.helse.Fødselsnummer
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.utbetalingslinjer.Utbetaling

internal interface Featurefilter {
    fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean
}

internal class Brukerutbetalingfilter(
    private val fødselsnummer: Fødselsnummer,
    private val periodetype: Periodetype,
    private val utbetaling: Utbetaling,
    private val inntektskilde: Inntektskilde
) : Featurefilter {

    override fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean {
        if (!Fødselsnummer.brukerutbetalingfilter(fødselsnummer)) return nei(aktivitetslogg, "Fødselsdag passer ikke")
        if (periodetype != Periodetype.FØRSTEGANGSBEHANDLING) return nei(aktivitetslogg, "Perioden er ikke førstegangsbehandling")
        if (utbetaling.harDelvisRefusjon()) return nei(aktivitetslogg, "Utbetalingen består av delvis refusjon")
        if (inntektskilde != Inntektskilde.EN_ARBEIDSGIVER) return nei(aktivitetslogg, "Inntektskilden er ikke for en arbeidsgiver")
        return true
    }

    private fun nei(aktivitetslogg: IAktivitetslogg, årsak: String) = false.also {
        aktivitetslogg.info("Ikke kandidat til brukerutbetaling fordi: $årsak")
    }

    class Builder(private val fødselsnummer: Fødselsnummer) {
        private lateinit var periodetype: Periodetype
        private lateinit var utbetaling: Utbetaling
        private lateinit var inntektskilde: Inntektskilde


        internal fun periodetype(periodetype: Periodetype) = this.apply {
            this.periodetype = periodetype
        }

        internal fun utbetaling(utbetaling: Utbetaling) = this.apply {
            this.utbetaling = utbetaling
        }

        internal fun inntektkilde(inntektskilde: Inntektskilde) = this.apply {
            this.inntektskilde = inntektskilde
        }

        internal fun build() = Brukerutbetalingfilter(fødselsnummer, periodetype, utbetaling, inntektskilde)
    }
}
