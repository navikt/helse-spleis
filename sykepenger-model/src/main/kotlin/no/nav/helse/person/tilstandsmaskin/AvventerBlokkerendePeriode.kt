package no.nav.helse.person.tilstandsmaskin

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import no.nav.helse.Toggle
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

internal fun <T> Vedtaksperiodetilstand.vurderÅGåVidereHvisOmAtOgDersomAt(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg, vurderÅGåVidere: (vedtaksperiode: Vedtaksperiode) -> T?): T? {
    check(this in setOf(AvventerBlokkerendePeriode, AvventerInntektsopplysningerForAnnenArbeidsgiver, AvventerRefusjonsopplysningerAnnenPeriode, AvventerSøknadForOverlappendePeriode)) { "Hei! hva holder du på med??" }
    if (ChronoUnit.DAYS.between(vedtaksperiode.opprettet, LocalDateTime.now()) >= 90) return vurderÅGåVidere(vedtaksperiode)
    if (vedtaksperiode.behandlinger.børBrukeSkatteinntekterDirekte()) return vurderÅGåVidere(vedtaksperiode)

    if (!vedtaksperiode.harEksisterendeInntekt() && Toggle.HoldIgjenPerioderUtenInntekt.enabled) {
        aktivitetslogg.info("Hei, hei, hold litt på hesten! Vi står i ${this::class.simpleName} uten inntekt, dette var snodig..")
        return null
    }
    return vurderÅGåVidere(vedtaksperiode)
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
        vurderÅGåVidereHvisOmAtOgDersomAt(vedtaksperiode, aktivitetslogg) {
            val nesteTilstandEtterInntekt = tilstandHvisBlokkeresAvAndre(vedtaksperiode)
            when {
                nesteTilstandEtterInntekt != null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt)
                vedtaksperiode.vilkårsgrunnlag == null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerVilkårsprøving)
                else -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerHistorikk)
            }
        }
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        return vurderÅGåVidereHvisOmAtOgDersomAt(vedtaksperiode, aktivitetslogg) {
            val nesteTilstandEtterInntekt = tilstandHvisBlokkeresAvAndre(vedtaksperiode)
            when {
                nesteTilstandEtterInntekt != null -> vedtaksperiode.tilstand(eventBus, aktivitetslogg, nesteTilstandEtterInntekt)
                else -> vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
            }
            null
        }
    }
}
