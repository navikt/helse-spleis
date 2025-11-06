package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object TilUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_UTBETALING

    override fun håndterKorrigerendeInntektsmelding(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.nyBehandling(eventBus, dager.hendelse)
        vedtaksperiode.håndterKorrigerendeInntektsmelding(eventBus, dager, FunksjonelleFeilTilVarsler(aktivitetslogg))
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }
}
