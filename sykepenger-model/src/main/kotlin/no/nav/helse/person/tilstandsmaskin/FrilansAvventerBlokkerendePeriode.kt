package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object FrilansAvventerBlokkerendePeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.FRILANS_AVVENTER_BLOKKERENDE_PERIODE

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        error("Frilansperioder er ikke støttet ennå, så dette blir veis ende! PS: Denne feilen skal ikke kunne nås.")
    }
}
