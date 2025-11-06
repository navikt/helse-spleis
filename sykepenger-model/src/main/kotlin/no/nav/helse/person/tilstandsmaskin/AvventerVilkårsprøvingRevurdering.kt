package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerVilkårsprøvingRevurdering : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)
}
