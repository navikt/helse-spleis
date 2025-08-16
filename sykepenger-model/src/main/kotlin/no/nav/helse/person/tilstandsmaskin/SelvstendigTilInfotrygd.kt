package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigTilInfotrygd : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_TIL_INFOTRYGD
    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Vedtaksperioden kan ikke behandles i Spleis.")
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = Venteårsak.Hva.HJELP.utenBegrunnelse

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        throw IllegalStateException("Revurdering håndteres av en periode i til_infotrygd")
    }
}
