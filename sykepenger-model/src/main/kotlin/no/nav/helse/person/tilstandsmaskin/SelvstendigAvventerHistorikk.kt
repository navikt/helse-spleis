package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerHistorikk : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_HISTORIKK
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler) {
        checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Forventer vilkårsgrunnlag for å beregne utbetaling" }
        vedtaksperiode.trengerYtelser(aktivitetslogg)
        aktivitetslogg.info("Forespør sykdoms- og inntektshistorikk")
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler): Revurderingseventyr? {
        vedtaksperiode.trengerYtelser(aktivitetslogg)
        return null
    }
}
