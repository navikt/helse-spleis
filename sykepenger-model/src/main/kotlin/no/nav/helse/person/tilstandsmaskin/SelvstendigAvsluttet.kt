package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object SelvstendigAvsluttet : Vedtaksperiodetilstand {
    override val type = TilstandType.SELVSTENDIG_AVSLUTTET

    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftAvsluttetBehandlingMedVedtak(vedtaksperiode.yrkesaktivitet)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.yrkesaktivitet)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        error("Kan ikke håndtere overstyring i tilstand $this for vedtaksperiode ${vedtaksperiode.id}")
    }
}
