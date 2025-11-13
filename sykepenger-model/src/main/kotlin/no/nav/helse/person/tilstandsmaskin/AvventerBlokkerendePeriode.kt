package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VenterPå
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg

internal fun nesteTilstandEtterInntekt(vedtaksperiode: Vedtaksperiode) =
    when {
        vedtaksperiode.avventerSøknad() -> AvventerSøknadForOverlappendePeriode
        else -> AvventerBlokkerendePeriode
    }

internal fun Vedtaksperiodetilstand.bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode: Vedtaksperiode) {
    check(vedtaksperiode.skalArbeidstakerBehandlesISpeil()) { "forventer ikke at en periode som skal til AUU, skal ende opp i $this" }
    check(!vedtaksperiode.måInnhenteInntektEllerRefusjon()) { "Periode i $this har ikke tilstrekkelig informasjon til utbetaling! VedtaksperiodeId = ${vedtaksperiode.id}." }
}

internal data object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = TilstandType.AVVENTER_BLOKKERENDE_PERIODE
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        bekreftAtPeriodenSkalBehandlesISpeilOgHarNokInformasjon(vedtaksperiode)
        check(!vedtaksperiode.avventerSøknad()) {
            "forventer ikke å vente annen søknad"
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    fun venterpå(vedtaksperiode: Vedtaksperiode) = when (val t = tilstand(vedtaksperiode)) {
        KlarForBeregning,
        KlarForVilkårsprøving -> VenterPå.Nestemann
        is TrengerInntektsmeldingAnnenPeriode -> VenterPå.AnnenPeriode(t.trengerInntektsmelding.venter(), Venteårsak.INNTEKTSMELDING)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) =
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, eventBus, hendelse, aktivitetslogg)

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        tilstand(vedtaksperiode).håndterPåminnelse(vedtaksperiode, eventBus, påminnelse, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun tilstand(vedtaksperiode: Vedtaksperiode): Tilstand {
        return when {
            vedtaksperiode.vilkårsgrunnlag == null -> when (val førstePeriodeSomTrengerInntekt = vedtaksperiode.førstePeriodeSomVenterPåInntekt()) {
                null -> when (val førstePeriodeSomTrengerRefusjonsopplysninger = vedtaksperiode.førstePeriodeSomVenterPåRefusjonsopplysninger()) {
                    null -> KlarForVilkårsprøving
                    // om vi venter på refusjonsopplysninger så må det være pga. mursteinsproblematikk
                    else -> TrengerInntektsmeldingAnnenPeriode(førstePeriodeSomTrengerRefusjonsopplysninger)
                }
                else -> TrengerInntektsmeldingAnnenPeriode(førstePeriodeSomTrengerInntekt)
            }
            else -> when (val førstePeriodeSomTrengerRefusjonsopplysninger = vedtaksperiode.førstePeriodeSomVenterPåRefusjonsopplysninger()) {
                null -> KlarForBeregning
                else -> TrengerInntektsmeldingAnnenPeriode(førstePeriodeSomTrengerRefusjonsopplysninger)
            }
        }
    }

    private sealed interface Tilstand {
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg)
        fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {}
    }

    private data class TrengerInntektsmeldingAnnenPeriode(val trengerInntektsmelding: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            eventBus: EventBus,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Gjenopptar ikke behandling fordi minst én overlappende periode venter på nødvendig opplysninger fra arbeidsgiver")
        }
    }

    private data object KlarForVilkårsprøving : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            eventBus: EventBus,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerVilkårsprøving)
        }
    }

    private data object KlarForBeregning : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            eventBus: EventBus,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerHistorikk)
        }
    }
}
