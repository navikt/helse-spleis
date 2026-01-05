package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.Vedtaksperiode.Companion.MED_SKJÆRINGSTIDSPUNKT
import no.nav.helse.person.Vedtaksperiode.Companion.medAktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal data object AvventerVilkårsprøving : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_VILKÅRSPRØVING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (påminnelse.når(Påminnelse.Predikat.Flagg("allePerioderForSammeArbeidsgiverMedSammeSkjæringstidspunktSomAvventerInntektsmeldingMåKommeSegVidere"))) {
            vedtaksperiode.person.vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(vedtaksperiode.skjæringstidspunkt))
                .filter { it.yrkesaktivitet === vedtaksperiode.yrkesaktivitet }
                .filter { it.tilstand is AvventerInntektsmelding }
                .medAktivitetslogg(aktivitetslogg) { annenVedtaksperiode, annenAktivitetslogg ->
                    annenVedtaksperiode.nullKronerRefusjonOmViManglerRefusjonsopplysninger(eventBus, påminnelse.metadata, annenAktivitetslogg)
                    annenVedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt(vedtaksperiode))
                }
            return Revurderingseventyr.reberegning(påminnelse, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
        }

        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
        return null
    }
}
