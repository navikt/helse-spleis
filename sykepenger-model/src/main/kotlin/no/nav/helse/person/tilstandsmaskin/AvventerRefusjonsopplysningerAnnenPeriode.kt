package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.Behovsamler
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerRefusjonsopplysningerAnnenPeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE

    override fun entering(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        behovsamler: Behovsamler
    ) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, behovsamler: Behovsamler) {
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
    }
}
