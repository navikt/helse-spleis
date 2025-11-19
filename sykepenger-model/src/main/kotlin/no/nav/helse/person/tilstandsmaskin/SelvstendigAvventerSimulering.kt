package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerSimulering : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.SELVSTENDIG_AVVENTER_SIMULERING
    override fun entering(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        behovsamler: Behovsamler
    ) {
        trengerSimulering(vedtaksperiode, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler): Revurderingseventyr? {
        trengerSimulering(vedtaksperiode, aktivitetslogg)
        return null
    }
}
