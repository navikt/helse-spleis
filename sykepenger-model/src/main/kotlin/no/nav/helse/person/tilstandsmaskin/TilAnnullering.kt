package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import org.slf4j.LoggerFactory

internal data object TilAnnullering : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_ANNULLERING
    val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

    override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {}

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående annullering")
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Vi har ikke fått kvittering fra OS for annullering av vedtaksperiode ${vedtaksperiode.id}")
        sikkerLogg.warn("Vi har ikke fått kvittering fra OS for annullering av vedtaksperiode ${vedtaksperiode.id}")
    }
}
