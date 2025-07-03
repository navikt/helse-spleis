package no.nav.helse.person

import java.time.LocalDateTime
import java.time.Period
import no.nav.helse.etterlevelse.`fvl § 35 ledd 1`
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Påminnelse.Predikat.Flagg
import no.nav.helse.hendelser.Påminnelse.Predikat.VentetMinst
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_ANNULLERING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INFOTRYGDHISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_ANNULLERING
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.Venteårsak.Companion.fordi
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.Venteårsak.Hva.BEREGNING
import no.nav.helse.person.Venteårsak.Hva.GODKJENNING
import no.nav.helse.person.Venteårsak.Hva.HJELP
import no.nav.helse.person.Venteårsak.Hva.INNTEKTSMELDING
import no.nav.helse.person.Venteårsak.Hva.SØKNAD
import no.nav.helse.person.Venteårsak.Hva.UTBETALING
import no.nav.helse.person.Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT
import no.nav.helse.person.Venteårsak.Hvorfor.VIL_OMGJØRES
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SY_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_1
import no.nav.helse.person.inntekt.InntekterForBeregning
import no.nav.helse.utbetalingslinjer.Utbetalingtype

// Gang of four State pattern
internal sealed interface Vedtaksperiodetilstand {
    val type: TilstandType
    val erFerdigBehandlet: Boolean get() = false

    fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        LocalDateTime.MAX

    fun håndterMakstid(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.funksjonellFeil(RV_VT_1)
        vedtaksperiode.forkast(påminnelse, aktivitetslogg)
    }

    // Gitt at du er nestemann som skal behandles - hva venter du på?
    fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak?

    // venter du på noe?
    fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode): VedtaksperiodeVenter? = null

    fun replayUtført(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {}
    fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        dager.skalHåndteresAv(vedtaksperiode.periode)

    fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.håndterKorrigerendeInntektsmelding(dager, aktivitetslogg)
    }

    fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {}

    fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Forventet ikke utbetaling i %s".format(type.name))
    }

    fun håndter(
        vedtaksperiode: Vedtaksperiode,
        hendelse: OverstyrArbeidsgiveropplysninger,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    fun nyAnnullering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        aktivitetslogg.info("Tidligere periode ferdigbehandlet, men gjør ingen tilstandsendring.")
    }

    fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    )

    fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
}

internal data object Start : Vedtaksperiodetilstand {
    override val type = START
    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = HJELP.utenBegrunnelse

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.tilstand(
            aktivitetslogg,
            when {
                !vedtaksperiode.person.infotrygdhistorikk.harHistorikk() -> AvventerInfotrygdHistorikk
                else -> when (vedtaksperiode.arbeidsgiver.yrkesaktivitetssporing) {
                    is Arbeidstaker -> AvventerInntektsmelding
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    Behandlingsporing.Yrkesaktivitet.Frilans,
                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> AvventerBlokkerendePeriode
                }
            }
        )
    }
}

internal data object AvventerInfotrygdHistorikk : Vedtaksperiodetilstand {
    override val type = AVVENTER_INFOTRYGDHISTORIKK
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = null
    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.trengerHistorikkFraInfotrygd(aktivitetslogg)
    }

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
    }
}

