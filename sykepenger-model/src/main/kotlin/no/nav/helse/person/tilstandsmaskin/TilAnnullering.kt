package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object TilAnnullering : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_ANNULLERING

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        trengerAnnullering(vedtaksperiode, aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående annullering")
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        trengerAnnullering(vedtaksperiode, aktivitetslogg)
        return null
    }
}

private fun trengerAnnullering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
    return trengerUtbetaling(vedtaksperiode, aktivitetslogg, medMaksdato = false)
}
