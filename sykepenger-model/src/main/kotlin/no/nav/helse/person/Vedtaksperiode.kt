package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.dto.AnnulleringskandidatDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.deserialisering.VedtaksperiodeInnDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.erRettFør
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav a`
import no.nav.helse.etterlevelse.`§ 8-29`
import no.nav.helse.forrigeDag
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.AnnullerUtbetaling
import no.nav.helse.hendelser.Arbeidsgiveropplysning
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeNyArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.IkkeUtbetaltArbeidsgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittArbeidgiverperiode
import no.nav.helse.hendelser.Arbeidsgiveropplysning.OppgittInntekt
import no.nav.helse.hendelser.Arbeidsgiveropplysning.RedusertUtbetaltBeløpIArbeidsgiverperioden
import no.nav.helse.hendelser.Arbeidsgiveropplysning.UtbetaltDelerAvArbeidsgiverperioden
import no.nav.helse.hendelser.Arbeidsgiveropplysninger
import no.nav.helse.hendelser.Avsender
import no.nav.helse.hendelser.Behandlingsavgjørelse
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.BitAvArbeidsgiverperiode
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.HendelseMetadata
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrInntektsgrunnlag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Påminnelse.Predikat.Flagg
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.Revurderingseventyr.Companion.annullering
import no.nav.helse.hendelser.Revurderingseventyr.Companion.tidligsteEventyr
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
import no.nav.helse.hendelser.SykepengegrunnlagForArbeidsgiver
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.UtbetalingHendelse
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.hendelser.til
import no.nav.helse.mapWithNext
import no.nav.helse.nesteDag
import no.nav.helse.person.Behandlinger.Behandling.Endring.ForberedendeVilkårsgrunnlag
import no.nav.helse.person.Behandlinger.Behandlingkilde
import no.nav.helse.person.Behandlinger.Companion.berik
import no.nav.helse.person.Dokumentsporing.Companion.inntektFraAOrdingen
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingDager
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingInntekt
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingRefusjon
import no.nav.helse.person.Dokumentsporing.Companion.overstyrTidslinje
import no.nav.helse.person.Dokumentsporing.Companion.søknad
import no.nav.helse.person.Venteårsak.Companion.fordi
import no.nav.helse.person.VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsforhold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForBeregning
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForOpptjeningsvurdering
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlagForArbeidsgiver
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad som delvis overlapper`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_11
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_5
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.person.builders.UtkastTilVedtakBuilder
import no.nav.helse.person.infotrygdhistorikk.ArbeidsgiverUtbetalingsperiode
import no.nav.helse.person.infotrygdhistorikk.Friperiode
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Infotrygdperiode
import no.nav.helse.person.infotrygdhistorikk.PersonUtbetalingsperiode
import no.nav.helse.person.inntekt.ArbeidsgiverInntektsopplysning
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklartInntekt
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.InntekterForBeregning
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektsmeldinginntekt
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.person.inntekt.SelvstendigInntektsopplysning
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.person.inntekt.SkatteopplysningerForSykepengegrunnlag
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.person.tilstandsmaskin.Avsluttet
import no.nav.helse.person.tilstandsmaskin.AvsluttetUtenUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerAOrdningen
import no.nav.helse.person.tilstandsmaskin.AvventerAnnullering
import no.nav.helse.person.tilstandsmaskin.AvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.AvventerGodkjenning
import no.nav.helse.person.tilstandsmaskin.AvventerGodkjenningRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerHistorikk
import no.nav.helse.person.tilstandsmaskin.AvventerHistorikkRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.AvventerInntektsmelding
import no.nav.helse.person.tilstandsmaskin.AvventerRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerSimulering
import no.nav.helse.person.tilstandsmaskin.AvventerSimuleringRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerVilkårsprøving
import no.nav.helse.person.tilstandsmaskin.AvventerVilkårsprøvingRevurdering
import no.nav.helse.person.tilstandsmaskin.RevurderingFeilet
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvsluttet
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerGodkjenning
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerHistorikk
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerSimulering
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerVilkårsprøving
import no.nav.helse.person.tilstandsmaskin.SelvstendigStart
import no.nav.helse.person.tilstandsmaskin.SelvstendigTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.Start
import no.nav.helse.person.tilstandsmaskin.TilAnnullering
import no.nav.helse.person.tilstandsmaskin.TilInfotrygd
import no.nav.helse.person.tilstandsmaskin.TilUtbetaling
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.Vedtaksperiodetilstand
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.BeregnetPeriode
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Medlemskapsfilter
import no.nav.helse.utbetalingstidslinje.Minsteinntektfilter
import no.nav.helse.utbetalingstidslinje.Minsteinntektsvurdering.Companion.lagMinsteinntektsvurdering
import no.nav.helse.utbetalingstidslinje.Opptjeningfilter
import no.nav.helse.utbetalingstidslinje.Sykdomsgradfilter
import no.nav.helse.utbetalingstidslinje.UberegnetVedtaksperiode
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjesubsumsjon
import no.nav.helse.utbetalingstidslinje.lagUtbetalingstidslinjePerArbeidsgiver
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class Vedtaksperiode private constructor(
    internal val person: Person,
    internal val yrkesaktivitet: Yrkesaktivitet,
    internal val id: UUID,
    internal var tilstand: Vedtaksperiodetilstand,
    internal val behandlinger: Behandlinger,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet,
    private val regelverkslogg: Regelverkslogg
) : Aktivitetskontekst, Comparable<Vedtaksperiode>, BehandlingObserver {

    internal constructor(
        egenmeldingsperioder: List<Periode>,
        metadata: HendelseMetadata,
        person: Person,
        yrkesaktivitet: Yrkesaktivitet,
        sykdomstidslinje: Sykdomstidslinje,
        dokumentsporing: Dokumentsporing,
        sykmeldingsperiode: Periode,
        arbeidssituasjon: Behandlinger.Behandling.Endring.Arbeidssituasjon,
        faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
        inntektsendringer: Beløpstidslinje = Beløpstidslinje(),
        ventetid: Periode?,
        forberedendeVilkårsgrunnlag: ForberedendeVilkårsgrunnlag?,
        regelverkslogg: Regelverkslogg
    ) : this(
        person = person,
        yrkesaktivitet = yrkesaktivitet,
        id = UUID.randomUUID(),
        tilstand = when (yrkesaktivitet.yrkesaktivitetstype) {
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigStart

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            is Arbeidstaker,
            Behandlingsporing.Yrkesaktivitet.Frilans -> Start
        },
        behandlinger = Behandlinger(),
        opprettet = LocalDateTime.now(),
        regelverkslogg = regelverkslogg
    ) {
        val periode = checkNotNull(sykdomstidslinje.periode()) { "sykdomstidslinjen er tom" }
        person.vedtaksperiodeOpprettet(id, yrkesaktivitet.yrkesaktivitetstype, periode, periode.start, opprettet)
        behandlinger.initiellBehandling(sykmeldingsperiode, sykdomstidslinje, arbeidssituasjon, egenmeldingsperioder, faktaavklartInntekt, inntektsendringer, ventetid, dokumentsporing, metadata.behandlingkilde, forberedendeVilkårsgrunnlag)
    }

    private val sykmeldingsperiode get() = behandlinger.sykmeldingsperiode()
    internal val periode get() = behandlinger.periode()
    internal val sykdomstidslinje get() = behandlinger.sykdomstidslinje()
    internal val subsumsjonslogg get() = behandlinger.subsumsjonslogg(regelverkslogg, id, person.fødselsnummer, yrkesaktivitet.organisasjonsnummer)
    internal val skjæringstidspunkt get() = behandlinger.skjæringstidspunkt()
    internal val førsteFraværsdag get() = yrkesaktivitet.finnFørsteFraværsdag(this.periode)

    // 💡Må ikke forveksles med `førsteFraværsdag` 💡
    // F.eks. januar med agp 1-10 & 16-21 så er `førsteFraværsdag` 16.januar, mens `startdatoPåSammenhengendeVedtaksperioder` er 1.januar
    private val startdatoPåSammenhengendeVedtaksperioder
        get() = yrkesaktivitet.startdatoPåSammenhengendeVedtaksperioder(
            this
        )
    internal val vilkårsgrunnlag get() = person.vilkårsgrunnlagFor(skjæringstidspunkt)
    private val eksterneIder get() = behandlinger.eksterneIder()
    private val eksterneIderSet get() = eksterneIder.map { it.id }.toSet()
    private val refusjonstidslinje get() = behandlinger.refusjonstidslinje()

    init {
        behandlinger.addObserver(this)
    }

    internal fun view() = VedtaksperiodeView(
        id = id,
        periode = periode,
        tilstand = tilstand.type,
        oppdatert = oppdatert,
        skjæringstidspunkt = skjæringstidspunkt,
        skjæringstidspunkter = behandlinger.skjæringstidspunkter(),
        egenmeldingsdager = behandlinger.egenmeldingsdager(),
        behandlinger = behandlinger.view(),
        førsteFraværsdag = førsteFraværsdag,
        skalBehandlesISpeil = skalBehandlesISpeil(),
        annulleringskandidater = person.finnAnnulleringskandidater(this)
    )

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun håndterSykmelding(sykmelding: Sykmelding) {
        sykmelding.trimLeft(periode.endInclusive)
    }

    private fun validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        check(tilstand != Start || tilstand != SelvstendigStart) { "en vedtaksperiode blir stående i Start-tilstanden" }
        if (!tilstand.erFerdigBehandlet) return behandlinger.validerIkkeFerdigBehandlet(hendelse.metadata.meldingsreferanseId, aktivitetslogg)

        behandlinger.validerFerdigBehandlet(hendelse.metadata.meldingsreferanseId, aktivitetslogg)
    }

    internal fun håndterSøknadFørsteGang(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        yrkesaktiviteter: List<Yrkesaktivitet>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        check(tilstand is Start || tilstand is SelvstendigStart) { "Kan ikke håndtere søknad i tilstand $tilstand" }
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        person.emitSøknadHåndtert(søknad.metadata.meldingsreferanseId.id, id, yrkesaktivitet.organisasjonsnummer)
        søknad.forUng(aktivitetsloggMedVedtaksperiodekontekst, person.alder)
        yrkesaktivitet.vurderOmSøknadIkkeKanHåndteres(aktivitetsloggMedVedtaksperiodekontekst, periode, yrkesaktiviteter)

        infotrygdhistorikk.validerMedFunksjonellFeil(aktivitetsloggMedVedtaksperiodekontekst, periode)
        håndterSøknad(søknad, aktivitetsloggMedVedtaksperiodekontekst)
        aktivitetsloggMedVedtaksperiodekontekst.info("Fullført behandling av søknad")

        if (aktivitetsloggMedVedtaksperiodekontekst.harFunksjonelleFeilEllerVerre()) forkast(søknad, aktivitetslogg)
        return Revurderingseventyr.nyPeriode(søknad, skjæringstidspunkt, behandlinger.egenmeldingsdager().plusElement(periode).periode()!!)
    }

    internal fun håndterKorrigertSøknad(søknad: Søknad, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!søknad.erRelevant(this.periode)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        person.emitSøknadHåndtert(søknad.metadata.meldingsreferanseId.id, id, yrkesaktivitet.organisasjonsnummer)

        when (tilstand) {
            AvventerBlokkerendePeriode,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerAOrdningen,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerSimulering,
            AvventerVilkårsprøving -> håndterOverlappendeSøknad(søknad, aktivitetsloggMedVedtaksperiodekontekst)

            AvsluttetUtenUtbetaling,
            Avsluttet,
            AvventerGodkjenningRevurdering,
            AvventerHistorikkRevurdering,
            AvventerRevurdering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøvingRevurdering,
            TilUtbetaling -> {
                håndterOverlappendeSøknadRevurdering(søknad, aktivitetsloggMedVedtaksperiodekontekst)
            }

            Start,
            RevurderingFeilet,
            AvventerAnnullering,
            TilAnnullering,
            TilInfotrygd -> error("Kan ikke håndtere søknad mens perioden er i $tilstand")

            SelvstendigStart -> error("Kan ikke håndtere søknad mens perioden er i $tilstand")

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigTilUtbetaling -> when (behandlinger.harFattetVedtak()) {
                true -> håndterOverlappendeSøknadRevurdering(søknad, aktivitetsloggMedVedtaksperiodekontekst)
                false -> håndterOverlappendeSøknad(søknad, aktivitetsloggMedVedtaksperiodekontekst)
            }
        }
        if (aktivitetsloggMedVedtaksperiodekontekst.harFunksjonelleFeilEllerVerre()) forkast(søknad, aktivitetsloggMedVedtaksperiodekontekst)
        return Revurderingseventyr.korrigertSøknad(søknad, skjæringstidspunkt, periode)
    }

    internal fun håndterOverstyrTidslinje(hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!hendelse.erRelevant(this.periode)) {
            hendelse.vurdertTilOgMed(periode.endInclusive)
            return null
        }
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val overstyring = when (tilstand) {
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerAOrdningen,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            TilUtbetaling,

            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigAvsluttet -> {
                val dagerNavOvertarAnsvar = behandlinger.dagerNavOvertarAnsvar
                oppdaterHistorikk(hendelse.metadata.behandlingkilde, overstyrTidslinje(hendelse.metadata.meldingsreferanseId), hendelse.sykdomstidslinje, aktivitetsloggMedVedtaksperiodekontekst, hendelse.dagerNavOvertarAnsvar(dagerNavOvertarAnsvar)) {
                    // ingen validering å gjøre :(
                }
                aktivitetsloggMedVedtaksperiodekontekst.info("Igangsetter overstyring av tidslinje")

                Revurderingseventyr.sykdomstidslinje(hendelse, this.skjæringstidspunkt, this.periode)
            }

            RevurderingFeilet,
            Start,
            AvventerAnnullering,
            TilAnnullering,
            TilInfotrygd -> error("Kan ikke overstyre tidslinjen i $tilstand")

            SelvstendigStart,
            SelvstendigTilUtbetaling -> error("Kan ikke overstyre tidslinjen i $tilstand")
        }

        hendelse.vurdertTilOgMed(periode.endInclusive)
        return overstyring
    }

    internal fun håndterAnmodningOmForkasting(anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        if (!anmodningOmForkasting.erRelevant(id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Behandler anmodning om forkasting")
        when (tilstand) {
            AvventerInntektsmelding,
            AvventerBlokkerendePeriode,
            AvventerAOrdningen,
            AvsluttetUtenUtbetaling -> forkast(anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst)

            Avsluttet,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerRevurdering,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            RevurderingFeilet,
            Start,
            TilInfotrygd,
            AvventerAnnullering,
            TilAnnullering,
            TilUtbetaling -> {
                if (anmodningOmForkasting.force) return forkast(anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst, true)
                aktivitetsloggMedVedtaksperiodekontekst.info("Avslår anmodning om forkasting i $tilstand")
            }

            SelvstendigAvventerBlokkerendePeriode -> forkast(anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst)

            SelvstendigAvsluttet,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling -> {
                if (anmodningOmForkasting.force) return forkast(anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst, true)
                aktivitetsloggMedVedtaksperiodekontekst.info("Avslår anmodning om forkasting i $tilstand")
            }
        }
    }

    internal fun håndterInntektFraInntektsmelding(inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg, inntektshistorikk: Inntektshistorikk): Revurderingseventyr? {
        // håndterer kun inntekt hvis inntektsdato treffer perioden
        if (inntektsmelding.datoForHåndteringAvInntekt !in periode) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        // 1. legger til inntekten sånn at den kanskje kan brukes i forbindelse med faktaavklaring av inntekt
        // 1.1 lagrer på den datoen inntektsmeldingen mener
        val inntektsmeldinginntekt = Inntektsmeldinginntekt(UUID.randomUUID(), inntektsmelding.inntektsdata, Inntektsmeldinginntekt.Kilde.Arbeidsgiver)
        inntektshistorikk.leggTil(inntektsmeldinginntekt)
        // 1.2 lagrer på vedtaksperioden også..
        this.førsteFraværsdag?.takeUnless { it == inntektsmeldinginntekt.inntektsdata.dato }?.also { alternativDato ->
            inntektshistorikk.leggTil(Inntektsmeldinginntekt(UUID.randomUUID(), inntektsmelding.inntektsdata.copy(dato = alternativDato), Inntektsmeldinginntekt.Kilde.Arbeidsgiver))
        }

        inntektsmeldingHåndtert(inntektsmelding)

        // 2. endrer vilkårsgrunnlaget hvis det finnes et
        if (!oppdaterVilkårsgrunnlagMedInntekt(inntektsmelding.korrigertInntekt())) return null

        check(!behandlinger.erAvsluttet()) {
            "forventer ikke at vedtaksperioden har en lukket behandling når inntekt håndteres"
        }

        aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
        return Revurderingseventyr.korrigertInntektsmeldingInntektsopplysninger(inntektsmelding, skjæringstidspunkt, skjæringstidspunkt)
    }

    private fun inntektsmeldingHåndtert(inntektsmelding: Inntektsmelding) {
        inntektsmelding.inntektHåndtert()
        behandlinger.oppdaterDokumentsporing(inntektsmelding.dokumentsporing)
        person.emitInntektsmeldingHåndtert(
            meldingsreferanseId = inntektsmelding.metadata.meldingsreferanseId.id,
            vedtaksperiodeId = id,
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer
        )
    }

    private fun oppdaterVilkårsgrunnlagMedInntekt(korrigertInntekt: ArbeidstakerFaktaavklartInntekt): Boolean {
        val grunnlag = vilkårsgrunnlag ?: return false
        /* fest setebeltet. nå skal vi prøve å endre vilkårsgrunnlaget */
        val resultat = grunnlag.nyeArbeidsgiverInntektsopplysninger(
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            inntekt = korrigertInntekt
        ) ?: return false

        val (nyttGrunnlag, _) = resultat
        person.lagreVilkårsgrunnlag(nyttGrunnlag)
        return true
    }

    internal fun håndterReplayAvInntektsmelding(vedtaksperiodeIdForReplay: UUID, inntektsmeldinger: List<Inntektsmelding>, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (vedtaksperiodeIdForReplay != this.id) return null
        if (inntektsmeldinger.isEmpty()) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        when (tilstand) {
            AvventerInntektsmelding,
            AvsluttetUtenUtbetaling -> {
                val antallInntektsmeldinger = inntektsmeldinger.size
                aktivitetsloggMedVedtaksperiodekontekst.info("Replayer inntektsmeldinger ($antallInntektsmeldinger stk) i $tilstand.")
                val trengerRefusjonsopplysninger = refusjonstidslinje.isEmpty() == true

                if (antallInntektsmeldinger > 1) aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
                return inntektsmeldinger
                    .mapNotNull { yrkesaktivitet.håndterInntektsmelding(it, aktivitetsloggMedVedtaksperiodekontekst, trengerRefusjonsopplysninger) }
                    .tidligsteEventyr()
            }

            AvventerBlokkerendePeriode,
            AvventerAOrdningen,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerRevurdering,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            AvventerAnnullering,
            TilAnnullering,
            TilUtbetaling -> {
                aktivitetsloggMedVedtaksperiodekontekst.info("Replayer ikke inntektsmelding fordi tilstanden er $tilstand.")
                aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
            }

            Avsluttet,
            RevurderingFeilet,
            Start,
            TilInfotrygd -> {
                aktivitetsloggMedVedtaksperiodekontekst.info("Replayer ikke inntektsmelding fordi tilstanden er $tilstand.")
            }

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling -> error("Kan ikke håndtere replay av inntektsmelding i en selvstendigtilstand: $tilstand")
        }
        return null
    }

    internal fun håndterInntektsmeldingerReplay(replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg) {
        if (!replays.erRelevant(this.id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.replayUtført(this, replays, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun inntektsmeldingFerdigbehandlet(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.inntektsmeldingFerdigbehandlet(this, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun håndterArbeidsgiveropplysninger(eventyr: List<List<Revurderingseventyr>>, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        person.emitInntektsmeldingHåndtert(hendelse.metadata.meldingsreferanseId.id, id, yrkesaktivitet.organisasjonsnummer)
        val tidligsteEventyr = eventyr.flatten().tidligsteEventyr()
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) forkast(hendelse, aktivitetslogg)
        return tidligsteEventyr
    }

    internal fun håndterArbeidsgiveropplysninger(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, vedtaksperioder: List<Vedtaksperiode>, inntektshistorikk: Inntektshistorikk, ubrukteRefusjonsopplysninger: Refusjonsservitør): Revurderingseventyr? {
        if (arbeidsgiveropplysninger.vedtaksperiodeId != id) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        if (tilstand !is AvventerInntektsmelding) {
            aktivitetsloggMedVedtaksperiodekontekst.info("Mottok arbeidsgiveropplysninger i ${tilstand.type}")
            return null
        }

        val eventyr = listOf(
            nullstillEgenmeldingsdagerIArbeidsgiverperiode(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst, inntektsmeldingDager(arbeidsgiveropplysninger.metadata.meldingsreferanseId)),
            håndterOppgittArbeidsgiverperiode(arbeidsgiveropplysninger, vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst),
            håndterOppgittRefusjon(arbeidsgiveropplysninger, vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst, ubrukteRefusjonsopplysninger),
            håndterOppgittInntekt(arbeidsgiveropplysninger, inntektshistorikk),
            håndterIkkeNyArbeidsgiverperiode(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterIkkeUtbetaltArbeidsgiverperiode(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterRedusertUtbetaltBeløpIArbeidsgiverperioden(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterUtbetaltDelerAvArbeidsgiverperioden(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterOpphørAvNaturalytelser(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
        )

        return håndterArbeidsgiveropplysninger(eventyr, arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndterKorrigerteArbeidsgiveropplysninger(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, vedtaksperioder: List<Vedtaksperiode>, inntektshistorikk: Inntektshistorikk, ubrukteRefusjonsopplysninger: Refusjonsservitør): Revurderingseventyr? {
        if (korrigerteArbeidsgiveropplysninger.vedtaksperiodeId != id) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        check(tilstand !is AvventerInntektsmelding) { "Mottok Korrigerende arbeidsgiveropplysninger i AvventerInntektsmelding " }

        val eventyr = listOf(
            håndterOppgittRefusjon(korrigerteArbeidsgiveropplysninger, vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst, ubrukteRefusjonsopplysninger),
            håndterOppgittInntekt(korrigerteArbeidsgiveropplysninger, inntektshistorikk),
            håndterKorrigertArbeidsgiverperiode(korrigerteArbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterKorrigertOpphørAvNaturalytelser(korrigerteArbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
        )

        return håndterArbeidsgiveropplysninger(eventyr, korrigerteArbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun håndterOppgittArbeidsgiverperiode(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val oppgittArbeidgiverperiode = arbeidsgiveropplysninger.filterIsInstance<OppgittArbeidgiverperiode>().singleOrNull() ?: return emptyList()
        val eventyr = mutableListOf<Revurderingseventyr>()
        val initiell = OppgittArbeidsgiverperiodehåndtering.opprett(oppgittArbeidgiverperiode.perioder, arbeidsgiveropplysninger.metadata)

        val rester = vedtaksperioder.fold(initiell) { acc, vedtaksperiode ->
            val arbeidsgiverperiodetidslinje = acc.sykdomstidslinje(vedtaksperiode.periode)
            if (arbeidsgiverperiodetidslinje != null) {
                eventyr.add(vedtaksperiode.håndterBitAvArbeidsgiverperiode(arbeidsgiveropplysninger, aktivitetslogg, arbeidsgiverperiodetidslinje))
            }
            acc.håndterVedtaksperiode(vedtaksperiode.periode)
        }

        val antallDagerIgjen = rester.sykdomstidslinje.count()
        if (antallDagerIgjen > 0) {
            aktivitetslogg.info("Det er rester igjen etter håndtering av dager ($antallDagerIgjen dager)")
        }

        return eventyr
    }

    private fun håndterBitAvArbeidsgiverperiode(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, arbeidsgiverperiodetidslinje: Sykdomstidslinje): Revurderingseventyr {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val bitAvArbeidsgiverperiode = BitAvArbeidsgiverperiode(arbeidsgiveropplysninger.metadata, arbeidsgiverperiodetidslinje, emptyList())
        when (tilstand) {
            AvventerInntektsmelding,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode -> {
                håndterDager(arbeidsgiveropplysninger, bitAvArbeidsgiverperiode, aktivitetsloggMedVedtaksperiodekontekst) {}
            }

            else -> {
                // det er oppgitt arbeidsgiverperiode på uventede perioder; mest sannsynlig
                // har da ikke vedtaksperioden bedt om Arbeidsgiverperiode som opplysning, men vi har fått det likevel
                varselFraArbeidsgiveropplysning(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst, RV_IM_24)
                aktivitetsloggMedVedtaksperiodekontekst.info("Håndterer ikke arbeidsgiverperiode i ${tilstand.type}")
            }
        }
        return Revurderingseventyr.arbeidsgiverperiode(arbeidsgiveropplysninger, skjæringstidspunkt, periode)
    }

    private data class OppgittArbeidsgiverperiodehåndtering(
        val arbeidsgiverperiode: List<Periode>,
        val sykdomstidslinje: Sykdomstidslinje
    ) {
        companion object {
            fun opprett(arbeidsgiverperiode: List<Periode>, hendelseMetadata: HendelseMetadata): OppgittArbeidsgiverperiodehåndtering {
                val hendelsekilde = Hendelseskilde("Inntektsmelding", hendelseMetadata.meldingsreferanseId, hendelseMetadata.innsendt) // TODO: Type? 🤔
                val sykdomstidslinje = if (arbeidsgiverperiode.isNotEmpty())
                    Sykdomstidslinje.arbeidsdager(arbeidsgiverperiode.first().start, arbeidsgiverperiode.last().endInclusive, hendelsekilde).merge(arbeidsgiverperiode.fold(Sykdomstidslinje()) { acc, periode ->
                        acc + Sykdomstidslinje.arbeidsgiverdager(periode.start, periode.endInclusive, 100.prosent, hendelsekilde)
                    }, replace)
                else Sykdomstidslinje()
                return OppgittArbeidsgiverperiodehåndtering(arbeidsgiverperiode, sykdomstidslinje)
            }
        }

        private val omsluttendePeriode = sykdomstidslinje.periode()

        private fun skalHåndtere(vedtaksperiode: Periode): Boolean {
            if (omsluttendePeriode == null) return false
            return vedtaksperiode.endInclusive >= omsluttendePeriode.start || vedtaksperiode.endInclusive.erRettFør(omsluttendePeriode.start)
        }

        fun sykdomstidslinje(vedtaksperiode: Periode): Sykdomstidslinje? {
            if (!skalHåndtere(vedtaksperiode)) return null
            val sykdomstidslinje = sykdomstidslinje.fremTilOgMed(vedtaksperiode.endInclusive)
            val snute = if (vedtaksperiode.start < omsluttendePeriode!!.start) Sykdomstidslinje.arbeidsdager(vedtaksperiode.start, omsluttendePeriode.start.forrigeDag, this.sykdomstidslinje.first().kilde) else Sykdomstidslinje()
            return snute.merge(sykdomstidslinje)
        }

        fun håndterVedtaksperiode(vedtaksperiode: Periode) = this.copy(
            sykdomstidslinje = sykdomstidslinje.fraOgMed(vedtaksperiode.endInclusive.nesteDag)
        )
    }

    private fun <T> håndterOppgittRefusjon(hendelse: T, vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg, ubrukteRefusjonsopplysninger: Refusjonsservitør): List<Revurderingseventyr> where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
        val oppgittRefusjon = hendelse.filterIsInstance<Arbeidsgiveropplysning.OppgittRefusjon>().singleOrNull() ?: return emptyList()
        val hovedopplysning = Arbeidsgiveropplysning.OppgittRefusjon.Refusjonsendring(startdatoPåSammenhengendeVedtaksperioder, oppgittRefusjon.beløp)
        val endringer = oppgittRefusjon.endringer.filter { it.fom > startdatoPåSammenhengendeVedtaksperioder }
        val alle = (endringer + hovedopplysning).distinctBy { it.fom }
        val sisteTom = ubrukteRefusjonsopplysningerEtter(ubrukteRefusjonsopplysninger).lastOrNull()?.dato
        val refusjonstidslinje = alle.sortedBy { it.fom }.mapWithNext { nåværende, neste ->
            // Om vi har et neste element tar vi dens forrige dag. Ellers tar vi den største datoen av det vi kjenner til og nåværende fom'en
            val tom = neste?.fom?.forrigeDag ?: (listOfNotNull(sisteTom, nåværende.fom).max())
            Beløpstidslinje.fra(periode = nåværende.fom til tom, beløp = nåværende.beløp, kilde = Kilde(hendelse.metadata.meldingsreferanseId, Avsender.ARBEIDSGIVER, hendelse.metadata.innsendt))
        }.reduce(Beløpstidslinje::plus)
        val servitør = Refusjonsservitør.fra(refusjonstidslinje)

        val eventyr = vedtaksperioder.mapNotNull { vedtaksperiode ->
            vedtaksperiode.håndterRefusjon(hendelse, inntektsmeldingRefusjon(hendelse.metadata.meldingsreferanseId), aktivitetslogg, servitør)
        }
        servitør.servér(ubrukteRefusjonsopplysninger, aktivitetslogg)
        return eventyr
    }

    private fun <T> håndterOppgittInntekt(hendelse: T, inntektshistorikk: Inntektshistorikk): List<Revurderingseventyr> where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
        val oppgittInntekt = hendelse.filterIsInstance<OppgittInntekt>().singleOrNull() ?: return emptyList()

        val inntektsdata = Inntektsdata(
            hendelseId = hendelse.metadata.meldingsreferanseId,
            dato = skjæringstidspunkt, // Her skulle du kanskje tro at det riktige var å lagre på første fraværsdag, MEN siden dette er arbeidsgiveropplysninger fra HAG har de hensyntatt at man er syk i annen måned enn skjæringstidspunktet, så vi skal bare sluke det de opplyser om og lagre på skjæringstidspunktet.
            beløp = oppgittInntekt.inntekt,
            tidsstempel = LocalDateTime.now()
        )
        inntektshistorikk.leggTil(
            Inntektsmeldinginntekt(
                id = UUID.randomUUID(),
                inntektsdata = inntektsdata,
                kilde = Inntektsmeldinginntekt.Kilde.Arbeidsgiver
            )
        )

        val grunnlag = vilkårsgrunnlag

        // Skjæringstidspunktet er _ikke_ vilkårsprøvd før (det mest normale - står typisk i AvventerInntektsmelding)
        if (grunnlag == null) {
            dokumentsporingFraArbeidsgiveropplysning(hendelse, ::inntektsmeldingInntekt)
            return listOf(Revurderingseventyr.inntekt(hendelse, skjæringstidspunkt))
        }

        val result = grunnlag.nyeArbeidsgiverInntektsopplysninger(
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            inntekt = ArbeidstakerFaktaavklartInntekt(
                id = UUID.randomUUID(),
                inntektsdata = inntektsdata,
                inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver
            )
        )
        // todo: per 10. januar 2025 så sender alltid Hag inntekt i portal-inntektsmeldinger selv om vi ikke har bedt om det, derfor må vi ta høyde for at det ikke nødvendigvis er endringer
            ?: return emptyList()

        val (nyttGrunnlag, _) = result
        person.lagreVilkårsgrunnlag(nyttGrunnlag)
        // Skjæringstidspunktet er allerede vilkårsprøvd, men inntekten for arbeidsgiveren er byttet ut med denne oppgitte inntekten
        dokumentsporingFraArbeidsgiveropplysning(hendelse, ::inntektsmeldingInntekt)
        return listOf(Revurderingseventyr.inntekt(hendelse, skjæringstidspunkt))
    }

    private fun håndterIkkeNyArbeidsgiverperiode(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        if (arbeidsgiveropplysninger.filterIsInstance<IkkeNyArbeidsgiverperiode>().isEmpty()) return emptyList()
        aktivitetslogg.info("Arbeidsgiver mener at det ikke er noen ny arbeidsgiverperiode")
        aktivitetslogg.varsel(RV_IM_25)
        return emptyList()
    }

    private fun håndterIkkeUtbetaltArbeidsgiverperiode(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val ikkeUbetaltArbeidsgiverperiode = arbeidsgiveropplysninger.filterIsInstance<IkkeUtbetaltArbeidsgiverperiode>().singleOrNull() ?: return emptyList()
        return håndterNavUtbetalerArbeidsgiverperiode(aktivitetslogg, arbeidsgiveropplysninger) {
            ikkeUbetaltArbeidsgiverperiode.valider(aktivitetslogg)
        }
    }

    private fun håndterRedusertUtbetaltBeløpIArbeidsgiverperioden(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val redusertUtbetaltBeløpIArbeidsgiverperioden = arbeidsgiveropplysninger.filterIsInstance<RedusertUtbetaltBeløpIArbeidsgiverperioden>().singleOrNull() ?: return emptyList()
        return håndterNavUtbetalerArbeidsgiverperiode(aktivitetslogg, arbeidsgiveropplysninger) {
            redusertUtbetaltBeløpIArbeidsgiverperioden.valider(aktivitetslogg)
        }
    }

    private fun håndterUtbetaltDelerAvArbeidsgiverperioden(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val utbetaltDelerAvArbeidsgiverperioden = arbeidsgiveropplysninger.filterIsInstance<UtbetaltDelerAvArbeidsgiverperioden>().singleOrNull() ?: return emptyList()
        val perioderNavUtbetaler = behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.dager.flatMap { it.uten(LocalDate.MIN til utbetaltDelerAvArbeidsgiverperioden.utbetaltTilOgMed) }
        return håndterNavUtbetalerArbeidsgiverperiode(aktivitetslogg, arbeidsgiveropplysninger, perioderNavUtbetaler = perioderNavUtbetaler) {
            utbetaltDelerAvArbeidsgiverperioden.valider(aktivitetslogg)
        }
    }

    private fun håndterNavUtbetalerArbeidsgiverperiode(
        aktivitetslogg: IAktivitetslogg,
        arbeidsgiveropplysninger: Arbeidsgiveropplysninger,
        perioderNavUtbetaler: List<Periode> = behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.dager,
        valider: () -> Unit
    ): List<Revurderingseventyr> {
        val bit = sykNavBit(arbeidsgiveropplysninger, perioderNavUtbetaler)
        if (bit == null) valider()
        else håndterDager(arbeidsgiveropplysninger, bit, aktivitetslogg, valider)
        return listOf(Revurderingseventyr.arbeidsgiverperiode(arbeidsgiveropplysninger, skjæringstidspunkt, this.periode))
    }

    private fun håndterOpphørAvNaturalytelser(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        if (arbeidsgiveropplysninger.filterIsInstance<Arbeidsgiveropplysning.OpphørAvNaturalytelser>().isEmpty()) return emptyList()
        aktivitetslogg.funksjonellFeil(RV_IM_7)
        return emptyList()
    }

    private fun håndterKorrigertOpphørAvNaturalytelser(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        if (korrigerteArbeidsgiveropplysninger.filterIsInstance<Arbeidsgiveropplysning.OpphørAvNaturalytelser>().isEmpty()) return emptyList()
        varselFraArbeidsgiveropplysning(korrigerteArbeidsgiveropplysninger, aktivitetslogg, RV_IM_7)
        return listOf(Revurderingseventyr.arbeidsgiverperiode(korrigerteArbeidsgiveropplysninger, skjæringstidspunkt, periode))
    }

    private fun håndterKorrigertArbeidsgiverperiode(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        varselVedEndretArbeidsgiverperiode(korrigerteArbeidsgiveropplysninger, aktivitetslogg)

        val korrigertUtbetalingIArbeidsgiverperiode =
            (korrigerteArbeidsgiveropplysninger.filterIsInstance<RedusertUtbetaltBeløpIArbeidsgiverperioden>() +
                korrigerteArbeidsgiveropplysninger.filterIsInstance<UtbetaltDelerAvArbeidsgiverperioden>() +
                korrigerteArbeidsgiveropplysninger.filterIsInstance<IkkeUtbetaltArbeidsgiverperiode>() +
                korrigerteArbeidsgiveropplysninger.filterIsInstance<IkkeNyArbeidsgiverperiode>())
                .singleOrNull()

        if (korrigertUtbetalingIArbeidsgiverperiode != null) {
            varselFraArbeidsgiveropplysning(korrigerteArbeidsgiveropplysninger, aktivitetslogg, RV_IM_8)
        }

        return listOf(Revurderingseventyr.arbeidsgiverperiode(korrigerteArbeidsgiveropplysninger, skjæringstidspunkt, periode))
    }

    private fun varselVedEndretArbeidsgiverperiode(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val oppgittArbeidgiverperiode = korrigerteArbeidsgiveropplysninger.filterIsInstance<OppgittArbeidgiverperiode>().singleOrNull() ?: return
        val beregnetArbeidsgiverperiode = behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.periode ?: return varselFraArbeidsgiveropplysning(korrigerteArbeidsgiveropplysninger, aktivitetslogg, RV_IM_24)
        if (oppgittArbeidgiverperiode.perioder.periode()!! in beregnetArbeidsgiverperiode) return
        varselFraArbeidsgiveropplysning(korrigerteArbeidsgiveropplysninger, aktivitetslogg, RV_IM_24)
    }

    private fun sykNavBit(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, perioderNavUtbetaler: List<Periode>): BitAvArbeidsgiverperiode? {
        val dagerNavOvertarAnsvar = perioderNavUtbetaler
            .filter { it.overlapperMed(this.periode) }
            .map { it.subset(this.periode) }

        if (dagerNavOvertarAnsvar.isEmpty()) return null
        return BitAvArbeidsgiverperiode(arbeidsgiveropplysninger.metadata, Sykdomstidslinje(), dagerNavOvertarAnsvar)
    }

    private fun <T> dokumentsporingFraArbeidsgiveropplysning(hendelse: T, dokumentsporing: (meldingsreferanseId: MeldingsreferanseId) -> Dokumentsporing) where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
        behandlinger.sikreNyBehandling(
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = hendelse.metadata.behandlingkilde,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = yrkesaktivitet.beregnArbeidsgiverperiode()
        )
        behandlinger.oppdaterDokumentsporing(dokumentsporing(hendelse.metadata.meldingsreferanseId))
    }

    private fun <T> varselFraArbeidsgiveropplysning(hendelse: T, aktivitetslogg: IAktivitetslogg, varselkode: Varselkode) where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
        dokumentsporingFraArbeidsgiveropplysning(hendelse, ::inntektsmeldingDager)
        aktivitetslogg.varsel(varselkode)
    }

    internal fun håndterDagerFraInntektsmelding(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!tilstand.skalHåndtereDager(this, dager, aktivitetsloggMedVedtaksperiodekontekst) || dager.alleredeHåndtert(behandlinger))
            return dager.vurdertTilOgMed(periode.endInclusive)
        tilstand.håndterKorrigerendeInntektsmelding(this, dager, aktivitetsloggMedVedtaksperiodekontekst)
        dager.vurdertTilOgMed(periode.endInclusive)
    }

    internal fun skalHåndtereDagerRevurdering(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg): Boolean {
        return skalHåndtereDager(dager, aktivitetslogg) { sammenhengende ->
            dager.skalHåndteresAvRevurdering(periode, sammenhengende, behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.dager)
        }
    }

    internal fun skalHåndtereDagerAvventerInntektsmelding(
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg
    ): Boolean {
        return skalHåndtereDager(dager, aktivitetslogg) { sammenhengende ->
            dager.skalHåndteresAv(sammenhengende)
        }
    }

    private fun skalHåndtereDager(
        dager: DagerFraInntektsmelding,
        aktivitetslogg: IAktivitetslogg,
        strategi: DagerFraInntektsmelding.(Periode) -> Boolean
    ): Boolean {
        val sammenhengende = yrkesaktivitet.finnSammenhengendeVedtaksperioder(this)
            .map { it.periode }
            .periode() ?: return false
        if (!strategi(dager, sammenhengende)) return false
        aktivitetslogg.info("Vedtaksperioden $periode håndterer dager fordi den sammenhengende perioden $sammenhengende overlapper med inntektsmelding")
        return true
    }

    internal fun håndterDager(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val bit = dager.bitAvInntektsmelding(aktivitetslogg, periode) ?: dager.tomBitAvInntektsmelding(
            aktivitetslogg,
            periode
        )
        håndterDager(dager.hendelse, bit, aktivitetslogg) {
            dager.valider(aktivitetslogg, vedtaksperiodeId = id)
            dager.validerArbeidsgiverperiode(aktivitetslogg, periode, behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.dager)
        }
    }

    private fun håndterDagerUtenEndring(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val hendelse = dager.tomBitAvInntektsmelding(aktivitetslogg, periode)
        håndterDager(dager.hendelse, hendelse, aktivitetslogg) {
            dager.valider(aktivitetslogg, behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.dager, vedtaksperiodeId = id)
        }
    }

    private fun håndterDager(
        hendelse: Hendelse,
        bit: BitAvArbeidsgiverperiode,
        aktivitetslogg: IAktivitetslogg,
        validering: () -> Unit
    ) {
        oppdaterHistorikk(
            behandlingkilde = hendelse.metadata.behandlingkilde,
            dokumentsporing = inntektsmeldingDager(hendelse.metadata.meldingsreferanseId),
            hendelseSykdomstidslinje = bit.sykdomstidslinje,
            aktivitetslogg = aktivitetslogg,
            dagerNavOvertarAnsvar = bit.dagerNavOvertarAnsvar,
            validering = validering
        )
    }

    internal fun håndterHistorikkFraInfotrygd(
        hendelse: Utbetalingshistorikk,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (hendelse.vedtaksperiodeId != this.id) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        when (tilstand) {
            AvventerInfotrygdHistorikk -> when (yrkesaktivitet.yrkesaktivitetstype) {
                is Arbeidstaker -> tilstand(aktivitetsloggMedVedtaksperiodekontekst, AvventerInntektsmelding)
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                Behandlingsporing.Yrkesaktivitet.Frilans -> tilstand(aktivitetsloggMedVedtaksperiodekontekst, AvventerBlokkerendePeriode)

                Behandlingsporing.Yrkesaktivitet.Selvstendig -> error("Forventer ikke tilstanden AvventerInfotrygdHistorikk for vedtaksperiode opprettet av en søknad for Selvstendig næringsdrivende")
            }

            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerAOrdningen,
            AvventerInntektsmelding,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikkRevurdering,
            AvventerHistorikk,
            AvventerRevurdering,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            RevurderingFeilet,
            Start,
            TilUtbetaling,
            AvventerAnnullering,
            TilAnnullering,
            TilInfotrygd -> {
                /* gjør ingenting */
            }

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling -> {
                /* gjør ingenting */
            }

            SelvstendigAvventerInfotrygdHistorikk -> when (val yrkesaktivitet = yrkesaktivitet.yrkesaktivitetstype) {
                is Arbeidstaker,
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                Behandlingsporing.Yrkesaktivitet.Frilans -> error("Forventer ikke tilstanden SelvstendigAvventerInfotrygdHistorikk for vedtaksperiode opprettet av en søknad for $yrkesaktivitet")

                Behandlingsporing.Yrkesaktivitet.Selvstendig -> tilstand(aktivitetsloggMedVedtaksperiodekontekst, SelvstendigAvventerBlokkerendePeriode)
            }
        }
    }

    internal fun håndter(
        ytelser: Ytelser,
        aktivitetslogg: IAktivitetslogg,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        if (!ytelser.erRelevant(id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        if (tilstand !in setOf(AvventerHistorikk, AvventerHistorikkRevurdering, SelvstendigAvventerHistorikk))
            return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke ytelsehistorikk i %s".format(tilstand.type))

        håndterYtelser(ytelser, aktivitetsloggMedVedtaksperiodekontekst.medFeilSomVarslerHvisNødvendig(), infotrygdhistorikk)
    }

    fun inntekterForBeregning(beregningsperiode: Periode, inntektsperioder: List<Triple<Behandlingsporing.Yrkesaktivitet, Kilde, no.nav.helse.hendelser.InntekterForBeregning.Inntektsperiode>> = emptyList()): InntekterForBeregning {
        return with(InntekterForBeregning.Builder(beregningsperiode)) {
            vilkårsgrunnlag?.inntektsgrunnlag?.beverte(this)
            inntektsperioder.forEach { (yrkesaktivitet, kilde, inntektsperiode) ->
                inntektsendringer(
                    yrkesaktivitet = yrkesaktivitet,
                    fom = inntektsperiode.fom,
                    tom = inntektsperiode.tom,
                    inntekt = inntektsperiode.inntekt,
                    kilde = kilde
                )
            }
            build()
        }
    }
    private fun håndterYtelser(ytelser: Ytelser, aktivitetslogg: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk) {
        val grunnlagsdata = checkNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for ${skjæringstidspunkt}, men har ikke. Lages det utbetaling for en periode som ikke skal lage utbetaling?"
        }
        val perioderSomMåHensyntasVedBeregning = perioderSomMåHensyntasVedBeregning()
            .map { it.uberegnetVedtaksperiode() }
        val beregningsperiode = perioderSomMåHensyntasVedBeregning.map { it.periode }.reduce(Periode::plus)
        // steg 1: sett sammen alle inntekter som skal brukes i beregning
        val inntektsperioder = ytelser.inntektsendringer()
        val inntekterForBeregning = inntekterForBeregning(beregningsperiode, inntektsperioder)
        // steg 2: lag utbetalingstidslinjer for alle vedtaksperiodene
        val uberegnetTidslinjePerArbeidsgiver = lagUtbetalingstidslinjePerArbeidsgiver(perioderSomMåHensyntasVedBeregning, inntekterForBeregning)
        // steg 3: beregn alle utbetalingstidslinjer (avslå dager, beregne maksdato og utbetalingsbeløp)
        val harOpptjening = when (val opptjening = grunnlagsdata.opptjening) {
            is ArbeidstakerOpptjening -> opptjening.harTilstrekkeligAntallOpptjeningsdager()
            is SelvstendigNæringsdrivendeOpptjening -> true
            null -> true
        }
        val sykepengegrunnlag = grunnlagsdata.inntektsgrunnlag.sykepengegrunnlag
        val beregningsgrunnlag = grunnlagsdata.inntektsgrunnlag.beregningsgrunnlag
        val medlemskapstatus = (grunnlagsdata as? VilkårsgrunnlagHistorikk.Grunnlagsdata)?.medlemskapstatus
        val minsteinntektsvurdering = lagMinsteinntektsvurdering(skjæringstidspunkt, sykepengegrunnlag)
        // grunnlaget for maksdatoberegning er alt som har skjedd før,
        // frem til og med vedtaksperioden som beregnes
        val historisktidslinje = person.vedtaksperioder { it.periode.endInclusive < periode.start }
            .map { it.behandlinger.utbetalingstidslinje() }
            .fold(person.infotrygdhistorikk.utbetalingstidslinje(), Utbetalingstidslinje::plus)
        val beregnetTidslinjePerVedtaksperiode = filtrerUtbetalingstidslinjer(
            aktivitetslogg = aktivitetslogg,
            uberegnetTidslinjePerArbeidsgiver = uberegnetTidslinjePerArbeidsgiver,
            harOpptjening = harOpptjening,
            sykepengegrunnlag = sykepengegrunnlag,
            medlemskapstatus = medlemskapstatus,
            sekstisyvårsdagen = person.alder.redusertYtelseAlder,
            syttiårsdagen = person.alder.syttiårsdagen,
            dødsdato = person.alder.dødsdato,
            erUnderMinsteinntektskravTilFylte67 = minsteinntektsvurdering.erUnderMinsteinntektskravTilFylte67,
            erUnderMinsteinntektEtterFylte67 = minsteinntektsvurdering.erUnderMinsteinntektEtterFylte67,
            historisktidslinje = historisktidslinje,
            perioderMedMinimumSykdomsgradVurdertOK = person.minimumSykdomsgradsvurdering.perioder
        )
        // steg 4: lag et utbetalingsobjekt for vedtaksperioder som ikke har fått det enda (én per arbeidsgiver)
        val perioderDetSkalBeregnesUtbetalingFor = perioderDetSkalBeregnesUtbetalingFor()
        check(perioderDetSkalBeregnesUtbetalingFor.all { it.skjæringstidspunkt == this.skjæringstidspunkt }) {
            "ugyldig situasjon: skal beregne utbetaling for vedtaksperioder med ulike skjæringstidspunkter"
        }
        perioderDetSkalBeregnesUtbetalingFor.forEach { other ->
            val beregning = beregnetTidslinjePerVedtaksperiode.single { it.vedtaksperiodeId == other.id }
            other.lagNyUtbetaling(
                yrkesaktivitetSomBeregner = this.yrkesaktivitet,
                aktivitetslogg = other.registrerKontekst(aktivitetslogg),
                beregning = beregning,
                grunnlagsdata = grunnlagsdata
            )
        }

        // steg 5: lage varsler ved gitte situasjoner
        if (minsteinntektsvurdering.erUnderMinsteinntektskrav(person.alder.redusertYtelseAlder, periode))
            aktivitetslogg.varsel(RV_SV_1)
        else
            aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")

        if (person.alder.dødsdato != null && person.alder.dødsdato in periode) {
            aktivitetslogg.info("Utbetaling stoppet etter ${person.alder.dødsdato} grunnet dødsfall")
        }

        when (yrkesaktivitet.yrkesaktivitetstype) {
            is Arbeidstaker -> grunnlagsdata.valider(aktivitetslogg, yrkesaktivitet.organisasjonsnummer)

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans,
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> {}
        }
        if (!harOpptjening) aktivitetslogg.varsel(RV_OV_1)

        if (grunnlagsdata.inntektsgrunnlag.er6GBegrenset())
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")

        infotrygdhistorikk.validerMedVarsel(aktivitetslogg, periode)
        infotrygdhistorikk.validerNyereOpplysninger(aktivitetslogg, periode)
        ytelser.valider(aktivitetslogg, periode, skjæringstidspunkt, behandlinger.maksdato.maksdato, erForlengelse())

        // steg 6: subsummere ting
        minsteinntektsvurdering.subsummere(subsumsjonslogg, skjæringstidspunkt, beregningsgrunnlag, person.alder.redusertYtelseAlder, periode)

        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return forkast(ytelser, aktivitetslogg)

        val nesteTilstander = when (tilstand) {
            AvventerHistorikk -> AvventerSimulering to AvventerGodkjenning
            AvventerHistorikkRevurdering -> AvventerSimuleringRevurdering to AvventerGodkjenningRevurdering
            SelvstendigAvventerHistorikk -> SelvstendigAvventerSimulering to SelvstendigAvventerGodkjenning
            else -> error("Forventer ikke ytelsehåndtering i $tilstand")
        }

        val (simuleringtilstand, godkjenningtilstand) = nesteTilstander
        høstingsresultater(aktivitetslogg, simuleringtilstand, godkjenningtilstand)
    }

    internal fun håndterUtbetalingsavgjørelse(utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!utbetalingsavgjørelse.relevantVedtaksperiode(id)) return
        if (behandlinger.gjelderIkkeFor(utbetalingsavgjørelse)) return aktivitetsloggMedVedtaksperiodekontekst.info("Ignorerer løsning på utbetalingsavgjørelse, utbetalingid på løsningen matcher ikke vedtaksperiodens nåværende utbetaling")

        if (tilstand !in setOf(AvventerGodkjenning, AvventerGodkjenningRevurdering, SelvstendigAvventerGodkjenning)) return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke utbetalingsavgjørelse i %s".format(tilstand.type.name))

        val erAvvist = behandlinger.erAvvist()
        if (erAvvist) {
            if (tilstand in setOf(AvventerGodkjenning, SelvstendigAvventerGodkjenning)) return forkast(utbetalingsavgjørelse, aktivitetsloggMedVedtaksperiodekontekst, tvingForkasting = true)
            if (utbetalingsavgjørelse.automatisert) aktivitetsloggMedVedtaksperiodekontekst.info("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
            aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_UT_24)
        }

        behandlinger.vedtakFattet(yrkesaktivitet, utbetalingsavgjørelse, aktivitetsloggMedVedtaksperiodekontekst)

        if (erAvvist) return // er i limbo
        tilstand(
            aktivitetsloggMedVedtaksperiodekontekst,

            if (behandlinger.harUtbetalinger()) {
                when (yrkesaktivitet.yrkesaktivitetstype) {
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    is Arbeidstaker -> TilUtbetaling

                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigTilUtbetaling

                    Behandlingsporing.Yrkesaktivitet.Frilans -> TODO("Ikke implementert hva som skjer med frilans som har utbetaling")
                }
            } else {
                when (yrkesaktivitet.yrkesaktivitetstype) {
                    Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                    is Arbeidstaker -> Avsluttet

                    Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigAvsluttet

                    Behandlingsporing.Yrkesaktivitet.Frilans -> TODO("Ikke implementert hva som skjer med frilans som har utbetaling")
                }
            }
        )
    }

    internal fun håndter(sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver, aktivitetslogg: IAktivitetslogg): Boolean {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        return when (tilstand) {
            AvventerAOrdningen -> {
                if (!håndterSykepengegrunnlagForArbeidsgiver(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedVedtaksperiodekontekst)) return false
                tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
                true
            }

            AvventerRevurdering -> {
                if (!håndterSykepengegrunnlagForArbeidsgiver(sykepengegrunnlagForArbeidsgiver, aktivitetsloggMedVedtaksperiodekontekst)) return false
                person.gjenopptaBehandling(aktivitetslogg)
                true
            }

            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerInntektsmelding,
            AvventerBlokkerendePeriode,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            RevurderingFeilet,
            Start,
            TilInfotrygd,
            AvventerAnnullering,
            TilAnnullering,
            TilUtbetaling,

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling -> false
        }
    }

    private fun videreførEllerIngenRefusjon(sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver, aktivitetslogg: IAktivitetslogg) {
        videreførEksisterendeRefusjonsopplysninger(
            behandlingkilde = sykepengegrunnlagForArbeidsgiver.metadata.behandlingkilde,
            dokumentsporing = null,
            aktivitetslogg = aktivitetslogg
        )
        if (refusjonstidslinje.isNotEmpty()) return

        val ingenRefusjon = Beløpstidslinje.fra(
            periode = periode,
            beløp = INGEN,
            kilde = Kilde(
                sykepengegrunnlagForArbeidsgiver.metadata.meldingsreferanseId,
                sykepengegrunnlagForArbeidsgiver.metadata.avsender,
                sykepengegrunnlagForArbeidsgiver.metadata.innsendt
            )
        )
        behandlinger.håndterRefusjonstidslinje(
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = sykepengegrunnlagForArbeidsgiver.metadata.behandlingkilde,
            dokumentsporing = inntektFraAOrdingen(sykepengegrunnlagForArbeidsgiver.metadata.meldingsreferanseId),
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = yrkesaktivitet.beregnArbeidsgiverperiode(),
            refusjonstidslinje = ingenRefusjon
        )
    }

    private fun håndterSykepengegrunnlagForArbeidsgiver(sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver, aktivitetslogg: IAktivitetslogg): Boolean {
        if (sykepengegrunnlagForArbeidsgiver.skjæringstidspunkt != skjæringstidspunkt) {
            aktivitetslogg.info("Vilkårsgrunnlag var relevant for Vedtaksperiode, men skjæringstidspunktene var ulikte: [$skjæringstidspunkt, ${sykepengegrunnlagForArbeidsgiver.skjæringstidspunkt}]")
            return false
        }

        aktivitetslogg.info("Håndterer sykepengegrunnlag for arbeidsgiver")
        aktivitetslogg.varsel(RV_IV_10)

        val skatteopplysninger = sykepengegrunnlagForArbeidsgiver.inntekter()
        val omregnetÅrsinntekt = Skatteopplysning.omregnetÅrsinntekt(skatteopplysninger)

        yrkesaktivitet.lagreInntektFraAOrdningen(
            meldingsreferanseId = sykepengegrunnlagForArbeidsgiver.metadata.meldingsreferanseId,
            skjæringstidspunkt = skjæringstidspunkt,
            omregnetÅrsinntekt = omregnetÅrsinntekt
        )

        videreførEllerIngenRefusjon(sykepengegrunnlagForArbeidsgiver, aktivitetslogg)

        val event = PersonObserver.SkatteinntekterLagtTilGrunnEvent(
            yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = id,
            behandlingId = behandlinger.sisteBehandlingId,
            skjæringstidspunkt = skjæringstidspunkt,
            skatteinntekter = skatteopplysninger.map {
                PersonObserver.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(it.måned, it.beløp.månedlig)
            },
            omregnetÅrsinntekt = omregnetÅrsinntekt.årlig
        )
        person.sendSkatteinntekterLagtTilGrunn(event)
        return true
    }

    internal fun håndterVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        if (!vilkårsgrunnlag.erRelevant(aktivitetslogg, id, skjæringstidspunkt)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val nesteTilstand = when (tilstand) {
            AvventerVilkårsprøving -> AvventerHistorikk
            AvventerVilkårsprøvingRevurdering -> AvventerHistorikkRevurdering
            SelvstendigAvventerVilkårsprøving -> SelvstendigAvventerHistorikk
            else -> return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke vilkårsgrunnlag i %s".format(tilstand.type))
        }
        håndterVilkårsgrunnlag(vilkårsgrunnlag, aktivitetsloggMedVedtaksperiodekontekst.medFeilSomVarslerHvisNødvendig(), nesteTilstand)
    }

    internal fun håndterSimulering(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        if (simulering.vedtaksperiodeId != this.id.toString()) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val nesteTilstand = when (tilstand) {
            AvventerSimulering -> AvventerGodkjenning
            AvventerSimuleringRevurdering -> AvventerGodkjenningRevurdering
            SelvstendigAvventerSimulering -> SelvstendigAvventerGodkjenning
            else -> return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke simulering i %s".format(tilstand.type.name))
        }

        val wrapper = aktivitetsloggMedVedtaksperiodekontekst.medFeilSomVarslerHvisNødvendig()
        behandlinger.valider(simulering, wrapper)
        if (!behandlinger.erKlarForGodkjenning()) return wrapper.info("Kan ikke gå videre da begge oppdragene ikke er simulert.")
        tilstand(wrapper, nesteTilstand)
    }

    internal fun håndterUtbetalingHendelse(hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!behandlinger.håndterUtbetalinghendelse(hendelse, aktivitetsloggMedVedtaksperiodekontekst)) return
        tilstand.håndterUtbetalingHendelse(this, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndterAnnullerUtbetaling(
        hendelse: AnnullerUtbetaling,
        aktivitetslogg: IAktivitetslogg,
        annulleringskandidater: List<Vedtaksperiode>
    ): Revurderingseventyr? {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        if (!annulleringskandidater.contains(this)) return null

        val sisteVedtaksperiodeFørMegSelvMedSammenhengendeUtbetaling = yrkesaktivitet.finnSisteVedtaksperiodeFørMedSammenhengendeUtbetaling(this)
        val periodeForEndring = sisteVedtaksperiodeFørMegSelvMedSammenhengendeUtbetaling?.periode ?: periode

        when (tilstand) {
            Avsluttet,
            TilUtbetaling,
            SelvstendigTilUtbetaling,
            SelvstendigAvsluttet,

            AvventerSimuleringRevurdering,
            AvventerGodkjenningRevurdering,
            RevurderingFeilet,

            AvventerVilkårsprøvingRevurdering,
            AvventerHistorikkRevurdering,
            AvventerRevurdering -> {
                behandlinger.håndterAnnullering(
                    yrkesaktivitet = yrkesaktivitet,
                    behandlingkilde = hendelse.metadata.behandlingkilde,
                    aktivitetslogg = aktivitetsloggMedVedtaksperiodekontekst
                )
                tilstand(aktivitetsloggMedVedtaksperiodekontekst, AvventerAnnullering)
                return annullering(hendelse, periodeForEndring)
            }

            Start,
            SelvstendigStart,
            AvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAOrdningen,
            AvventerBlokkerendePeriode,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerSimulering,
            AvventerVilkårsprøving,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            TilAnnullering -> return null

            TilInfotrygd -> error("Forventet ikke annulleringshendelse i tilstand $tilstand for vedtaksperiodeId $id")
        }
    }

    internal fun håndterPåminnelse(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!påminnelse.erRelevant(id)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        return tilstand.påminnelse(this, påminnelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndterOverstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!overstyrArbeidsgiveropplysninger.erRelevant(skjæringstidspunkt)) return null
        if (vilkårsgrunnlag?.erArbeidsgiverRelevant(yrkesaktivitet.organisasjonsnummer) != true) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val grunnlag = vilkårsgrunnlag ?: return null
        val (nyttGrunnlag, endretInntektsgrunnlag) = grunnlag.overstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger, subsumsjonslogg) ?: return null
        person.lagreVilkårsgrunnlag(nyttGrunnlag)

        endretInntektsgrunnlag.inntekter
            .forEach {
                val opptjening = nyttGrunnlag.opptjening!! as ArbeidstakerOpptjening
                val opptjeningFom = opptjening.startdatoFor(it.inntektEtter.orgnummer)
                overstyrArbeidsgiveropplysninger.subsummer(subsumsjonslogg, opptjeningFom, it.inntektEtter.orgnummer)
            }

        val eventyr = Revurderingseventyr.arbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger, skjæringstidspunkt, skjæringstidspunkt)
        return eventyr
    }

    internal fun håndterOverstyrInntektsgrunnlag(overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!overstyrInntektsgrunnlag.erRelevant(skjæringstidspunkt)) return null
        val grunnlag = vilkårsgrunnlag ?: return null
        if (grunnlag.erArbeidsgiverRelevant(yrkesaktivitet.organisasjonsnummer) != true) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        // i praksis double-dispatch, kotlin-style
        val (nyttGrunnlag, revurderingseventyr) = when (overstyrInntektsgrunnlag) {
            is Grunnbeløpsregulering -> {
                val nyttGrunnlag = grunnlag.grunnbeløpsregulering(subsumsjonslogg)
                if (nyttGrunnlag == null) {
                    aktivitetsloggMedVedtaksperiodekontekst.info("Grunnbeløpet i sykepengegrunnlaget $skjæringstidspunkt er allerede korrekt.")
                } else {
                    aktivitetsloggMedVedtaksperiodekontekst.info("Grunnbeløpet i sykepengegrunnlaget $skjæringstidspunkt korrigeres til rett beløp.")
                }
                nyttGrunnlag to Revurderingseventyr.grunnbeløpsregulering(overstyrInntektsgrunnlag, skjæringstidspunkt)
            }

            is OverstyrArbeidsforhold -> {
                val nyttGrunnlag = grunnlag.overstyrArbeidsforhold(overstyrInntektsgrunnlag, subsumsjonslogg)
                nyttGrunnlag to Revurderingseventyr.arbeidsforhold(overstyrInntektsgrunnlag, skjæringstidspunkt)
            }

            is SkjønnsmessigFastsettelse -> {
                val nyttGrunnlag = grunnlag.skjønnsmessigFastsettelse(overstyrInntektsgrunnlag, subsumsjonslogg)
                nyttGrunnlag to Revurderingseventyr.skjønnsmessigFastsettelse(overstyrInntektsgrunnlag, skjæringstidspunkt, skjæringstidspunkt)
            }

            is OverstyrArbeidsgiveropplysninger -> error("Error. Det finnes en konkret dispatcher-konfigurasjon for dette tilfellet")
        }
        if (nyttGrunnlag == null) return null
        person.lagreVilkårsgrunnlag(nyttGrunnlag)
        return revurderingseventyr
    }

    internal fun håndterRefusjon(hendelse: Hendelse, dokumentsporing: Dokumentsporing, aktivitetslogg: IAktivitetslogg, servitør: Refusjonsservitør): Revurderingseventyr? {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val refusjonstidslinje = servitør.servér(startdatoPåSammenhengendeVedtaksperioder, periode)
        if (refusjonstidslinje.isEmpty()) return null
        if (!behandlinger.håndterRefusjonstidslinje(
                yrkesaktivitet,
                hendelse.metadata.behandlingkilde,
                dokumentsporing,
                aktivitetsloggMedVedtaksperiodekontekst,
                person.beregnSkjæringstidspunkt(),
                yrkesaktivitet.beregnArbeidsgiverperiode(),
                refusjonstidslinje
            )) return null
        return Revurderingseventyr.refusjonsopplysninger(hendelse, skjæringstidspunkt, periode)
    }

    override fun compareTo(other: Vedtaksperiode): Int {
        val delta = this.periode.start.compareTo(other.periode.start)
        if (delta != 0) return delta
        return this.periode.endInclusive.compareTo(other.periode.endInclusive)
    }

    internal infix fun før(other: Vedtaksperiode) = this < other
    internal infix fun etter(other: Vedtaksperiode) = this > other
    internal fun erVedtaksperiodeRettFør(other: Vedtaksperiode) =
        this.sykdomstidslinje.erRettFør(other.sykdomstidslinje)

    private fun erForlengelse(): Boolean = yrkesaktivitet
        .finnVedtaksperiodeRettFør(this)
        ?.takeIf { it.skalBehandlesISpeil() } != null

    internal fun manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(): Boolean {
        return vilkårsgrunnlag?.harNødvendigInntektForVilkårsprøving(yrkesaktivitet.organisasjonsnummer) == false
    }

    internal fun forkast(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, tvingForkasting: Boolean = false) {
        val forkastinger = forkastingskandidater(aktivitetslogg, tvingForkasting)
        person.søppelbøtte(hendelse, aktivitetslogg, forkastinger)
    }

    private fun forkastingskandidater(aktivitetslogg: IAktivitetslogg, tvingForkasting: Boolean): List<Vedtaksperiode> {
        val potensielle = person.vedtaksperioder(OVERLAPPENDE_OG_ETTERGØLGENDE(this))
        aktivitetslogg.info("Potensielt ${potensielle.size} vedtaksperioder vil bli forkastes")

        val vedtaksperioderSomSkalForkastes = potensielle
            .filter { kandidat -> kandidat.kanForkastes() }
            .onEach { it.registrerKontekst(aktivitetslogg).info("Kan forkastes fordi evt. overlappende utbetalinger er annullerte/forkastet") }

        if (tvingForkasting && this !in vedtaksperioderSomSkalForkastes) {
            aktivitetslogg.info("Behandlingene sier at denne _ikke_ kan forkastes. Men ettersom tvingForkasting er satt forkastes perioden læll. Ta en god titt på at det ikke blir hengende noen utbetalinger her!")
            return listOf(this) + vedtaksperioderSomSkalForkastes
        }
        return vedtaksperioderSomSkalForkastes
    }

    internal fun kanForkastes() =
        yrkesaktivitet.kanForkastes(this)

    internal fun tillaterBehandlingForkasting(vedtaksperioder: List<Vedtaksperiode>): Boolean {
        return behandlinger.kanForkastes(vedtaksperioder.map { it.behandlinger })
    }

    internal fun utførForkasting(
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ): VedtaksperiodeForkastetEventBuilder {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Forkaster vedtaksperiode: %s", this.id.toString())
        this.behandlinger.forkast(yrkesaktivitet, hendelse.metadata.behandlingkilde, hendelse.metadata.automatiskBehandling, aktivitetsloggMedVedtaksperiodekontekst)
        val vedtaksperiodeForkastetEventBuilder = when (tilstand) {
            // Vedtaksperioder i disse tilstandene har rukket å sende ut egne forespørsler før de ble forkastet
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerAOrdningen,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            RevurderingFeilet,
            TilInfotrygd,
            AvventerAnnullering,
            TilUtbetaling,
            TilAnnullering,

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling -> VedtaksperiodeForkastetEventBuilder()

            AvventerInfotrygdHistorikk,
            Start -> {
                VedtaksperiodeForkastetEventBuilder().apply {
                    yrkesaktivitet.trengerArbeidsgiveropplysninger(periode, ::trengerArbeidsgiveropplysninger)
                }
            }
        }
        tilstand(aktivitetsloggMedVedtaksperiodekontekst, TilInfotrygd)
        return vedtaksperiodeForkastetEventBuilder
    }

    internal inner class VedtaksperiodeForkastetEventBuilder {
        private val gjeldendeTilstand = tilstand.type
        private var sykmeldingsperioder: List<Periode> = emptyList()
        internal fun trengerArbeidsgiveropplysninger(sykmeldingsperioder: List<Periode>) {
            this.sykmeldingsperioder = sykmeldingsperioder
        }

        internal fun buildAndEmit() {
            person.vedtaksperiodeForkastet(
                PersonObserver.VedtaksperiodeForkastetEvent(
                    yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                    vedtaksperiodeId = id,
                    gjeldendeTilstand = gjeldendeTilstand,
                    hendelser = eksterneIderSet,
                    fom = periode.start,
                    tom = periode.endInclusive,
                    sykmeldingsperioder = sykmeldingsperioder,
                    speilrelatert = person.speilrelatert(periode)
                )
            )
        }
    }

    private fun registrerKontekst(aktivitetslogg: IAktivitetslogg): IAktivitetslogg {
        return aktivitetslogg
            .kontekst(yrkesaktivitet)
            .kontekst(this)
            .kontekst(behandlinger)
    }

    internal fun tilstand(
        event: IAktivitetslogg,
        nyTilstand: Vedtaksperiodetilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) return // Already in this state => ignore
        tilstand.leaving(this, event)

        val previousState = tilstand

        tilstand = nyTilstand
        oppdatert = LocalDateTime.now()
        block()

        emitVedtaksperiodeEndret(previousState)
        tilstand.entering(this, event)
    }

    private fun oppdaterHistorikk(
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing,
        hendelseSykdomstidslinje: Sykdomstidslinje,
        aktivitetslogg: IAktivitetslogg,
        dagerNavOvertarAnsvar: List<Periode>? = null,
        egenmeldingsdager: List<Periode>? = null,
        validering: () -> Unit
    ) {
        val haddeFlereSkjæringstidspunkt = behandlinger.harFlereSkjæringstidspunkt()
        behandlinger.håndterEndring(
            person = person,
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = behandlingkilde,
            dokumentsporing = dokumentsporing,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            egenmeldingsdager = egenmeldingsdager,
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = yrkesaktivitet.beregnArbeidsgiverperiode(),
            validering = validering
        )
        if (!haddeFlereSkjæringstidspunkt && behandlinger.harFlereSkjæringstidspunkt()) {
            aktivitetslogg.varsel(RV_IV_11)
        }
    }

    private fun håndterEgenmeldsingsdager(hendelse: Hendelse, dokumentsporing: Dokumentsporing?, aktivitetslogg: IAktivitetslogg, egenmeldingsdager: List<Periode>) = behandlinger.håndterEgenmeldingsdager(
        person = person,
        yrkesaktivitet = yrkesaktivitet,
        behandlingkilde = hendelse.metadata.behandlingkilde,
        dokumentsporing = dokumentsporing,
        aktivitetslogg = aktivitetslogg,
        beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
        beregnArbeidsgiverperiode = yrkesaktivitet.beregnArbeidsgiverperiode(),
        egenmeldingsdager = egenmeldingsdager
    )

    internal fun nullstillEgenmeldingsdagerIArbeidsgiverperiode(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, dokumentsporing: Dokumentsporing?): List<Revurderingseventyr> {
        val arbeidsgiverperiode = behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.periode ?: return emptyList()
        return yrkesaktivitet.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode)
            .filter { it.håndterEgenmeldsingsdager(hendelse, dokumentsporing, it.registrerKontekst(aktivitetslogg), emptyList()) }
            .map { Revurderingseventyr.arbeidsgiverperiode(hendelse, it.skjæringstidspunkt, it.periode) }
    }

    private fun håndterSøknad(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg
    ) {
        videreførEksisterendeRefusjonsopplysninger(søknad.metadata.behandlingkilde, søknad(søknad.metadata.meldingsreferanseId), aktivitetslogg)
        oppdaterHistorikk(søknad.metadata.behandlingkilde, søknad(søknad.metadata.meldingsreferanseId), søknad.sykdomstidslinje, aktivitetslogg) {
            søknad.valider(aktivitetslogg, vilkårsgrunnlag, refusjonstidslinje, subsumsjonslogg)
        }
    }

    private fun håndterOverlappendeSøknad(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (søknad.delvisOverlappende) return aktivitetslogg.funksjonellFeil(`Mottatt søknad som delvis overlapper`)
        aktivitetslogg.info("Håndterer overlappende søknad")
        håndterSøknad(søknad, aktivitetslogg)
    }

    private fun håndterOverlappendeSøknadRevurdering(søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Søknad har trigget en revurdering")
        oppdaterHistorikk(søknad.metadata.behandlingkilde, søknad(søknad.metadata.meldingsreferanseId), søknad.sykdomstidslinje, aktivitetslogg) {
            if (søknad.delvisOverlappende) aktivitetslogg.varsel(`Mottatt søknad som delvis overlapper`)
            søknad.valider(FunksjonelleFeilTilVarsler(aktivitetslogg), vilkårsgrunnlag, refusjonstidslinje, subsumsjonslogg)
        }
    }

    internal fun håndterKorrigerendeInntektsmelding(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val opprinneligAgp = behandlinger.arbeidsgiverperiode()
        if (dager.erKorrigeringForGammel(aktivitetslogg, opprinneligAgp.arbeidsgiverperiode.dager)) {
            håndterDagerUtenEndring(dager, aktivitetslogg)
        } else {
            håndterDager(dager, aktivitetslogg)
        }

        val nyAgp = behandlinger.arbeidsgiverperiode()
        if (opprinneligAgp == nyAgp) return

        aktivitetslogg.varsel(RV_IM_24, "Ny agp er utregnet til å være ulik tidligere utregnet agp i ${tilstand.type.name}")
    }

    private fun inntektForArbeidsgiver(
        hendelse: Hendelse,
        aktivitetsloggTilDenSomVilkårsprøver: IAktivitetslogg,
        skatteopplysning: SkatteopplysningerForSykepengegrunnlag?,
        alleForSammeArbeidsgiver: List<Vedtaksperiode>
    ): ArbeidstakerFaktaavklartInntekt {
        val inntektForArbeidsgiver = yrkesaktivitet
            .avklarInntekt(skjæringstidspunkt, alleForSammeArbeidsgiver)
            // velger bort inntekten hvis situasjonen er "fom ulik skjæringstidspunktet"
            ?.takeUnless {
                (skjæringstidspunkt.yearMonth < it.inntektsdata.dato.yearMonth).also { harUlikFom ->
                    if (harUlikFom) aktivitetsloggTilDenSomVilkårsprøver.varsel(Varselkode.RV_VV_2)
                }
            }

        val (inntektsdata, opplysning) = if (inntektForArbeidsgiver != null)
            inntektForArbeidsgiver.inntektsdata to when (inntektForArbeidsgiver.kilde) {
                Inntektsmeldinginntekt.Kilde.Arbeidsgiver -> Arbeidstakerinntektskilde.Arbeidsgiver
                Inntektsmeldinginntekt.Kilde.AOrdningen -> Arbeidstakerinntektskilde.AOrdningen.fraSkatt()
            }
        else
            (skatteopplysning?.inntektsdata ?: Inntektsdata.ingen(hendelse.metadata.meldingsreferanseId, skjæringstidspunkt)) to Arbeidstakerinntektskilde.AOrdningen.fraSkatt(skatteopplysning?.treMånederFørSkjæringstidspunkt)

        if (opplysning is Arbeidstakerinntektskilde.AOrdningen)
            subsummerBrukAvSkatteopplysninger(yrkesaktivitet.organisasjonsnummer, inntektsdata, skatteopplysning?.treMånederFørSkjæringstidspunkt ?: emptyList())

        return ArbeidstakerFaktaavklartInntekt(
            id = UUID.randomUUID(),
            inntektsdata = inntektsdata,
            inntektsopplysningskilde = opplysning
        )
    }

    private fun avklarSykepengegrunnlagArbeidstaker(
        hendelse: Hendelse,
        aktivitetsloggTilDenSomVilkårsprøver: IAktivitetslogg,
        skatteopplysning: SkatteopplysningerForSykepengegrunnlag?,
        vedtaksperioderMedSammeSkjæringstidspunkt: List<Vedtaksperiode>
    ): ArbeidsgiverInntektsopplysning {
        check(yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker) {
            "Skal kun avklare sykepengegrunnlag for arbeidstakere"
        }
        val alleForSammeArbeidsgiver = vedtaksperioderMedSammeSkjæringstidspunkt
            .filter { it.yrkesaktivitet === this.yrkesaktivitet }

        return ArbeidsgiverInntektsopplysning(
            orgnummer = yrkesaktivitet.organisasjonsnummer,
            faktaavklartInntekt = inntektForArbeidsgiver(hendelse, aktivitetsloggTilDenSomVilkårsprøver, skatteopplysning, alleForSammeArbeidsgiver),
            korrigertInntekt = null,
            skjønnsmessigFastsatt = null
        )
    }

    private fun avklarSykepengegrunnlagForSelvstendig(): SelvstendigInntektsopplysning? {
        return person
            .vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .firstOrNull { it.yrkesaktivitet.yrkesaktivitetstype is Behandlingsporing.Yrkesaktivitet.Selvstendig }
            ?.inntektForSelvstendig()
    }

    private fun inntektForSelvstendig(): SelvstendigInntektsopplysning {
        val faktaavklartInntekt = checkNotNull(behandlinger.faktaavklartInntekt) { "Forventer å ha en inntekt for selvstendig" }
        val inntektsgrunnlag = faktaavklartInntekt.beregnInntektsgrunnlag(`1G`.beløp(skjæringstidspunkt))

        val inntekt = faktaavklartInntekt.copy(inntektsdata = faktaavklartInntekt.inntektsdata.copy(beløp = inntektsgrunnlag))

        return SelvstendigInntektsopplysning(
            faktaavklartInntekt = inntekt,
            skjønnsmessigFastsatt = null
        )
    }

    private fun subsummerBrukAvSkatteopplysninger(orgnummer: String, inntektsdata: Inntektsdata, skatteopplysninger: List<Skatteopplysning>) {
        val inntekter = skatteopplysninger.subsumsjonsformat()
        subsumsjonslogg.logg(
            `§ 8-28 ledd 3 bokstav a`(
                organisasjonsnummer = orgnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                inntekterSisteTreMåneder = inntekter,
                grunnlagForSykepengegrunnlagÅrlig = inntektsdata.beløp.årlig,
                grunnlagForSykepengegrunnlagMånedlig = inntektsdata.beløp.månedlig
            )
        )
        subsumsjonslogg.logg(
            `§ 8-29`(
                skjæringstidspunkt = skjæringstidspunkt,
                grunnlagForSykepengegrunnlagÅrlig = inntektsdata.beløp.årlig,
                inntektsopplysninger = inntekter,
                organisasjonsnummer = orgnummer
            )
        )
    }

    private fun inntektsgrunnlagArbeidsgivere(
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        skatteopplysninger: List<SkatteopplysningerForSykepengegrunnlag>
    ): List<ArbeidsgiverInntektsopplysning> {
        // hvilke arbeidsgivere skal inngå i sykepengegrunnlaget?
        // de vi har søknad for på skjæringstidspunktet er jo et godt utgangspunkt 👍
        val perioderMedSammeSkjæringstidspunkt = person
            .vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .filter { it.yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker }

        // en inntekt per arbeidsgiver med søknad
        return perioderMedSammeSkjæringstidspunkt
            .distinctBy { it.yrkesaktivitet }
            .map { vedtaksperiode ->
                val skatteopplysningForArbeidsgiver = skatteopplysninger.firstOrNull { it.arbeidsgiver == vedtaksperiode.yrkesaktivitet.organisasjonsnummer }
                vedtaksperiode.avklarSykepengegrunnlagArbeidstaker(hendelse, aktivitetslogg, skatteopplysningForArbeidsgiver, perioderMedSammeSkjæringstidspunkt)
            }
    }

    private fun ghostArbeidsgivere(arbeidsgivere: List<ArbeidsgiverInntektsopplysning>, skatteopplysninger: List<SkatteopplysningerForSykepengegrunnlag>): List<ArbeidsgiverInntektsopplysning> {
        return skatteopplysninger
            .filter { skatteopplysning -> arbeidsgivere.none { it.orgnummer == skatteopplysning.arbeidsgiver } }
            .filter { skatteopplysning -> skatteopplysning.erGhostarbeidsgiver }
            .map { skatteopplysning ->
                // vi er ghost, ingen søknader på skjæringstidspunktet og inntekten fra skatt anses som ghost
                subsummerBrukAvSkatteopplysninger(skatteopplysning.arbeidsgiver, skatteopplysning.inntektsdata, skatteopplysning.treMånederFørSkjæringstidspunkt)
                ArbeidsgiverInntektsopplysning(
                    orgnummer = skatteopplysning.arbeidsgiver,
                    faktaavklartInntekt = ArbeidstakerFaktaavklartInntekt(
                        id = UUID.randomUUID(),
                        inntektsdata = skatteopplysning.inntektsdata,
                        inntektsopplysningskilde = Arbeidstakerinntektskilde.AOrdningen.fraSkatt(skatteopplysning.treMånederFørSkjæringstidspunkt)
                    ),
                    korrigertInntekt = null,
                    skjønnsmessigFastsatt = null
                )
            }
    }

    private fun avklarSykepengegrunnlag(
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        skatteopplysninger: List<SkatteopplysningerForSykepengegrunnlag>
    ): Inntektsgrunnlag {
        val inntektsgrunnlagArbeidsgivere = inntektsgrunnlagArbeidsgivere(hendelse, aktivitetslogg, skatteopplysninger)
        val inntektsgrunnlagSelvstendig = avklarSykepengegrunnlagForSelvstendig()
        // ghosts er alle inntekter fra skatt, som vi ikke har søknad for og som skal vektlegges som ghost
        val ghosts = ghostArbeidsgivere(inntektsgrunnlagArbeidsgivere, skatteopplysninger)
        val erKombinert = inntektsgrunnlagSelvstendig != null && (inntektsgrunnlagArbeidsgivere.isNotEmpty() || ghosts.isNotEmpty())

        if (ghosts.isNotEmpty()) aktivitetslogg.varsel(Varselkode.RV_VV_2)
        if (erKombinert) aktivitetslogg.funksjonellFeil(Varselkode.RV_IV_13)

        return Inntektsgrunnlag.opprett(
            arbeidsgiverInntektsopplysninger = inntektsgrunnlagArbeidsgivere + ghosts,
            selvstendigInntektsopplysning = inntektsgrunnlagSelvstendig,
            deaktiverteArbeidsforhold = emptyList(),
            skjæringstidspunkt = skjæringstidspunkt,
            subsumsjonslogg = subsumsjonslogg
        )
    }

    private fun håndterVilkårsgrunnlag(
        vilkårsgrunnlag: Vilkårsgrunnlag,
        aktivitetslogg: IAktivitetslogg,
        nesteTilstand: Vedtaksperiodetilstand
    ) {
        val skatteopplysninger = vilkårsgrunnlag.skatteopplysninger()

        val sykepengegrunnlag = avklarSykepengegrunnlag(
            hendelse = vilkårsgrunnlag,
            aktivitetslogg = aktivitetslogg,
            skatteopplysninger = skatteopplysninger
        )
        val selvstendigOpptjening = when (yrkesaktivitet.yrkesaktivitetstype) {
            is Behandlingsporing.Yrkesaktivitet.Selvstendig -> {
                val erOpptjeningVurdertOk = behandlinger.forberedendeVilkårsgrunnlag?.erOpptjeningVurdertOk
                check(erOpptjeningVurdertOk == true) { "Opptjening for selvstendige er ikke vurdert ok eller finnes ikke. Det skal vel ikke skje?" }
                SelvstendigOpptjeningOppfylt
            }

            is Arbeidstaker -> SelvstendigOpptjeningIkkeVurdert

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans -> error("Hvorfor er vi her? Litt rart å vurdere selvstendigopptjening for arbeidsledige og frilans per nå")
        }

        vilkårsgrunnlag.valider(aktivitetslogg, sykepengegrunnlag, selvstendigOpptjening, subsumsjonslogg)
        val grunnlagsdata = vilkårsgrunnlag.grunnlagsdata()

        when (yrkesaktivitet.yrkesaktivitetstype) {
            is Arbeidstaker -> grunnlagsdata.validerFørstegangsvurderingArbeidstaker(aktivitetslogg)

            Behandlingsporing.Yrkesaktivitet.Selvstendig -> grunnlagsdata.validerFørstegangsvurderingSelvstendig(aktivitetslogg, subsumsjonslogg)

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans -> error("Hvorfor er vi her? Litt rart å validere opptjening for arbeidsledige og frilans per nå")
        }
        person.lagreVilkårsgrunnlag(grunnlagsdata)
        aktivitetslogg.info("Vilkårsgrunnlag vurdert")
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return forkast(vilkårsgrunnlag, aktivitetslogg)
        tilstand(aktivitetslogg, nesteTilstand)
    }

    internal fun håndterUtbetalingHendelse(aktivitetslogg: IAktivitetslogg) {
        if (!aktivitetslogg.harFunksjonelleFeilEllerVerre()) return
        aktivitetslogg.funksjonellFeil(RV_UT_5)
    }

    internal fun trengerYtelser(aktivitetslogg: IAktivitetslogg) {
        val søkevinduFamilieytelser = periode.familieYtelserPeriode
        foreldrepenger(aktivitetslogg, søkevinduFamilieytelser)
        pleiepenger(aktivitetslogg, søkevinduFamilieytelser)
        omsorgspenger(aktivitetslogg, søkevinduFamilieytelser)
        opplæringspenger(aktivitetslogg, søkevinduFamilieytelser)
        institusjonsopphold(aktivitetslogg, periode)
        arbeidsavklaringspenger(aktivitetslogg, periode.start.minusMonths(6), periode.endInclusive)
        dagpenger(aktivitetslogg, periode.start.minusMonths(2), periode.endInclusive)
        inntekterForBeregning(aktivitetslogg, perioderSomMåHensyntasVedBeregning().map { it.periode }.reduce(Periode::plus))
    }

    internal fun trengerVilkårsgrunnlag(aktivitetslogg: IAktivitetslogg) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntekterForSykepengegrunnlag(aktivitetslogg, skjæringstidspunkt, beregningSlutt.minusMonths(2), beregningSlutt)
        inntekterForOpptjeningsvurdering(aktivitetslogg, skjæringstidspunkt, beregningSlutt, beregningSlutt)
        arbeidsforhold(aktivitetslogg, skjæringstidspunkt)
        medlemskap(aktivitetslogg, skjæringstidspunkt, periode.start, periode.endInclusive)
    }

    internal fun trengerInntektFraSkatt(aktivitetslogg: IAktivitetslogg) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntekterForSykepengegrunnlagForArbeidsgiver(
            aktivitetslogg,
            skjæringstidspunkt,
            yrkesaktivitet.organisasjonsnummer,
            beregningSlutt.minusMonths(2),
            beregningSlutt
        )
    }

    private fun opplysningerViTrenger(): Set<PersonObserver.ForespurtOpplysning> {
        if (!skalBehandlesISpeil()) return emptySet() // perioden er AUU ✋

        if (yrkesaktivitet.finnVedtaksperiodeRettFør(this)?.skalBehandlesISpeil() == true) return emptySet() // Da har perioden foran oss spurt for oss/ vi har det vi trenger ✋

        val opplysninger = mutableSetOf<PersonObserver.ForespurtOpplysning>().apply {
            if (!harEksisterendeInntekt()) addAll(setOf(PersonObserver.Inntekt, PersonObserver.Refusjon)) // HAG støtter ikke skjema uten refusjon, så når vi først spør om inntekt _må_ vi også spørre om refusjon
            if (refusjonstidslinje.isEmpty()) add(PersonObserver.Refusjon) // For de tilfellene vi faktiske trenger refusjon
        }
        if (opplysninger.isEmpty()) return emptySet() // Om vi har inntekt og refusjon så er saken biff 🥩

        if (behandlinger.dagerNavOvertarAnsvar.isNotEmpty()) return opplysninger // Trenger hvert fall ikke opplysninger om arbeidsgiverperiode dersom Nav har overtatt ansvar for den ✋

        return opplysninger.apply {
            val sisteDelAvAgp = behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.dager.lastOrNull()
            // Vi "trenger" jo aldri AGP, men spør om vi perioden overlapper/er rett etter beregnet AGP
            if (sisteDelAvAgp?.overlapperMed(periode) == true || sisteDelAvAgp?.erRettFør(periode) == true) {
                add(PersonObserver.Arbeidsgiverperiode)
            }
        }
    }

    internal fun sendTrengerArbeidsgiveropplysninger() {
        val forespurteOpplysninger = opplysningerViTrenger().takeUnless { it.isEmpty() } ?: return
        person.trengerArbeidsgiveropplysninger(trengerArbeidsgiveropplysninger(forespurteOpplysninger))

        // ved out-of-order gir vi beskjed om at vi ikke trenger arbeidsgiveropplysninger for den seneste perioden lenger
        yrkesaktivitet.finnVedtaksperiodeRettEtter(this)?.trengerIkkeArbeidsgiveropplysninger()
    }

    private fun trengerArbeidsgiveropplysninger(
        forespurteOpplysninger: Set<PersonObserver.ForespurtOpplysning>
    ): PersonObserver.TrengerArbeidsgiveropplysningerEvent {
        val vedtaksperioder = when {
            // For å beregne riktig arbeidsgiverperiode/første fraværsdag
            PersonObserver.Arbeidsgiverperiode in forespurteOpplysninger -> vedtaksperioderIArbeidsgiverperiodeTilOgMedDenne()
            // Dersom vi ikke trenger å beregne arbeidsgiverperiode/første fravarsdag trenger vi bare denne sykemeldingsperioden
            else -> listOf(this)
        }
        return PersonObserver.TrengerArbeidsgiveropplysningerEvent(
            personidentifikator = person.personidentifikator,
            yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = id,
            skjæringstidspunkt = skjæringstidspunkt,
            sykmeldingsperioder = sykmeldingsperioder(vedtaksperioder),
            egenmeldingsperioder = vedtaksperioder.egenmeldingsperioder(),
            førsteFraværsdager = førsteFraværsdagerForForespørsel(),
            forespurteOpplysninger = forespurteOpplysninger
        )
    }

    private fun førsteFraværsdagerForForespørsel(): List<PersonObserver.FørsteFraværsdag> {
        val deAndre = person.vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(this.skjæringstidspunkt))
            .filterNot { it.yrkesaktivitet === this.yrkesaktivitet }
            .groupBy { it.yrkesaktivitet }
            .mapNotNull { (arbeidsgiver, perioder) ->
                val førsteFraværsdagForArbeidsgiver = perioder
                    .asReversed()
                    .firstNotNullOfOrNull { it.førsteFraværsdag }
                førsteFraværsdagForArbeidsgiver?.let {
                    PersonObserver.FørsteFraværsdag(arbeidsgiver.yrkesaktivitetstype, it)
                }
            }
        val minEgen = førsteFraværsdag?.let {
            PersonObserver.FørsteFraværsdag(yrkesaktivitet.yrkesaktivitetstype, it)
        } ?: return deAndre
        return deAndre.plusElement(minEgen)
    }

    private fun vedtaksperioderIArbeidsgiverperiodeTilOgMedDenne(): List<Vedtaksperiode> {
        val arbeidsgiverperiode = behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.periode ?: return listOf(this)
        return yrkesaktivitet.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).filter { it <= this }
    }

    private fun trengerIkkeArbeidsgiveropplysninger() {
        person.trengerIkkeArbeidsgiveropplysninger(
            PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent(
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                vedtaksperiodeId = id
            )
        )
    }

    internal fun trengerInntektsmeldingReplay() {
        val erKortPeriode = !skalBehandlesISpeil()
        val opplysningerViTrenger = if (erKortPeriode) opplysningerViTrenger() + PersonObserver.Arbeidsgiverperiode else opplysningerViTrenger()

        person.inntektsmeldingReplay(trengerArbeidsgiveropplysninger(opplysningerViTrenger))
    }

    private fun emitVedtaksperiodeEndret(previousState: Vedtaksperiodetilstand) {
        val event = PersonObserver.VedtaksperiodeEndretEvent(
            yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = id,
            behandlingId = behandlinger.sisteBehandlingId,
            gjeldendeTilstand = tilstand.type,
            forrigeTilstand = previousState.type,
            hendelser = eksterneIderSet,
            makstid = makstid(),
            fom = periode.start,
            tom = periode.endInclusive,
            skjæringstidspunkt = skjæringstidspunkt
        )

        person.vedtaksperiodeEndret(event)
    }

    override fun avsluttetUtenVedtak(
        aktivitetslogg: IAktivitetslogg,
        behandlingId: UUID,
        tidsstempel: LocalDateTime,
        periode: Periode,
        dekkesAvArbeidsgiverperioden: Boolean,
        dokumentsporing: Set<UUID>
    ) {
        if (dekkesAvArbeidsgiverperioden) {
            subsumsjonslogg.logg(`§ 8-17 ledd 1 bokstav a - arbeidsgiversøknad`(periode, sykdomstidslinje.subsumsjonsformat()))
        }
        person.avsluttetUtenVedtak(
            PersonObserver.AvsluttetUtenVedtakEvent(
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                vedtaksperiodeId = id,
                behandlingId = behandlingId,
                periode = periode,
                hendelseIder = eksterneIderSet,
                skjæringstidspunkt = skjæringstidspunkt,
                avsluttetTidspunkt = tidsstempel
            )
        )
        person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun vedtakIverksatt(
        aktivitetslogg: IAktivitetslogg,
        vedtakFattetTidspunkt: LocalDateTime,
        behandling: Behandlinger.Behandling
    ) {
        val utkastTilVedtakBuilder = utkastTilVedtakBuilder()
        // Til ettertanke: Her er vi aldri innom "behandlinger"-nivå, så får ikke "Grunnbeløpsregulering"-tag, men AvsluttetMedVedtak har jo ikke tags nå uansett.
        behandling.berik(utkastTilVedtakBuilder)
        person.avsluttetMedVedtak(utkastTilVedtakBuilder.buildAvsluttedMedVedtak(vedtakFattetTidspunkt, eksterneIder))
        person.analytiskDatapakke(behandlinger.analytiskDatapakke(this.yrkesaktivitet.yrkesaktivitetstype, this.id))
        person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun vedtakAnnullert(aktivitetslogg: IAktivitetslogg, behandlingId: UUID) {
        person.vedtaksperiodeAnnullert(
            PersonObserver.VedtaksperiodeAnnullertEvent(
                fom = periode.start,
                tom = periode.endInclusive,
                vedtaksperiodeId = id,
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                behandlingId = behandlingId
            )
        )
    }

    override fun behandlingLukket(behandlingId: UUID) {
        person.behandlingLukket(
            PersonObserver.BehandlingLukketEvent(
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                vedtaksperiodeId = id,
                behandlingId = behandlingId
            )
        )
    }

    override fun behandlingForkastet(behandlingId: UUID, automatiskBehandling: Boolean) {
        person.behandlingForkastet(
            PersonObserver.BehandlingForkastetEvent(
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                vedtaksperiodeId = id,
                behandlingId = behandlingId,
                automatiskBehandling = automatiskBehandling
            )
        )
    }

    override fun nyBehandling(
        id: UUID,
        periode: Periode,
        meldingsreferanseId: MeldingsreferanseId,
        innsendt: LocalDateTime,
        registert: LocalDateTime,
        avsender: Avsender,
        type: PersonObserver.BehandlingOpprettetEvent.Type,
        søknadIder: Set<MeldingsreferanseId>
    ) {
        val event = PersonObserver.BehandlingOpprettetEvent(
            yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = this.id,
            søknadIder = (behandlinger.søknadIder() + søknadIder).map { it.id }.toSet(),
            behandlingId = id,
            fom = periode.start,
            tom = periode.endInclusive,
            type = type,
            kilde = PersonObserver.BehandlingOpprettetEvent.Kilde(meldingsreferanseId.id, innsendt, registert, avsender)
        )
        person.nyBehandling(event)
    }

    override fun utkastTilVedtak(utkastTilVedtak: PersonObserver.UtkastTilVedtakEvent) {
        person.utkastTilVedtak(utkastTilVedtak)
    }

    private fun høstingsresultater(
        aktivitetslogg: IAktivitetslogg,
        simuleringtilstand: Vedtaksperiodetilstand,
        godkjenningtilstand: Vedtaksperiodetilstand
    ) = when {
        behandlinger.harUtbetalinger() -> tilstand(aktivitetslogg, simuleringtilstand) {
            aktivitetslogg.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
        }

        else -> tilstand(aktivitetslogg, godkjenningtilstand) {
            aktivitetslogg.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
        }
    }

    private fun Vedtaksperiodetilstand.påminnelse(
        vedtaksperiode: Vedtaksperiode,
        påminnelse: Påminnelse,
        aktivitetslogg: IAktivitetslogg
    ): Revurderingseventyr? {
        if (!påminnelse.gjelderTilstand(aktivitetslogg, type)) {
            vedtaksperiode.person.vedtaksperiodeIkkePåminnet(id, yrkesaktivitet.organisasjonsnummer, type)
            return null
        }
        vedtaksperiode.person.vedtaksperiodePåminnet(id, yrkesaktivitet.organisasjonsnummer, påminnelse)
        val beregnetMakstid = { tilstandsendringstidspunkt: LocalDateTime -> makstid(tilstandsendringstidspunkt) }
        if (påminnelse.nåddMakstid(beregnetMakstid)) {
            håndterMakstid(vedtaksperiode, påminnelse, aktivitetslogg)
            return null
        }

        val overstyring = when (påminnelse.når(Flagg("nullstillEgenmeldingsdager"))) {
            true -> nullstillEgenmeldingsdagerIArbeidsgiverperiode(påminnelse, aktivitetslogg, null).tidligsteEventyr()
            false -> påminnelse.eventyr(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
        }

        if (overstyring != null) {
            aktivitetslogg.info("Reberegner perioden ettersom det er ønsket")
            return overstyring
        }
        håndterPåminnelse(vedtaksperiode, påminnelse, aktivitetslogg)
        return null
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    internal fun trengerGodkjenning(aktivitetslogg: IAktivitetslogg) {
        behandlinger.godkjenning(aktivitetslogg, utkastTilVedtakBuilder())
    }

    private fun utkastTilVedtakBuilder(): UtkastTilVedtakBuilder {
        val builder = UtkastTilVedtakBuilder(
            yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = id,
            kanForkastes = kanForkastes(),
            erForlengelse = erForlengelse(),
            harPeriodeRettFør = yrkesaktivitet.finnVedtaksperiodeRettFør(this) != null,
            overlapperMedInfotrygd = person.erBehandletIInfotrygd(periode)
        )
        person.vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .sorted()
            .associate { it.id to it.behandlinger }
            .berik(builder)

        return builder
    }

    internal fun gjenopptaBehandling(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Forsøker å gjenoppta $this")
        tilstand.gjenopptaBehandling(this, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun igangsettOverstyring(revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (revurdering.erIkkeRelevantFor(periode)) return sendNyttGodkjenningsbehov(aktivitetsloggMedVedtaksperiodekontekst)
        tilstand.igangsettOverstyring(this, revurdering, aktivitetsloggMedVedtaksperiodekontekst)
        videreførEksisterendeOpplysninger(revurdering.hendelse.metadata.behandlingkilde, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun sendNyttGodkjenningsbehov(aktivitetslogg: IAktivitetslogg) {
        if (this.tilstand !in setOf(AvventerGodkjenningRevurdering, AvventerGodkjenning, SelvstendigAvventerGodkjenning)) return
        this.trengerGodkjenning(aktivitetslogg)
    }

    internal fun inngåIRevurderingseventyret(
        vedtaksperioder: MutableList<PersonObserver.OverstyringIgangsatt.VedtaksperiodeData>,
        typeEndring: String
    ) {
        vedtaksperioder.add(
            PersonObserver.OverstyringIgangsatt.VedtaksperiodeData(
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                vedtaksperiodeId = id,
                skjæringstidspunkt = skjæringstidspunkt,
                periode = periode,
                typeEndring = typeEndring
            )
        )
    }

    // gitt at du står i tilstand X, hva/hvem henter du på og hvorfor?
    internal val venterPå
        get() = when (val t = tilstand) {
            AvsluttetUtenUtbetaling -> when (skalBehandlesISpeil()) {
                true -> VenterPå.SegSelv(Venteårsak.HJELP fordi Venteårsak.Hvorfor.VIL_OMGJØRES)
                false -> null
            }

            AvventerGodkjenning -> when (behandlinger.erAvvist()) {
                true -> VenterPå.SegSelv(Venteårsak.HJELP)
                false -> VenterPå.SegSelv(Venteårsak.GODKJENNING)
            }

            AvventerGodkjenningRevurdering -> when (behandlinger.erAvvist()) {
                true -> VenterPå.SegSelv(Venteårsak.HJELP)
                false -> VenterPå.SegSelv(Venteårsak.GODKJENNING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)
            }

            RevurderingFeilet -> when (kanForkastes()) {
                true -> null
                false -> VenterPå.SegSelv(Venteårsak.HJELP)
            }

            SelvstendigAvventerGodkjenning -> when (behandlinger.erAvvist()) {
                true -> VenterPå.SegSelv(Venteårsak.HJELP)
                false -> VenterPå.SegSelv(Venteårsak.GODKJENNING)
            }

            // disse to er litt spesielle, fordi tilstanden er både en ventetilstand og en "det er min tur"-tilstand
            is AvventerBlokkerendePeriode -> t.venterpå(this)
            is AvventerRevurdering -> t.venterpå(this)

            AvventerAnnullering,
            SelvstendigAvventerBlokkerendePeriode -> VenterPå.Nestemann

            AvventerInntektsmelding -> VenterPå.SegSelv(Venteårsak.INNTEKTSMELDING)

            AvventerHistorikk,
            SelvstendigAvventerHistorikk -> VenterPå.SegSelv(Venteårsak.BEREGNING)

            AvventerHistorikkRevurdering -> VenterPå.SegSelv(Venteårsak.BEREGNING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)
            AvventerSimuleringRevurdering -> VenterPå.SegSelv(Venteårsak.UTBETALING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)

            AvventerSimulering,
            SelvstendigAvventerSimulering,
            SelvstendigTilUtbetaling,
            TilAnnullering,
            TilUtbetaling -> VenterPå.SegSelv(Venteårsak.UTBETALING)

            AvventerInfotrygdHistorikk,
            AvventerVilkårsprøving,
            AvventerAOrdningen,
            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerVilkårsprøving,
            Start,
            SelvstendigStart,
            Avsluttet,
            SelvstendigAvsluttet,
            TilInfotrygd -> null
        }

    // Hvem venter jeg på? Og hvorfor?
    internal val vedtaksperiodeVenter: VedtaksperiodeVenter? get() = venterPå?.let { venter(it) }

    private fun venter(venterPå: VenterPå) =
        VedtaksperiodeVenter(
            vedtaksperiodedata = venter(),
            venterPå = venterPå
        )

    internal fun venter() = VedtaksperiodeVenterdata(
        yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
        vedtaksperiodeId = id,
        behandlingId = behandlinger.sisteBehandlingId,
        skjæringstidspunkt = skjæringstidspunkt,
        hendelseIder = eksterneIderSet,
        ventetSiden = oppdatert,
        venterTil = makstid()
    )

    private fun makstid(tilstandsendringstidspunkt: LocalDateTime = oppdatert) =
        tilstand.makstid(this, tilstandsendringstidspunkt)

    fun slutterEtter(dato: LocalDate) = periode.slutterEtter(dato)

    private fun lagNyUtbetaling(
        yrkesaktivitetSomBeregner: Yrkesaktivitet,
        aktivitetslogg: IAktivitetslogg,
        beregning: BeregnetPeriode,
        grunnlagsdata: VilkårsgrunnlagElement
    ) {
        behandlinger.nyUtbetaling(
            vedtaksperiodeSomLagerUtbetaling = this.id,
            yrkesaktivitet = this.yrkesaktivitet,
            aktivitetslogg = aktivitetslogg,
            beregning = beregning,
            grunnlagsdata = grunnlagsdata
        )
        val subsumsjonen = Utbetalingstidslinjesubsumsjon(this.subsumsjonslogg, this.sykdomstidslinje, beregning.utbetalingstidslinje)
        subsumsjonen.subsummer(periode, this.yrkesaktivitet.yrkesaktivitetstype)
        beregning.maksdatovurdering.subsummer(subsumsjonslogg, periode)
        loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(yrkesaktivitetSomBeregner, aktivitetslogg)
    }

    private fun loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(
        yrkesaktivitetSomBeregner: Yrkesaktivitet,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (!behandlinger.trekkerTilbakePenger()) return
        if (this.yrkesaktivitet === yrkesaktivitetSomBeregner && !person.blitt6GBegrensetSidenSist(skjæringstidspunkt)) return
        aktivitetslogg.info("En endring hos en arbeidsgiver har medført at det trekkes tilbake penger hos andre arbeidsgivere")
    }

    private fun perioderDetSkalBeregnesUtbetalingFor(): List<Vedtaksperiode> {
        // lag utbetaling for seg selv + andre overlappende perioder hos andre arbeidsgivere (som ikke er utbetalt/avsluttet allerede)
        return person
            .nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
            .filter { it.behandlinger.forventerUtbetaling(periode, skjæringstidspunkt, it.skalBehandlesISpeil()) }
    }

    private fun mursteinsperioderMedSammeSkjæringstidspunkt(): List<Vedtaksperiode> {
        // lager en liste av alle vedtaksperioder (inkludert this) som har samme skjæringstidspunkt,
        // og som overlapper med hverandre
        val skjæringstidspunkt = this.skjæringstidspunkt
        return person.mursteinsperioder(this)
            .filter { it.skjæringstidspunkt == skjæringstidspunkt }
    }

    private fun perioderSomMåHensyntasVedBeregning(): List<Vedtaksperiode> {
        // finner alle perioder som må beregnes sammen for at vi skal
        // kunne vurdere alle aktuelle vilkår.
        // unngår eldre perioder som slutter før this da de skal ha blitt beregnet før this
        // for eksempel kan listen returnere senere perioder som ikke overlapper med this i det hele tatt,
        // men som overlapper med en periode som overlapper med this
        return mursteinsperioderMedSammeSkjæringstidspunkt()
            .filterNot { it.periode.endInclusive < this.periode.start }
    }

    internal fun skalBehandlesISpeil(): Boolean {
        return when (yrkesaktivitet.yrkesaktivitetstype) {
            is Arbeidstaker -> behandlinger.arbeidsgiverperiode().skalFatteVedtak

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans -> false

            Behandlingsporing.Yrkesaktivitet.Selvstendig -> true
        }
    }

    internal fun måInnhenteInntektEllerRefusjon(): Boolean {
        when (yrkesaktivitet.yrkesaktivitetstype) {
            is Arbeidstaker -> {
                if (!skalBehandlesISpeil()) return false
                if (harInntektOgRefusjon()) return false
                return true
            }

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans,
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> return false
        }
    }

    private fun harInntektOgRefusjon(): Boolean {
        if (refusjonstidslinje.isEmpty()) return false
        return harEksisterendeInntekt() || behandlinger.harGjenbrukbarInntekt(yrkesaktivitet.organisasjonsnummer)
    }

    // Inntekt vi allerede har i vilkårsgrunnlag/inntektshistorikken på arbeidsgiver
    private fun harEksisterendeInntekt(): Boolean {
        // inntekt kreves så lenge det ikke finnes et vilkårsgrunnlag.
        // hvis det finnes et vilkårsgrunnlag så antas det at inntekten er representert der (vil vi slå ut på tilkommen inntekt-error senere hvis ikke)
        val vilkårsgrunnlag = vilkårsgrunnlag
        return vilkårsgrunnlag != null || kanAvklareInntekt()
    }

    private fun kanAvklareInntekt(): Boolean {
        val perioderMedSammeSkjæringstidspunkt = person
            .vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .filter { it.yrkesaktivitet === this.yrkesaktivitet }

        return yrkesaktivitet.kanBeregneSykepengegrunnlag(skjæringstidspunkt, perioderMedSammeSkjæringstidspunkt)
    }

    internal fun førstePeriodeSomTrengerInntektsmelding(): Vedtaksperiode? {
        val førsteMursteinsperiodeSomTrengerInntektEllerRefusjon = perioderSomMåHensyntasVedBeregning()
            .firstOrNull { it.måInnhenteInntektEllerRefusjon() }

        if (vilkårsgrunnlag != null) return førsteMursteinsperiodeSomTrengerInntektEllerRefusjon

        val førstePeriodePåSkjæringstidspunktetAnnenArbeidsgiverSomTrengerInntektEllerRefusjon = person.nåværendeVedtaksperioder { other ->
            this.yrkesaktivitet !== other.yrkesaktivitet && other.skjæringstidspunkt == skjæringstidspunkt && other.måInnhenteInntektEllerRefusjon()
        }.minOrNull()

        return førstePeriodePåSkjæringstidspunktetAnnenArbeidsgiverSomTrengerInntektEllerRefusjon ?: førsteMursteinsperiodeSomTrengerInntektEllerRefusjon
    }

    internal fun uberegnetVedtaksperiode(): UberegnetVedtaksperiode {
        val yrkesaktivitetstype = yrkesaktivitet.yrkesaktivitetstype
        val utbetalingstidslinjeBuilder = when (yrkesaktivitetstype) {
            is Arbeidstaker -> behandlinger.utbetalingstidslinjeBuilderForArbeidstaker()
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> behandlinger.utbetalingstidslinjeBuilderForSelvstendig()

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans -> error("Forventer ikke å lage utbetalingstidslinje for ${yrkesaktivitetstype::class.simpleName}")
        }
        return UberegnetVedtaksperiode(
            vedtaksperiodeId = id,
            yrkesaktivitet = yrkesaktivitetstype,
            periode = periode,
            sykdomstidslinje = sykdomstidslinje,
            utbetalingstidslinjeBuilder = utbetalingstidslinjeBuilder
        )
    }

    private fun harSammeUtbetalingSom(annenVedtaksperiode: Vedtaksperiode) = behandlinger.harSammeUtbetalingSom(annenVedtaksperiode)

    private fun filtrerUtbetalingstidslinjer(
        aktivitetslogg: IAktivitetslogg,
        uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
        sykepengegrunnlag: Inntekt,
        medlemskapstatus: Medlemskapsvurdering.Medlemskapstatus?,
        harOpptjening: Boolean,
        sekstisyvårsdagen: LocalDate,
        syttiårsdagen: LocalDate,
        dødsdato: LocalDate?,
        erUnderMinsteinntektskravTilFylte67: Boolean,
        erUnderMinsteinntektEtterFylte67: Boolean,
        historisktidslinje: Utbetalingstidslinje,
        perioderMedMinimumSykdomsgradVurdertOK: Set<Periode>
    ): List<BeregnetPeriode> {
        val maksdatofilter = MaksimumSykepengedagerfilter(
            sekstisyvårsdagen = sekstisyvårsdagen,
            syttiårsdagen = syttiårsdagen,
            dødsdato = dødsdato,
            subsumsjonslogg = subsumsjonslogg,
            aktivitetslogg = aktivitetslogg,
            arbeidsgiverRegler = person.regler,
            infotrygdtidslinje = historisktidslinje
        )
        val filtere = listOf(
            Sykdomsgradfilter(perioderMedMinimumSykdomsgradVurdertOK, subsumsjonslogg, aktivitetslogg),
            Minsteinntektfilter(
                sekstisyvårsdagen = sekstisyvårsdagen,
                erUnderMinsteinntektskravTilFylte67 = erUnderMinsteinntektskravTilFylte67,
                erUnderMinsteinntektEtterFylte67 = erUnderMinsteinntektEtterFylte67,
            ),
            Medlemskapsfilter(
                medlemskapstatus = medlemskapstatus,
            ),
            Opptjeningfilter(
                harOpptjening = harOpptjening
            ),
            maksdatofilter,
            MaksimumUtbetalingFilter(
                sykepengegrunnlagBegrenset6G = sykepengegrunnlag
            )
        )

        val beregnetTidslinjePerArbeidsgiver = filtere.fold(uberegnetTidslinjePerArbeidsgiver) { tidslinjer, filter ->
            filter.filter(tidslinjer, periode)
        }

        return beregnetTidslinjePerArbeidsgiver.flatMap {
            it.vedtaksperioder.map { vedtaksperiodeberegning ->
                BeregnetPeriode(
                    vedtaksperiodeId = vedtaksperiodeberegning.vedtaksperiodeId,
                    utbetalingstidslinje = vedtaksperiodeberegning.utbetalingstidslinje,
                    maksdatovurdering = maksdatofilter.maksdatoresultatForVedtaksperiode(vedtaksperiodeberegning.periode),
                    inntekterForBeregning = vedtaksperiodeberegning.inntekterForBeregning
                )
            }
        }
    }

    internal fun håndterOverstyringIgangsattRevurderingArbeidstaker(
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg, AvventerRevurdering)
    }

    internal fun håndterOverstyringIgangsattRevurderingSelvstendig(
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        håndterOverstyringIgangsattRevurdering(revurdering, aktivitetslogg, SelvstendigAvventerBlokkerendePeriode)
    }

    private fun håndterOverstyringIgangsattRevurdering(
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg,
        nesteTilstand: Vedtaksperiodetilstand
    ) {
        revurdering.inngåSomRevurdering(this, aktivitetslogg)
        behandlinger.sikreNyBehandling(
            yrkesaktivitet,
            revurdering.hendelse.metadata.behandlingkilde,
            person.beregnSkjæringstidspunkt(),
            yrkesaktivitet.beregnArbeidsgiverperiode()
        )
        tilstand(aktivitetslogg, nesteTilstand)
    }

    internal fun håndterOverstyringIgangsattFørstegangsvurdering(
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        revurdering.inngåSomEndring(this, aktivitetslogg)
        behandlinger.forkastUtbetaling(aktivitetslogg)
        if (måInnhenteInntektEllerRefusjon()) return tilstand(aktivitetslogg, AvventerInntektsmelding)
        tilstand(aktivitetslogg, AvventerBlokkerendePeriode)
    }

    internal fun håndterSelvstendigOverstyringIgangsattFørstegangsvurdering(
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        revurdering.inngåSomEndring(this, aktivitetslogg)
        behandlinger.forkastUtbetaling(aktivitetslogg)
        tilstand(aktivitetslogg, SelvstendigAvventerBlokkerendePeriode)
    }

    internal fun sikreRefusjonsopplysningerHvisTomt(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (!påminnelse.når(Flagg("fullRefusjon"))) return
        if (!behandlinger.refusjonstidslinje().isEmpty()) return
        val grunnlag = vilkårsgrunnlag ?: return
        val inntekt = grunnlag.inntektsgrunnlag.arbeidsgiverInntektsopplysninger.firstOrNull { it.orgnummer == yrkesaktivitet.organisasjonsnummer } ?: return
        behandlinger.håndterRefusjonstidslinje(
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = påminnelse.metadata.behandlingkilde,
            dokumentsporing = null,
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = yrkesaktivitet.beregnArbeidsgiverperiode(),
            refusjonstidslinje = Beløpstidslinje.fra(periode, inntekt.fastsattÅrsinntekt, Kilde(inntekt.faktaavklartInntekt.inntektsdata.hendelseId, Avsender.ARBEIDSGIVER, inntekt.faktaavklartInntekt.inntektsdata.tidsstempel))
        )
    }

    private fun prioritertNabolag(): List<Vedtaksperiode> {
        val (nabolagFør, nabolagEtter) = this.yrkesaktivitet.finnSammenhengendeVedtaksperioder(this)
            .partition { it.periode.endInclusive < this.periode.start }
        // Vi prioriterer refusjonsopplysninger fra perioder før oss før vi sjekker forlengelsene
        // Når vi ser på periodene før oss starter vi med den nærmeste
        return (nabolagFør.asReversed() + nabolagEtter)
    }

    internal fun videreførEksisterendeRefusjonsopplysninger(
        behandlingkilde: Behandlingkilde,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (refusjonstidslinje.isNotEmpty()) return
        val refusjonstidslinjeFraNabolaget =
            prioritertNabolag().firstOrNull { it.refusjonstidslinje.isNotEmpty() }?.let { nabo ->
                aktivitetslogg.info("Fant refusjonsopplysninger for $periode hos nabo-vedtaksperiode ${nabo.periode} (${nabo.id})")
                nabo.refusjonstidslinje
            } ?: Beløpstidslinje()
        val refusjonstidslinjeFraArbeidsgiver =
            yrkesaktivitet.refusjonstidslinje(this).takeUnless { it.isEmpty() }?.also { ubrukte ->
                val unikeKilder = ubrukte.filterIsInstance<Beløpsdag>().map { it.kilde.meldingsreferanseId }.toSet()
                aktivitetslogg.info("Fant ubrukte refusjonsopplysninger for $periode fra kildene ${unikeKilder.joinToString()}")
            } ?: Beløpstidslinje()
        val benyttetRefusjonstidslinje =
            (refusjonstidslinjeFraArbeidsgiver + refusjonstidslinjeFraNabolaget).fyll(periode)
        if (benyttetRefusjonstidslinje.isEmpty()) return
        this.behandlinger.håndterRefusjonstidslinje(
            yrkesaktivitet,
            behandlingkilde,
            dokumentsporing,
            aktivitetslogg,
            person.beregnSkjæringstidspunkt(),
            yrkesaktivitet.beregnArbeidsgiverperiode(),
            benyttetRefusjonstidslinje
        )
    }

    internal fun videreførEksisterendeOpplysninger(behandlingkilde: Behandlingkilde, aktivitetslogg: IAktivitetslogg) {
        lagreGjenbrukbarInntekt(aktivitetslogg)
        videreførEksisterendeRefusjonsopplysninger(
            behandlingkilde = behandlingkilde,
            dokumentsporing = null,
            aktivitetslogg = aktivitetslogg
        )
    }

    private fun lagreGjenbrukbarInntekt(aktivitetslogg: IAktivitetslogg) {
        if (harEksisterendeInntekt()) return // Trenger ikke lagre gjenbrukbare inntekter om vi har det vi trenger allerede
        // Ikke 100% at dette lagrer noe. F.eks.
        //  - det er en periode som aldri er vilkårsprøvd før
        //  - revurderinger med Infotryfd-vilkårsgrunnlag har ikke noe å gjenbruke
        //  - inntekten i vilkårsgrunnlaget er skatteopplysninger
        behandlinger.lagreGjenbrukbarInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            yrkesaktivitet = yrkesaktivitet,
            aktivitetslogg = aktivitetslogg
        )
    }

    internal fun ubrukteRefusjonsopplysningerEtter(ubrukteRefusjonsopplysninger: Refusjonsservitør) =
        ubrukteRefusjonsopplysninger.dessertmeny(startdatoPåSammenhengendeVedtaksperioder, periode).fraOgMed(periode.endInclusive.nesteDag)

    internal fun hensyntattUbrukteRefusjonsopplysninger(ubrukteRefusjonsopplysninger: Refusjonsservitør) =
        refusjonstidslinje + ubrukteRefusjonsopplysningerEtter(ubrukteRefusjonsopplysninger)

    internal companion object {
        // dersom "ny" slutter på en fredag, så starter ikke oppholdstelling før påfølgende mandag.
        // det kan derfor være mer enn 16 dager avstand mellom periodene, og arbeidsgiverperioden kan være den samme
        // Derfor bruker vi tallet 18 fremfor kanskje det forventende 16…
        internal const val MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD = 18L
        internal fun List<Vedtaksperiode>.egenmeldingsperioder(): List<Periode> = flatMap { it.behandlinger.egenmeldingsdager() }
        internal fun List<Vedtaksperiode>.refusjonstidslinje() =
            fold(Beløpstidslinje()) { beløpstidslinje, vedtaksperiode ->
                beløpstidslinje + vedtaksperiode.refusjonstidslinje
            }

        internal fun List<Vedtaksperiode>.startdatoerPåSammenhengendeVedtaksperioder(): Set<LocalDate> {
            val startdatoer = mutableMapOf<UUID, LocalDate>()

            this.forEach { vedtaksperiode ->
                if (vedtaksperiode.id in startdatoer) return@forEach
                val sammenhendeVedtaksperioder =
                    vedtaksperiode.yrkesaktivitet.finnSammenhengendeVedtaksperioder(vedtaksperiode)
                val startdatoPåSammenhengendeVedtaksperioder = sammenhendeVedtaksperioder.first().periode.start
                startdatoer.putAll(sammenhendeVedtaksperioder.associate { it.id to startdatoPåSammenhengendeVedtaksperioder })
            }

            return startdatoer.values.toSet()
        }

        internal fun List<Vedtaksperiode>.medSammeUtbetaling(vedtaksperiodeSomForsøkesAnnullert: Vedtaksperiode) = this.filter { it.harSammeUtbetalingSom(vedtaksperiodeSomForsøkesAnnullert) }.toSet()

        internal fun List<Vedtaksperiode>.aktiv(vedtaksperiodeId: UUID) = any { it.id == vedtaksperiodeId }

        // Fredet funksjonsnavn
        internal val OVERLAPPENDE_OG_ETTERGØLGENDE = fun(segSelv: Vedtaksperiode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode): Boolean {
                return segSelv.periode.overlapperEllerStarterFør(other.periode)
            }
        }

        internal val SAMME_ARBEIDSGIVERPERIODE = fun(yrkesaktivitet: Yrkesaktivitet, arbeidsgiverperiode: Periode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode): Boolean {
                return other.yrkesaktivitet.organisasjonsnummer == yrkesaktivitet.organisasjonsnummer && (other.behandlinger.arbeidsgiverperiode().arbeidsgiverperiode.periode?.overlapperMed(arbeidsgiverperiode) == true)
            }
        }

        private val HAR_AVVENTENDE_GODKJENNING: VedtaksperiodeFilter = {
            it.tilstand == AvventerGodkjenning || it.tilstand == AvventerGodkjenningRevurdering
        }

        private val HAR_PÅGÅENDE_UTBETALING: VedtaksperiodeFilter = { it.behandlinger.utbetales() }
        private val IKKE_FERDIG_BEHANDLET: VedtaksperiodeFilter = { !it.tilstand.erFerdigBehandlet }

        internal val MED_SKJÆRINGSTIDSPUNKT = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.skjæringstidspunkt == skjæringstidspunkt }
        }

        internal val AUU_SOM_VIL_UTBETALES: VedtaksperiodeFilter = {
            it.tilstand == AvsluttetUtenUtbetaling && it.skalBehandlesISpeil()
        }

        internal fun SPEILRELATERT(vararg perioder: Periode): VedtaksperiodeFilter {
            return fun(vedtaksperiode: Vedtaksperiode): Boolean {
                if (!vedtaksperiode.skalBehandlesISpeil()) return false // Om vedtaksperioden er en AUU skal den ikke hensyntas ved vurdering på avstand mellom perioder & vedtaksperiode
                return perioder.any { periode ->
                    // Om avstand mellom vedtaksperioden og en av periodene er mindre enn 18 dager er det speilrelatert.
                    // Når det ikke er noen periode mellom (?: 0) så er det kant-i-kant/overlapp som også er speilrelatert
                    (Periode.mellom(periode, vedtaksperiode.periode)?.count() ?: 0) < MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
                }
            }
        }

        private fun sykmeldingsperioder(vedtaksperioder: List<Vedtaksperiode>): List<Periode> {
            return vedtaksperioder.map { it.sykmeldingsperiode }
        }

        internal fun List<Vedtaksperiode>.beregnSkjæringstidspunkter(
            beregnSkjæringstidspunkt: () -> Skjæringstidspunkt,
            beregnArbeidsgiverperiode: (Periode) -> Arbeidsgiverperiodeavklaring
        ) {
            forEach { it.behandlinger.beregnSkjæringstidspunkt(beregnSkjæringstidspunkt, beregnArbeidsgiverperiode) }
        }

        internal fun List<Vedtaksperiode>.aktiveSkjæringstidspunkter(): Set<LocalDate> {
            return map { it.skjæringstidspunkt }.toSet()
        }

        internal fun Iterable<Vedtaksperiode>.nåværendeVedtaksperiode(filter: VedtaksperiodeFilter) =
            firstOrNull(filter)

        private fun Iterable<Vedtaksperiode>.førstePeriode() =
            sortedWith(compareBy({ it.skjæringstidspunkt }, { it.periode.start }, { it.periode.endInclusive })).firstOrNull()

        internal fun Iterable<Vedtaksperiode>.nestePeriodeSomSkalGjenopptas() =
            firstOrNull(HAR_PÅGÅENDE_UTBETALING) ?: filter(IKKE_FERDIG_BEHANDLET).førstePeriode()

        internal fun Iterable<Vedtaksperiode>.checkBareEnPeriodeTilGodkjenningSamtidig() {
            val perioderTilGodkjenning = this.filter(HAR_AVVENTENDE_GODKJENNING)
            check(perioderTilGodkjenning.size <= 1) {
                "Ugyldig situasjon! Flere perioder til godkjenning samtidig:\n${perioderTilGodkjenning.joinToString(separator = "\n") { "- ${it.id} - $it" }}"
            }
        }

        internal fun List<Vedtaksperiode>.venter() =
            mapNotNull { vedtaksperiode -> vedtaksperiode.vedtaksperiodeVenter }

        internal fun List<Vedtaksperiode>.validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) =
            forEach { it.validerTilstand(hendelse, aktivitetslogg) }

        internal fun gjenopprett(
            person: Person,
            yrkesaktivitet: Yrkesaktivitet,
            dto: VedtaksperiodeInnDto,
            regelverkslogg: Regelverkslogg,
            grunnlagsdata: Map<UUID, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>,
            utbetalinger: Map<UUID, Utbetaling>
        ): Vedtaksperiode {
            return Vedtaksperiode(
                person = person,
                yrkesaktivitet = yrkesaktivitet,
                id = dto.id,
                tilstand = when (dto.tilstand) {
                    VedtaksperiodetilstandDto.AVSLUTTET -> Avsluttet
                    VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING -> AvsluttetUtenUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> AvventerBlokkerendePeriode
                    VedtaksperiodetilstandDto.AVVENTER_A_ORDNINGEN -> AvventerAOrdningen
                    VedtaksperiodetilstandDto.AVVENTER_GODKJENNING -> AvventerGodkjenning
                    VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING -> AvventerGodkjenningRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_HISTORIKK -> AvventerHistorikk
                    VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING -> AvventerHistorikkRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> AvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> AvventerInntektsmelding
                    VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> AvventerRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_SIMULERING -> AvventerSimulering
                    VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING -> AvventerSimuleringRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING -> AvventerVilkårsprøving
                    VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> AvventerVilkårsprøvingRevurdering
                    VedtaksperiodetilstandDto.REVURDERING_FEILET -> RevurderingFeilet
                    VedtaksperiodetilstandDto.START -> Start
                    VedtaksperiodetilstandDto.TIL_INFOTRYGD -> TilInfotrygd
                    VedtaksperiodetilstandDto.TIL_UTBETALING -> TilUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_ANNULLERING -> AvventerAnnullering
                    VedtaksperiodetilstandDto.TIL_ANNULLERING -> TilAnnullering

                    VedtaksperiodetilstandDto.SELVSTENDIG_START -> SelvstendigStart
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK -> SelvstendigAvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE -> SelvstendigAvventerBlokkerendePeriode
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING -> SelvstendigAvventerVilkårsprøving
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK -> SelvstendigAvventerHistorikk
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING -> SelvstendigAvventerSimulering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING -> SelvstendigAvventerGodkjenning

                    VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING -> SelvstendigTilUtbetaling
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET -> SelvstendigAvsluttet
                },
                behandlinger = Behandlinger.gjenopprett(dto.behandlinger, grunnlagsdata, utbetalinger),
                opprettet = dto.opprettet,
                oppdatert = dto.oppdatert,
                regelverkslogg = regelverkslogg
            )
        }
    }

    fun overlappendeInfotrygdperioder(
        result: PersonObserver.OverlappendeInfotrygdperioder,
        perioder: List<Infotrygdperiode>
    ): PersonObserver.OverlappendeInfotrygdperioder {
        val overlappende = perioder.filter { it.overlapperMed(this.periode) }
        if (overlappende.isEmpty()) return result
        return result.copy(
            overlappendeInfotrygdperioder = result.overlappendeInfotrygdperioder.plusElement(
                PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                    yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                    vedtaksperiodeId = this.id,
                    kanForkastes = kanForkastes(),
                    vedtaksperiodeFom = this.periode.start,
                    vedtaksperiodeTom = this.periode.endInclusive,
                    vedtaksperiodetilstand = tilstand.type.name,
                    infotrygdperioder = overlappende.map {
                        when (it) {
                            is Friperiode -> PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
                                fom = it.periode.start,
                                tom = it.periode.endInclusive,
                                type = "FRIPERIODE",
                                orgnummer = null
                            )

                            is ArbeidsgiverUtbetalingsperiode -> PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
                                fom = it.periode.start,
                                tom = it.periode.endInclusive,
                                type = "ARBEIDSGIVERUTBETALING",
                                orgnummer = it.orgnr
                            )

                            is PersonUtbetalingsperiode -> PersonObserver.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
                                fom = it.periode.start,
                                tom = it.periode.endInclusive,
                                type = "PERSONUTBETALING",
                                orgnummer = it.orgnr
                            )
                        }
                    }
                )
            ))
    }

    internal fun dto(nestemann: Vedtaksperiode?) = VedtaksperiodeUtDto(
        id = id,
        tilstand = when (tilstand) {
            Avsluttet -> VedtaksperiodetilstandDto.AVSLUTTET
            AvsluttetUtenUtbetaling -> VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING
            AvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE
            AvventerAOrdningen -> VedtaksperiodetilstandDto.AVVENTER_A_ORDNINGEN
            AvventerGodkjenning -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING
            AvventerGodkjenningRevurdering -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING
            AvventerHistorikk -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK
            AvventerHistorikkRevurdering -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING
            AvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK
            AvventerInntektsmelding -> VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING
            AvventerRevurdering -> VedtaksperiodetilstandDto.AVVENTER_REVURDERING
            AvventerSimulering -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING
            AvventerSimuleringRevurdering -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING
            AvventerVilkårsprøving -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING
            AvventerVilkårsprøvingRevurdering -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING
            RevurderingFeilet -> VedtaksperiodetilstandDto.REVURDERING_FEILET
            Start -> VedtaksperiodetilstandDto.START
            TilInfotrygd -> VedtaksperiodetilstandDto.TIL_INFOTRYGD
            TilUtbetaling -> VedtaksperiodetilstandDto.TIL_UTBETALING
            AvventerAnnullering -> VedtaksperiodetilstandDto.AVVENTER_ANNULLERING
            TilAnnullering -> VedtaksperiodetilstandDto.TIL_ANNULLERING

            SelvstendigAvsluttet -> VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET
            SelvstendigAvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
            SelvstendigAvventerGodkjenning -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING
            SelvstendigAvventerHistorikk -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK
            SelvstendigAvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
            SelvstendigAvventerSimulering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING
            SelvstendigAvventerVilkårsprøving -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
            SelvstendigStart -> VedtaksperiodetilstandDto.SELVSTENDIG_START
            SelvstendigTilUtbetaling -> VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING
        },
        skjæringstidspunkt = this.skjæringstidspunkt,
        fom = this.periode.start,
        tom = this.periode.endInclusive,
        sykmeldingFom = this.sykmeldingsperiode.start,
        sykmeldingTom = this.sykmeldingsperiode.endInclusive,
        behandlinger = behandlinger.dto(),
        venteårsak = nestemann?.vedtaksperiodeVenter?.let { this.vedtaksperiodeVenter?.dto(it) },
        opprettet = opprettet,
        oppdatert = oppdatert,
        annulleringskandidater = person.finnAnnulleringskandidater(this).map { AnnulleringskandidatDto(it.id, it.yrkesaktivitet.organisasjonsnummer, it.periode.start, it.periode.endInclusive) }
    )

    private fun IAktivitetslogg.medFeilSomVarslerHvisNødvendig() =
        when (!kanForkastes()) {
            true -> FunksjonelleFeilTilVarsler(this)
            false -> this
        }
}

internal typealias VedtaksperiodeFilter = (Vedtaksperiode) -> Boolean

internal data class VedtaksperiodeView(
    val id: UUID,
    val periode: Periode,
    val tilstand: TilstandType,
    val oppdatert: LocalDateTime,
    val skjæringstidspunkt: LocalDate,
    val skjæringstidspunkter: List<LocalDate>,
    val egenmeldingsdager: List<Periode>,
    val behandlinger: BehandlingerView,
    val førsteFraværsdag: LocalDate?,
    val skalBehandlesISpeil: Boolean,
    val annulleringskandidater: Set<Vedtaksperiode>
) {
    val sykdomstidslinje = behandlinger.behandlinger.last().endringer.last().sykdomstidslinje
    val refusjonstidslinje = behandlinger.behandlinger.last().endringer.last().refusjonstidslinje
    val dagerNavOvertarAnsvar = behandlinger.behandlinger.last().endringer.last().dagerNavOvertarAnsvar
}

internal val HendelseMetadata.behandlingkilde
    get() =
        Behandlingkilde(meldingsreferanseId, innsendt, registrert, avsender)
