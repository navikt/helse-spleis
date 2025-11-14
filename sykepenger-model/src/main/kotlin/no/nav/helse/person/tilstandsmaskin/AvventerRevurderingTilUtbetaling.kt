package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerRevurderingTilUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_REVURDERING_TIL_UTBETALING

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }
}