internal data object AvventerRevurdering : Vedtaksperiodetilstand {
    override val type = AVVENTER_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.forkastUtbetaling(aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak? {
        return tilstand(vedtaksperiode).venteårsak()
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
        vedtaksperiode.behandlinger.forkastUtbetaling(aktivitetslogg)
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode): VedtaksperiodeVenter? {
        val venterPå = tilstand(vedtaksperiode).venterPå() ?: nestemann
        return vedtaksperiode.vedtaksperiodeVenter(venterPå)
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, aktivitetslogg)
    }

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterUtbetalingHendelse(aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
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
        fun venteårsak(): Venteårsak? = null
        fun venterPå(): Vedtaksperiode? = null
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg)
    }

    private data class TrengerInnteksopplysninger(private val vedtaksperiode: Vedtaksperiode) : Tilstand {
        override fun venterPå() = vedtaksperiode
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Trenger inntektsopplysninger etter igangsatt revurdering. Etterspør inntekt fra skatt")
            vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
        }
    }

    private data object HarPågåendeUtbetaling : Tilstand {
        override fun venteårsak() = UTBETALING.utenBegrunnelse
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
        }
    }

    private data class TrengerInntektsopplysningerAnnenArbeidsgiver(private val trengerInntektsmelding: Vedtaksperiode) : Tilstand {
        override fun venterPå() = trengerInntektsmelding
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

internal data object AvventerHistorikkRevurdering : Vedtaksperiodetilstand {
    override val type = AVVENTER_HISTORIKK_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Forventer vilkårsgrunnlag for å beregne revurdering" }
        aktivitetslogg.info("Forespør sykdoms- og inntektshistorikk")
        vedtaksperiode.trengerYtelser(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) =
        BEREGNING fordi OVERSTYRING_IGANGSATT

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerYtelser(aktivitetslogg)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)
}

internal data object AvventerVilkårsprøvingRevurdering : Vedtaksperiodetilstand {
    override val type = AVVENTER_VILKÅRSPRØVING_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = null
    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)
}

internal data object AvventerInntektsmelding : Vedtaksperiodetilstand {
    override val type: TilstandType = AVVENTER_INNTEKTSMELDING
    override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
        tilstandsendringstidspunkt.plusDays(180)

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.arbeidsgiver.yrkesaktivitetssporing is Arbeidstaker) { "Forventer kun arbeidstakere her" }
        vedtaksperiode.trengerInntektsmeldingReplay()
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = INNTEKTSMELDING.utenBegrunnelse

    override fun venter(
        vedtaksperiode: Vedtaksperiode,
        nestemann: Vedtaksperiode
    ) = vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(vedtaksperiode.behandlinger.harIkkeUtbetaling()) {
            "hæ?! vedtaksperiodens behandling er ikke uberegnet!"
        }
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ): Boolean {
        return vedtaksperiode.skalHåndtereDagerAvventerInntektsmelding(dager, aktivitetslogg)
    }

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(dager, aktivitetslogg)
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return vedtaksperiode.forkast(dager.hendelse, aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, revurdering.hendelse, aktivitetslogg)
        if (vedtaksperiode.tilstand !in setOf(AvventerInntektsmelding, AvventerBlokkerendePeriode)) return
        if (vedtaksperiode.tilstand == AvventerInntektsmelding) {
            vedtaksperiode.sendTrengerArbeidsgiveropplysninger()
        }
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (vurderOmKanGåVidere(vedtaksperiode, påminnelse, aktivitetslogg)) {
            aktivitetslogg.info("Gikk videre fra AvventerInntektsmelding til ${vedtaksperiode.tilstand::class.simpleName} som følge av en vanlig påminnelse.")
        }

        if (påminnelse.når(Flagg("trengerReplay"))) return vedtaksperiode.trengerInntektsmeldingReplay()
        if (vurderOmInntektsmeldingAldriKommer(påminnelse)) {
            aktivitetslogg.info("Nå henter vi inntekt fra skatt!")
            return vedtaksperiode.trengerInntektFraSkatt(aktivitetslogg)
        }
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger()
    }

    private fun vurderOmInntektsmeldingAldriKommer(påminnelse: Påminnelse): Boolean {
        if (påminnelse.når(Flagg("ønskerInntektFraAOrdningen"))) return true
        val ventetMinst3Måneder = påminnelse.når(VentetMinst(Period.ofDays(90)))
        return ventetMinst3Måneder
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, hendelse, aktivitetslogg)
    }

    override fun replayUtført(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.sendTrengerArbeidsgiveropplysninger()
        vurderOmKanGåVidere(vedtaksperiode, hendelse, aktivitetslogg)
    }

    override fun inntektsmeldingFerdigbehandlet(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vurderOmKanGåVidere(vedtaksperiode, hendelse, aktivitetslogg)
    }

    private fun vurderOmKanGåVidere(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ): Boolean {
        if (vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag()) {
            aktivitetslogg.funksjonellFeil(RV_SV_2)
            vedtaksperiode.forkast(hendelse, aktivitetslogg)
            return true
        }
        vedtaksperiode.videreførEksisterendeOpplysninger(hendelse.metadata.behandlingkilde, aktivitetslogg)

        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) return false
        vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
        return true
    }
}

