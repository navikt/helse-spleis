package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerAvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_AVSLUTTET_UTEN_UTBETALING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        check(!vedtaksperiode.skalArbeidstakerBehandlesISpeil()) { "forventer ikke at en periode som skal behandles i speil, skal ende opp i $this" }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvsluttetUtenUtbetaling)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }
}
