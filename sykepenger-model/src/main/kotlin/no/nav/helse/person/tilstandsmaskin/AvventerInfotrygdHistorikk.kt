package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerInfotrygdHistorikk : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_INFOTRYGDHISTORIKK
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(behovsamler, aktivitetslogg)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        behovsamler: Behovsamler
    ) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(behovsamler, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler): Revurderingseventyr? {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(behovsamler, aktivitetslogg)
        return null
    }
}
