package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerRevurdering : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.SELVSTENDIG_AVVENTER_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val førstevedtaksperiode = vedtaksperiode.yrkesaktivitet.finnSammenhengendeVedtaksperioder(vedtaksperiode).first { it.tilstand.type == TilstandType.SELVSTENDIG_AVVENTER_REVURDERING }
        if (førstevedtaksperiode == vedtaksperiode) {
            aktivitetslogg.info("Gjenopptar behandling for revurdering")
            when (førstevedtaksperiode.vilkårsgrunnlag) {
                null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, SelvstendigAvventerVilkårsprøvingRevurdering)
                else -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, SelvstendigAvventerHistorikkRevurdering)
            }
        }
    }
}
