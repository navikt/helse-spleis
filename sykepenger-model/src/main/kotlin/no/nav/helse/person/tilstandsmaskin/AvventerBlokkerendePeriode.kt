package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal fun nesteTilstandEtterInntekt(vedtaksperiode: Vedtaksperiode): Vedtaksperiodetilstand {
    return tilstandHvisBlokkeresAvAndre(vedtaksperiode) ?: AvventerBlokkerendePeriode
}

private fun tilstandHvisBlokkeresAvAndre(vedtaksperiode: Vedtaksperiode): Vedtaksperiodetilstand? {
    val førstePeriodeAnnenArbeidsgiverSomTrengerInntekt = vedtaksperiode.førstePeriodeSomVenterPåInntekt()
    val førstePeriodeSomTrengerRefusjonsopplysninger = vedtaksperiode.førstePeriodeSomVenterPåRefusjonsopplysninger()

    return when {
        vedtaksperiode.avventerSøknad() -> AvventerSøknadForOverlappendePeriode
        vedtaksperiode.vilkårsgrunnlag == null && førstePeriodeAnnenArbeidsgiverSomTrengerInntekt != null -> AvventerInntektsopplysningerForAnnenArbeidsgiver
        førstePeriodeSomTrengerRefusjonsopplysninger != null -> AvventerRefusjonsopplysningerAnnenPeriode
        else -> null
    }
}

internal fun Vedtaksperiodetilstand.bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode: Vedtaksperiode) {
    check(vedtaksperiode.skalArbeidstakerBehandlesISpeil()) { "forventer ikke at en periode som skal til AUU, skal ende opp i $this" }
    check(vedtaksperiode.refusjonstidslinje.isNotEmpty()) { "Periode i $this har ikke tilstrekkelige refusjonsopplysninger til utbetaling! VedtaksperiodeId = ${vedtaksperiode.id}." }
}

internal data object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_BLOKKERENDE_PERIODE
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        check(!vedtaksperiode.avventerSøknad()) { "forventer ikke å vente annen søknad" }
        vedtaksperiode.lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus, aktivitetslogg) {
            aktivitetslogg.info("Denne perioden har ikke faktaavklart inntekt, så håper det er med overlegg at den skal bruke skatt!")
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        val nesteTilstandEtterInntekt = tilstandHvisBlokkeresAvAndre(vedtaksperiode)
        when {
            nesteTilstandEtterInntekt != null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt)
            vedtaksperiode.vilkårsgrunnlag == null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerVilkårsprøving)
            else -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerHistorikk)
        }
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        vedtaksperiode.lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus, aktivitetslogg)
        val nesteTilstandEtterInntekt = tilstandHvisBlokkeresAvAndre(vedtaksperiode)
        when {
            nesteTilstandEtterInntekt != null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt)
            else -> vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
        }
        return null
    }
}
