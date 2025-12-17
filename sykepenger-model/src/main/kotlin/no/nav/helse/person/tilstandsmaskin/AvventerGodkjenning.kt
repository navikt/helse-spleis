package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Behandlinger.Behandling.Endring.Companion.bestemSkjæringstidspunkt
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Yrkesaktivitet.Companion.beregnSkjæringstidspunkt
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerGodkjenning : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_GODKJENNING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        loggEventuellSkattPåDirekten(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.trengerGodkjenning(eventBus, aktivitetslogg)
        return null
    }

    private fun loggEventuellSkattPåDirekten(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        if (vedtaksperiode.behandlinger.skjæringstidspunkter().isNotEmpty()) return
        if (vedtaksperiode.harEksisterendeInntekt()) return

        val (skjæringstidspunkt, skjæringstidspunkter) = bestemSkjæringstidspunkt(
            beregnetSkjæringstidspunkter = vedtaksperiode.person.yrkesaktiviteter.beregnSkjæringstidspunkt(vedtaksperiode.person.infotrygdhistorikk),
            sykdomstidslinje = vedtaksperiode.sykdomstidslinje,
            periode = vedtaksperiode.periode
        )
        if (skjæringstidspunkter.isEmpty()) return aktivitetslogg.info("Skatt på direkten: Her har vi fortsatt ingen skjæringstidspunkt")
        if (vedtaksperiode.person.vilkårsgrunnlagFor(skjæringstidspunkt) == null) return aktivitetslogg.info("Skatt på direkten: Her har vi nå skjæringstidspunktet $skjæringstidspunkt, men det er ikke vilkårsprøvd")
        aktivitetslogg.info("Skatt på direkten: Denne kunne vi nok reberegnet slik at den hektet seg på det vilkårsprøvde skjæringstidspunktet $skjæringstidspunkt")
    }
}