internal data object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
    override val type: TilstandType = AVVENTER_BLOKKERENDE_PERIODE
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        check(!vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
            "Periode i avventer blokkerende har ikke tilstrekkelig informasjon til utbetaling! VedtaksperiodeId = ${vedtaksperiode.id}"
        }
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak? {
        return tilstand(vedtaksperiode).venteårsak()
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode): VedtaksperiodeVenter? {
        val venterPå = tilstand(vedtaksperiode).venterPå() ?: nestemann
        return vedtaksperiode.vedtaksperiodeVenter(venterPå)
    }

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (vedtaksperiode.skalBehandlesISpeil()) return vedtaksperiode.håndterKorrigerendeInntektsmelding(
            dager,
            aktivitetslogg
        )
        vedtaksperiode.håndterDager(dager, aktivitetslogg)
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return vedtaksperiode.forkast(
            dager.hendelse,
            aktivitetslogg
        )
    }

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) =
        tilstand(vedtaksperiode).gjenopptaBehandling(vedtaksperiode, hendelse, aktivitetslogg)

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        tilstand(vedtaksperiode).håndter(vedtaksperiode, påminnelse, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.forkastUtbetaling(aktivitetslogg)
        if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) vedtaksperiode.tilstand(
            aktivitetslogg,
            AvventerInntektsmelding
        )
    }

    private fun tilstand(
        vedtaksperiode: Vedtaksperiode,
    ): Tilstand {
        val førstePeriodeSomTrengerInntektsmelding = vedtaksperiode.førstePeriodeSomTrengerInntektsmelding()
        return when {
            !vedtaksperiode.skalBehandlesISpeil() -> ForventerIkkeInntekt
            vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() -> ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag
            vedtaksperiode.person.avventerSøknad(vedtaksperiode.periode) -> AvventerTidligereEllerOverlappendeSøknad
            førstePeriodeSomTrengerInntektsmelding != null -> when (førstePeriodeSomTrengerInntektsmelding) {
                vedtaksperiode -> TrengerInntektsmelding(førstePeriodeSomTrengerInntektsmelding)
                else -> TrengerInntektsmeldingAnnenPeriode(førstePeriodeSomTrengerInntektsmelding)
            }

            vedtaksperiode.vilkårsgrunnlag == null -> KlarForVilkårsprøving
            else -> KlarForBeregning
        }
    }

    private sealed interface Tilstand {
        fun venteårsak(): Venteårsak? = null
        fun venterPå(): Vedtaksperiode? = null
        fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg)
        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {}
    }

    private data object AvventerTidligereEllerOverlappendeSøknad : Tilstand {
        override fun venteårsak() = SØKNAD.utenBegrunnelse
        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
            aktivitetslogg.info("Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding som er før eller overlapper med vedtaksperioden")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
            if (påminnelse.når(VentetMinst(Period.ofMonths(3))) || påminnelse.når(Flagg("forkastOverlappendeSykmeldingsperioderAndreArbeidsgivere"))) {
                aktivitetslogg.varsel(RV_SY_4)
                vedtaksperiode.person.fjernSykmeldingsperiode(vedtaksperiode.periode)
            }
        }
    }

    private data object ForventerIkkeInntekt : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(aktivitetslogg, AvsluttetUtenUtbetaling)
        }
    }

    private data object ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.funksjonellFeil(RV_SV_2)
            vedtaksperiode.forkast(hendelse, aktivitetslogg)
        }
    }

    private data class TrengerInntektsmelding(val segSelv: Vedtaksperiode) : Tilstand {
        override fun venteårsak() = INNTEKTSMELDING.utenBegrunnelse
        override fun venterPå() = segSelv
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Går tilbake til Avventer inntektsmelding fordi perioden mangler inntekt og/eller refusjonsopplysninger")
            vedtaksperiode.tilstand(aktivitetslogg, AvventerInntektsmelding)
        }
    }

    private data class TrengerInntektsmeldingAnnenPeriode(private val trengerInntektsmelding: Vedtaksperiode) :
        Tilstand {
        override fun venteårsak() = INNTEKTSMELDING.utenBegrunnelse
        override fun venterPå() = trengerInntektsmelding
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Gjenopptar ikke behandling fordi minst én overlappende periode venter på nødvendig opplysninger fra arbeidsgiver")
        }
    }

    private data object KlarForVilkårsprøving : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(aktivitetslogg, AvventerVilkårsprøving)
        }
    }

    private data object KlarForBeregning : Tilstand {
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            hendelse: Hendelse,
            aktivitetslogg: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(aktivitetslogg, AvventerHistorikk)
        }
    }
}

