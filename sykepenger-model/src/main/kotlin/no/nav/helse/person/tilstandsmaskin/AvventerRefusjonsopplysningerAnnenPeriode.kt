package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VenterPå
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerRefusjonsopplysningerAnnenPeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    internal fun venterpå(vedtaksperiode: Vedtaksperiode): VenterPå {
        val annenPeriode = vedtaksperiode.førstePeriodeSomVenterPåRefusjonsopplysninger() ?: return VenterPå.Nestemann
        return VenterPå.AnnenPeriode(annenPeriode.venter(), Venteårsak.INNTEKTSMELDING)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
    }
}
