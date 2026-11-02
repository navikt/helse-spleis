package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.Toggle
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerRettighetskategori : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_RETTIGHETSKATEGORI

    override fun entering(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
    ) {
        vedtaksperiode.trengerRettighetskategori(aktivitetslogg, eventBus)
    }

    override fun håndterPåminnelse(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        påminnelse: Påminnelse,
        aktivitetslogg: IAktivitetslogg,
    ): Revurderingseventyr? {
        vedtaksperiode.trengerRettighetskategori(aktivitetslogg, eventBus)
        return null
    }

    internal fun nesteTilstand(vedtaksperiode: Vedtaksperiode): Vedtaksperiodetilstand =
        if (vedtaksperiode.erRevurdering()) AvventerVilkårsprøvingRevurdering else AvventerVilkårsprøving

    internal val førVilkårsprøving: Vedtaksperiodetilstand
        get() = if (Toggle.AvklareRettighetskategori.enabled) AvventerRettighetskategori else AvventerVilkårsprøving

    internal val førVilkårsprøvingRevurdering: Vedtaksperiodetilstand
        get() = if (Toggle.AvklareRettighetskategori.enabled) AvventerRettighetskategori else AvventerVilkårsprøvingRevurdering
}