internal data object AvventerVilkårsprøving : Vedtaksperiodetilstand {
    override val type = AVVENTER_VILKÅRSPRØVING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = null
    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerVilkårsgrunnlag(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattFørstegangsvurdering(revurdering, aktivitetslogg)
    }
}

internal data object AvventerHistorikk : Vedtaksperiodetilstand {
    override val type = AVVENTER_HISTORIKK
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Forventer vilkårsgrunnlag for å beregne utbetaling" }
        vedtaksperiode.trengerYtelser(aktivitetslogg)
        aktivitetslogg.info("Forespør sykdoms- og inntektshistorikk")
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = BEREGNING.utenBegrunnelse
    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerYtelser(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattFørstegangsvurdering(revurdering, aktivitetslogg)
    }
}

internal data object AvventerSimulering : Vedtaksperiodetilstand {
    override val type: TilstandType = AVVENTER_SIMULERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        trengerSimulering(vedtaksperiode, aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = UTBETALING.utenBegrunnelse
    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattFørstegangsvurdering(revurdering, aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        trengerSimulering(vedtaksperiode, aktivitetslogg)
    }

    private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.simuler(aktivitetslogg)
    }
}

internal data object AvventerSimuleringRevurdering : Vedtaksperiodetilstand {
    override val type: TilstandType = AVVENTER_SIMULERING_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.simuler(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = UTBETALING fordi OVERSTYRING_IGANGSATT
    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
    }

    override fun venter(
        vedtaksperiode: Vedtaksperiode,
        nestemann: Vedtaksperiode
    ) = vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.simuler(aktivitetslogg)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)
}

internal data object AvventerGodkjenning : Vedtaksperiodetilstand {
    override val type = AVVENTER_GODKJENNING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak {
        if (vedtaksperiode.behandlinger.erAvvist()) return HJELP.utenBegrunnelse
        return GODKJENNING.utenBegrunnelse
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun nyAnnullering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattFørstegangsvurdering(revurdering, aktivitetslogg)
    }
}

internal data object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
    override val type = AVVENTER_GODKJENNING_REVURDERING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak {
        if (vedtaksperiode.behandlinger.erAvvist()) return HJELP.utenBegrunnelse
        return GODKJENNING fordi OVERSTYRING_IGANGSATT
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
    }

    override fun nyAnnullering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        if (vedtaksperiode.behandlinger.erAvvist()) return
        vedtaksperiode.tilstand(aktivitetslogg, AvventerRevurdering)
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.trengerGodkjenning(aktivitetslogg)
    }
}

internal data object TilUtbetaling : Vedtaksperiodetilstand {
    override val type = TIL_UTBETALING
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = UTBETALING.utenBegrunnelse
    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg)
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående utbetaling")
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterUtbetalingHendelse(aktivitetslogg)
        if (!vedtaksperiode.behandlinger.erAvsluttet()) return
        vedtaksperiode.tilstand(aktivitetslogg, Avsluttet) {
            aktivitetslogg.info("OK fra Oppdragssystemet")
        }
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        when {
            vedtaksperiode.behandlinger.erUbetalt() -> vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
            vedtaksperiode.behandlinger.erAvsluttet() -> vedtaksperiode.tilstand(aktivitetslogg, Avsluttet)
        }
    }
}

internal data object TilAnnullering : Vedtaksperiodetilstand {
    override val type = TIL_ANNULLERING

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = UTBETALING.utenBegrunnelse

