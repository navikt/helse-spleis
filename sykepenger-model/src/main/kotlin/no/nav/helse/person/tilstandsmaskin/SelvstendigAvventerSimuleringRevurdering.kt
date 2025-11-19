package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.tilstandsmaskin.TilstandType.SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING

internal data object SelvstendigAvventerSimuleringRevurdering : Vedtaksperiodetilstand {
    override val type: TilstandType = SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(vedtaksperiode.behandlinger.utbetaling).simuler(aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        checkNotNull(vedtaksperiode.behandlinger.utbetaling).simuler(aktivitetslogg)
        return null
    }
}
