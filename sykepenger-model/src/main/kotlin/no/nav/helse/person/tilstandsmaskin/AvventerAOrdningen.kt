package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerAOrdningen : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_A_ORDNINGEN

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
        aktivitetslogg.info("Forespør inntekter fra a-ordningen")
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) return
        vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
    }
}
