package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object TilInfotrygd : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_INFOTRYGD
    override val erFerdigBehandlet = true
    override fun entering(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        behovsamler: Behovsamler
    ) {
        aktivitetslogg.info("Vedtaksperioden kan ikke behandles i Spleis.")
    }
}
