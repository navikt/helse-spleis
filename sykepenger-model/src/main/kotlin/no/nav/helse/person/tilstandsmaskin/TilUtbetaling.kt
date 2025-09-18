package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object TilUtbetaling : Vedtaksperiodetilstand {
    override val type = TilstandType.TIL_UTBETALING
    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }

    override fun håndterUtbetalingHendelse(
        vedtaksperiode: Vedtaksperiode,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterUtbetalingHendelse(aktivitetslogg)
        if (!vedtaksperiode.behandlinger.erAvsluttet()) return
        vedtaksperiode.tilstand(aktivitetslogg, Avsluttet) {
            aktivitetslogg.info("OK fra Oppdragssystemet")
        }
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        when {
            vedtaksperiode.behandlinger.erUbetalt() -> vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
            vedtaksperiode.behandlinger.erAvsluttet() -> vedtaksperiode.tilstand(aktivitetslogg, Avsluttet)
        }
    }
}
