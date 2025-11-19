package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerTilUtbetalingRevurdering : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_TIL_UTBETALING_REVURDERING

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }
}
