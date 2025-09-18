package no.nav.helse.person.tilstandsmaskin

import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VenterPå
import no.nav.helse.person.Venteårsak
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.behandlingkilde

internal data object AvventerRevurdering : Vedtaksperiodetilstand {
    override val type = TilstandType.AVVENTER_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.forkastUtbetaling(aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
        vedtaksperiode.behandlinger.forkastUtbetaling(aktivitetslogg)
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
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, aktivitetslogg)
    }

    override fun håndterUtbetalingHendelse(
        vedtaksperiode: Vedtaksperiode,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterUtbetalingHendelse(aktivitetslogg)
    }

    override fun håndterPåminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.sikreRefusjonsopplysningerHvisTomt(påminnelse, aktivitetslogg)
        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
            vedtaksperiode.videreførEksisterendeOpplysninger(påminnelse.metadata.behandlingkilde, aktivitetslogg)
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
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg)
    }

    private data class TrengerInnteksopplysninger(private val vedtaksperiode: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering. Etterspør inntekt fra skatt")
            vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
        }
    }

    private data object HarPågåendeUtbetaling : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
        }
    }

    private data class TrengerInntektsopplysningerAnnenArbeidsgiver(val trengerInntektsmelding: Vedtaksperiode) : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering på annen arbeidsgiver.")
        }
    }

    private data object KlarForVilkårsprøving : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.tilstand(aktivitetslogg, AvventerVilkårsprøvingRevurdering) {
                aktivitetslogg.info("Trenger å utføre vilkårsprøving før vi kan beregne utbetaling for revurderingen.")
            }
        }
    }

    private data object KlarForBeregning : Tilstand {
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.tilstand(aktivitetslogg, AvventerHistorikkRevurdering)
        }
    }
}