    override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
        TODO("Not yet implemented")
    }

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Stopper gjenoppta behandling pga. pågående annullering")
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        hendelse: UtbetalingHendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        TODO("Not yet implemented")
    }
}

internal data object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
    override val type = AVSLUTTET_UTEN_UTBETALING
    override val erFerdigBehandlet = true

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        val arbeidsgiverperiode = vedtaksperiode.arbeidsgiver.arbeidsgiverperiode(vedtaksperiode.periode)
        check(arbeidsgiverperiode?.forventerInntekt(vedtaksperiode.periode) != true) {
            "i granskauen! skal jo ikke skje dette ?!"
        }
        avsluttUtenVedtak(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun avsluttUtenVedtak(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        val inntekterForBeregning = with(InntekterForBeregning.Builder(vedtaksperiode.periode)) {
            vedtaksperiode.vilkårsgrunnlag?.inntektsgrunnlag?.beverte(this)
            build()
        }
        val (fastsattÅrsinntekt, inntektjusteringer) = inntekterForBeregning.tilBeregning(vedtaksperiode.arbeidsgiver.organisasjonsnummer)

        val utbetalingstidslinje = vedtaksperiode.behandlinger.lagUtbetalingstidslinje(fastsattÅrsinntekt, inntektjusteringer, vedtaksperiode.arbeidsgiver.yrkesaktivitetssporing)

        vedtaksperiode.behandlinger.avsluttUtenVedtak(vedtaksperiode.arbeidsgiver, aktivitetslogg, utbetalingstidslinje, inntekterForBeregning)
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.arbeidsgiver)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak {
        if (!vedtaksperiode.skalOmgjøres()) return HJELP.utenBegrunnelse
        return HJELP fordi VIL_OMGJØRES
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        if (!vedtaksperiode.skalOmgjøres()) null
        else vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.sikreNyBehandling(
            vedtaksperiode.arbeidsgiver,
            revurdering.hendelse.metadata.behandlingkilde,
            vedtaksperiode.person.beregnSkjæringstidspunkt(),
            vedtaksperiode.arbeidsgiver.beregnArbeidsgiverperiode()
        )
        if (vedtaksperiode.skalOmgjøres()) {
            revurdering.inngåSomEndring(vedtaksperiode, aktivitetslogg)
            revurdering.loggDersomKorrigerendeSøknad(
                aktivitetslogg,
                "Startet omgjøring grunnet korrigerende søknad"
            )
            vedtaksperiode.videreførEksisterendeRefusjonsopplysninger(
                behandlingkilde = revurdering.hendelse.metadata.behandlingkilde,
                dokumentsporing = null,
                aktivitetslogg = aktivitetslogg
            )
            aktivitetslogg.info("Denne perioden var tidligere regnet som innenfor arbeidsgiverperioden")
            if (vedtaksperiode.måInnhenteInntektEllerRefusjon()) {
                aktivitetslogg.info("mangler nødvendige opplysninger fra arbeidsgiver")
                return vedtaksperiode.tilstand(aktivitetslogg, AvventerInntektsmelding)
            }
        }
        vedtaksperiode.tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
    }

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterDager(dager, aktivitetslogg)

        if (!aktivitetslogg.harFunksjonelleFeilEllerVerre()) return
        if (!vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode, aktivitetslogg)) return
        vedtaksperiode.forkast(dager.hendelse, aktivitetslogg)
    }

    override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (!vedtaksperiode.skalOmgjøres() && vedtaksperiode.behandlinger.erAvsluttet()) return aktivitetslogg.info("Forventer ikke inntekt. Vil forbli i AvsluttetUtenUtbetaling")
    }
}

