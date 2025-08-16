package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode

internal data object RevurderingFeilet : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.REVURDERING_FEILET
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (!vedtaksperiode.kanForkastes()) return aktivitetslogg.info(
            "Gjenopptar ikke revurdering feilet fordi perioden har tidligere avsluttede utbetalinger. Må behandles manuelt vha annullering."
        )
        aktivitetslogg.funksjonellFeil(Varselkode.RV_RV_2)
        vedtaksperiode.forkast(hendelse, aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
    }
}
