package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerGodkjenning : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak {
        if (vedtaksperiode.behandlinger.erAvvist()) return Venteårsak.Hva.HJELP.utenBegrunnelse
        return Venteårsak.Hva.GODKJENNING.utenBegrunnelse
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun nyAnnullering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.tilstand(aktivitetslogg, SelvstendigAvventerBlokkerendePeriode)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterSelvstendigOverstyringIgangsattFørstegangsvurdering(revurdering, aktivitetslogg)
    }
}
