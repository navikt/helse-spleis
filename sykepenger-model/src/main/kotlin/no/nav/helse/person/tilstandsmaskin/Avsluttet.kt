package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object Avsluttet : Vedtaksperiodetilstand {
    override val type = TilstandType.AVSLUTTET

    override val erFerdigBehandlet = true
    override fun entering(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        behovsamler: Behovsamler
    ) {
        vedtaksperiode.behandlinger.bekreftAvsluttetBehandlingMedVedtak(vedtaksperiode.yrkesaktivitet)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.yrkesaktivitet)
    }
}
