package no.nav.helse.person.filter

import no.nav.helse.Fødselsnummer
import no.nav.helse.Toggle
import no.nav.helse.person.IAktivitetslogg
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.Periodetype
import no.nav.helse.utbetalingslinjer.Utbetaling
import java.time.LocalDateTime

internal interface Featurefilter {
    fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean
}

// Om Toggle.BrukerutbetalingFilter er enabled ønsker vi å gå videre også med revudering ettersom vi ikke vil kaste ut igjen en eventuell brukerutbetaling som revurderes.
// Dette kan medføre at en revurdering som går fra full til ingen refusjon kan gå gjennom uten å være en av de planlagte 2 om dagen som går gjennom Brukerutbetalingfilter.
// Om det revurderes fra full til delvis refusjon vil det uansett kastes ut ettersom delvis refusjon heller ikke støttes når filteret er på.
internal object RevurderingBrukerutbetalingfilter : Featurefilter {
    override fun filtrer(aktivitetslogg: IAktivitetslogg) = Toggle.BrukerutbetalingFilter.enabled
}

internal class Brukerutbetalingfilter private constructor(
    private val fødselsnummer: Fødselsnummer,
    private val periodetype: Periodetype,
    private val utbetaling: Utbetaling,
    private val inntektskilde: Inntektskilde,
    private val inntektsmeldingtidsstempel: LocalDateTime,
    private val vedtaksperiodeHarWarnings: Boolean,
    private val harBrukerutbetaling: Boolean
) : Featurefilter {

    override fun filtrer(aktivitetslogg: IAktivitetslogg): Boolean {
        val årsaker = mutableSetOf<String>()
        if (!Fødselsnummer.brukerutbetalingfilter(fødselsnummer)) årsaker.add("Fødselsdag passer ikke")
        if (periodetype !in setOf(Periodetype.FØRSTEGANGSBEHANDLING, Periodetype.FORLENGELSE)) årsaker.add("Perioden er ikke førstegangsbehandling eller forlengelse")
        if (utbetaling.harDelvisRefusjon()) årsaker.add("Utbetalingen består av delvis refusjon")
        if (inntektskilde != Inntektskilde.EN_ARBEIDSGIVER) årsaker.add("Inntektskilden er ikke for en arbeidsgiver")
        if (inntektsmeldingtidsstempel < LocalDateTime.now().minusHours(24)) årsaker.add("Inntektsmelding ble mottatt for mer enn 24 timer siden")
        if (vedtaksperiodeHarWarnings) årsaker.add("Vedtaksperioden har warnings")
        if (aktivitetslogg.hasWarningsOrWorse()) årsaker.add("Hendelsen har warnings")
        if (!harBrukerutbetaling) årsaker.add("Inneholder ikke brukerutbetaling")
        return evaluer(aktivitetslogg, årsaker)
    }

    private fun evaluer(aktivitetslogg: IAktivitetslogg, årsaker: Set<String>): Boolean {
        if (årsaker.isNotEmpty()) return false.also { aktivitetslogg.info("Ikke kandidat til brukerutbetaling fordi: ${årsaker.joinToString()}") }
        return if (Toggle.BrukerutbetalingFilter.enabled) true.also { aktivitetslogg.info("Plukket ut for brukerutbetaling") }
        else false.also { aktivitetslogg.info("Kandidat for brukerutbetaling") }
    }

    class Builder(private val fødselsnummer: Fødselsnummer) {
        private lateinit var periodetype: Periodetype
        private lateinit var utbetaling: Utbetaling
        private lateinit var inntektskilde: Inntektskilde
        private var inntektsmeldingtidsstempel = LocalDateTime.MIN
        private var vedtaksperiodeHarWarnings: Boolean? = null
        private var harBrukerutbetaling: Boolean? = null

        internal fun periodetype(periodetype: Periodetype) = this.apply {
            this.periodetype = periodetype
        }

        internal fun utbetaling(utbetaling: Utbetaling) = this.apply {
            this.utbetaling = utbetaling
        }

        internal fun inntektkilde(inntektskilde: Inntektskilde) = this.apply {
            this.inntektskilde = inntektskilde
        }

        internal fun inntektsmeldingtidsstempel(tidsstempel: LocalDateTime) = this.apply {
            this.inntektsmeldingtidsstempel = tidsstempel
        }

        internal fun vedtaksperiodeHarWarnings(vedtaksperiodeHarWarnings: Boolean) = this.apply {
            this.vedtaksperiodeHarWarnings = vedtaksperiodeHarWarnings
        }

        internal fun harBrukerutbetaling(harBrukerutbetaling: Boolean) = this.apply {
            this.harBrukerutbetaling = harBrukerutbetaling
        }

        internal fun build() =
            Brukerutbetalingfilter(
                fødselsnummer,
                periodetype,
                utbetaling,
                inntektskilde,
                inntektsmeldingtidsstempel,
                vedtaksperiodeHarWarnings!!,
                harBrukerutbetaling!!
            )
    }
}