internal data object Avsluttet : Vedtaksperiodetilstand {
    override val type = AVSLUTTET

    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftAvsluttetBehandlingMedVedtak(vedtaksperiode.arbeidsgiver)
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) =
        HJELP.utenBegrunnelse

    override fun håndter(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.håndterKorrigerendeInntektsmelding(dager, FunksjonelleFeilTilVarsler(aktivitetslogg))
    }

    override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.behandlinger.bekreftÅpenBehandling(vedtaksperiode.arbeidsgiver)
    }

    override fun skalHåndtereDager(
        vedtaksperiode: Vedtaksperiode,
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ) =
        vedtaksperiode.skalHåndtereDagerRevurdering(dager, aktivitetslogg)

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        vedtaksperiode.behandlinger.sikreNyBehandling(
            vedtaksperiode.arbeidsgiver,
            revurdering.hendelse.metadata.behandlingkilde,
            vedtaksperiode.person.beregnSkjæringstidspunkt(),
            vedtaksperiode.arbeidsgiver.beregnArbeidsgiverperiode()
        )
        vedtaksperiode.subsumsjonslogg.logg(`fvl § 35 ledd 1`())
        revurdering.inngåSomRevurdering(vedtaksperiode, aktivitetslogg)
        vedtaksperiode.tilstand(aktivitetslogg, AvventerRevurdering)
    }
}

internal data object RevurderingFeilet : Vedtaksperiodetilstand {
    override val type: TilstandType = REVURDERING_FEILET
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak? {
        if (vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode, Aktivitetslogg())) return null
        return HJELP.utenBegrunnelse
    }

    override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) =
        vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

    override fun gjenopptaBehandling(
        vedtaksperiode: Vedtaksperiode,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (!vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode, Aktivitetslogg())) return aktivitetslogg.info(
            "Gjenopptar ikke revurdering feilet fordi perioden har tidligere avsluttede utbetalinger. Må behandles manuelt vha annullering."
        )
        aktivitetslogg.funksjonellFeil(RV_RV_2)
        vedtaksperiode.forkast(hendelse, aktivitetslogg)
    }

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
    }
}

internal data object AvventerAnnullering : Vedtaksperiodetilstand {
    override val type = AVVENTER_ANNULLERING

    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        vedtaksperiode.person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode): Venteårsak? {
        return null
    }

    override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {}

    override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val sisteUtbetalteUtbetaling = vedtaksperiode.behandlinger.sisteUtbetalteUtbetaling()
        checkNotNull(sisteUtbetalteUtbetaling) { "Fant ikke en utbetalt utbetaling for vedtaksperiode ${vedtaksperiode.id}" }

        val sisteAktiveUtbetalingMedSammeKorrelasjonsId = vedtaksperiode.arbeidsgiver.sisteAktiveUtbetalingMedSammeKorrelasjonsId(sisteUtbetalteUtbetaling)
        val grunnlagsdata = checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Mangler vilkårsgrunnlag i pågående annullering, er ikke det litt rart?" }

        if (sisteAktiveUtbetalingMedSammeKorrelasjonsId.overlapperMed(vedtaksperiode.periode)) {
            val annullering = vedtaksperiode.arbeidsgiver.lagAnnulleringsutbetaling(hendelse, aktivitetslogg, sisteAktiveUtbetalingMedSammeKorrelasjonsId)
            vedtaksperiode.behandlinger.leggTilAnnullering(annullering, grunnlagsdata, aktivitetslogg)
        } else {
            val tomAnnullering = vedtaksperiode.arbeidsgiver.lagTomUtbetaling(vedtaksperiode.periode, Utbetalingtype.ANNULLERING)
                .also { it.opprett(aktivitetslogg) }
            vedtaksperiode.behandlinger.leggTilAnnullering(tomAnnullering, grunnlagsdata, aktivitetslogg)
        }
        vedtaksperiode.tilstand(aktivitetslogg, TilAnnullering)
    }
}

internal data object TilInfotrygd : Vedtaksperiodetilstand {
    override val type = TIL_INFOTRYGD
    override val erFerdigBehandlet = true
    override fun entering(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Vedtaksperioden kan ikke behandles i Spleis.")
    }

    override fun venteårsak(vedtaksperiode: Vedtaksperiode) = HJELP.utenBegrunnelse

    override fun skalHåndtereDager(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) = false
    override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {}

    override fun igangsettOverstyring(
        vedtaksperiode: Vedtaksperiode,
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        throw IllegalStateException("Revurdering håndteres av en periode i til_infotrygd")
    }
}
