package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerRefusjonsopplysningerAnnenPeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        vedtaksperiode.lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus, aktivitetslogg) {
            aktivitetslogg.info("Denne perioden har ikke faktaavklart inntekt, så håper det er med overlegg at den skal bruke skatt!")
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        val nyTilstand = nesteTilstandEtterInntekt(vedtaksperiode).takeUnless { it is AvventerRefusjonsopplysningerAnnenPeriode } ?: return null
        aktivitetslogg.info("Endrer tilstand fra AvventerRefusjonsopplysningerAnnenPeriode til ${nyTilstand::class.simpleName} som følge av en påminnelse.")
        vedtaksperiode.tilstand(eventBus, aktivitetslogg, nyTilstand)
        return null
    }
}
