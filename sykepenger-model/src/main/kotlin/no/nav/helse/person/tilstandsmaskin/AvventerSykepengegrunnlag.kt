package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerSykepengegrunnlag : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_SYKEPENGEGRUNNLAG

    override fun entering(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
    ) {
        vedtaksperiode.trengerSykepengegrunnlag(aktivitetslogg, eventBus)
    }

    override fun håndterPåminnelse(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        påminnelse: Påminnelse,
        aktivitetslogg: IAktivitetslogg,
    ): Revurderingseventyr? {
        vedtaksperiode.trengerSykepengegrunnlag(aktivitetslogg, eventBus)
        return null
    }

    internal val etterVilkårsprøving: Vedtaksperiodetilstand
        get() = if (Toggle.AvventerSykepengegrunnlag.enabled) AvventerSykepengegrunnlag else AvventerHistorikk
}
