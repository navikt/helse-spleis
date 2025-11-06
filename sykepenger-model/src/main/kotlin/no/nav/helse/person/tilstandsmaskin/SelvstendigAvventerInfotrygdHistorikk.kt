package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvventerInfotrygdHistorikk : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(aktivitetslogg)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(aktivitetslogg)
    }

    override fun håndterKorrigerendeInntektsmelding(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
    }
}
