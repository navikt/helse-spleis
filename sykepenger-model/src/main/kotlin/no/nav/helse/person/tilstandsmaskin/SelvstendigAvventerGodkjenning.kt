package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerGodkjenning : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(eventBus, aktivitetslogg)
    }
}
