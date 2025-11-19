package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler) {
        vedtaksperiode.trengerGodkjenning(eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler): Revurderingseventyr? {
        vedtaksperiode.trengerGodkjenning(eventBus, aktivitetslogg)
        return null
    }
}
