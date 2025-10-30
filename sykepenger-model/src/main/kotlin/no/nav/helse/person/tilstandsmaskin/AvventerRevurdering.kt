package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.person.EventBus
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VenterPå
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde

internal data object AvventerRevurdering : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.forkastBeregning(eventBus, with (vedtaksperiode.yrkesaktivitet) { eventBus.utbetalingEventBus }, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurderingArbeidstaker(eventBus, revurdering, aktivitetslogg)
        vedtaksperiode.behandlinger.forkastBeregning(eventBus, with (vedtaksperiode.yrkesaktivitet) { eventBus.utbetalingEventBus }, aktivitetslogg)
    }

    fun venterpå(vedtaksperiode: Vedtaksperiode) = when (val t = tilstand(vedtaksperiode)) {
        is TrengerInntektsopplysningerAnnenArbeidsgiver -> VenterPå.AnnenPeriode(t.trengerInntektsmelding.venter(), Venteårsak.INNTEKTSMELDING)

        HarPågåendeUtbetaling -> VenterPå.SegSelv(Venteårsak.UTBETALING)

        is TrengerInnteksopplysninger,
        KlarForVilkårsprøving,
        KlarForBeregning -> VenterPå.Nestemann
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, eventBus, aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.sikreRefusjonsopplysningerHvisTomt(eventBus, påminnelse, aktivitetslogg)
        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
            vedtaksperiode.videreførEksisterendeOpplysninger(eventBus, påminnelse.metadata.behandlingkilde, aktivitetslogg)
            if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) return
            aktivitetslogg.info("Ordnet opp i manglende inntekt/refusjon i AvventerRevurdering ved videreføring av eksisterende opplysninger.")
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)

    private fun tilstand(vedtaksperiode: Vedtaksperiode): Tilstand {
        if (vedtaksperiode.behandlinger.utbetales()) return HarPågåendeUtbetaling
        val førstePeriodeSomTrengerInntektsmelding = vedtaksperiode.førstePeriodeSomTrengerInntektsmelding()
        if (førstePeriodeSomTrengerInntektsmelding != null) {
            if (førstePeriodeSomTrengerInntektsmelding === vedtaksperiode)
                return TrengerInnteksopplysninger(vedtaksperiode)
            return TrengerInntektsopplysningerAnnenArbeidsgiver(førstePeriodeSomTrengerInntektsmelding)
        }
        if (vedtaksperiode.vilkårsgrunnlag == null) return KlarForVilkårsprøving
        return KlarForBeregning
    }

    private sealed interface Tilstand {
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg)
    }

    private data class TrengerInnteksopplysninger(private val vedtaksperiode: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering. Etterspør inntekt fra skatt")
            vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
        }
    }

    private data object HarPågåendeUtbetaling : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
        }
    }

    private data class TrengerInntektsopplysningerAnnenArbeidsgiver(val trengerInntektsmelding: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering på annen arbeidsgiver.")
        }
    }

    private data object KlarForVilkårsprøving : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerVilkårsprøvingRevurdering) {
                aktivitetslogg.info("Trenger å utføre vilkårsprøving før vi kan beregne utbetaling for revurderingen.")
            }
        }
    }

    private data object KlarForBeregning : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.tilstand(eventBus, aktivitetslogg, AvventerHistorikkRevurdering)
        }
    }
}
