package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerInntektsopplysningerForAnnenArbeidsgiver : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER

    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        vedtaksperiode.lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus, aktivitetslogg) {
            aktivitetslogg.info("Denne perioden har ikke faktaavklart inntekt, så håper det er med overlegg at den skal bruke skatt!")
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        vurderÅGåVidereHvisOmAtOgDersomAt(vedtaksperiode, aktivitetslogg) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
        }
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        vedtaksperiode.lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus, aktivitetslogg)
        vurderÅGåVidereHvisOmAtOgDersomAt(vedtaksperiode, aktivitetslogg) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
        }
        return null
    }
}
