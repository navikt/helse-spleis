package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.AnnullerTomUtbetaling
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import org.slf4j.LoggerFactory

internal data object TilAnnullering : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_ANNULLERING
    val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        if (vedtaksperiode.behandlinger.sisteUtbetalingSkalOverføres()) {
            vedtaksperiode.behandlinger.overførSisteUtbetaling(aktivitetslogg)
        } else {
            vedtaksperiode.behandlinger.avsluttTomAnnullering(aktivitetslogg)
            if (!vedtaksperiode.behandlinger.erAvsluttet()) return
            vedtaksperiode.forkast(AnnullerTomUtbetaling(vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype), aktivitetslogg)
        }
    }

    override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {}

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående annullering")
    }

    override fun håndterUtbetalingHendelse(
        vedtaksperiode: Vedtaksperiode,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterUtbetalingHendelse(aktivitetslogg)
        if (!vedtaksperiode.behandlinger.erAvsluttet()) return
        vedtaksperiode.forkast(hendelse, aktivitetslogg)
            .also { aktivitetslogg.info("Annulleringen fikk OK fra Oppdragssystemet") }
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Vi har ikke fått kvittering fra OS for annullering av vedtaksperiode ${vedtaksperiode.id}")
        sikkerLogg.warn("Vi har ikke fått kvittering fra OS for annullering av vedtaksperiode ${vedtaksperiode.id}")
    }
}
