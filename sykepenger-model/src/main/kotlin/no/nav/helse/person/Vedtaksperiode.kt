package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.Toggle
import no.nav.helse.dto.AnnulleringskandidatDto
import no.nav.helse.dto.VedtaksperiodetilstandDto
import no.nav.helse.dto.deserialisering.VedtaksperiodeInnDto
import no.nav.helse.dto.serialisering.VedtaksperiodeUtDto
import no.nav.helse.erRettFør
import no.nav.helse.etterlevelse.Regelverkslogg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.etterlevelse.UtbetalingstidslinjeBuilder.Companion.subsumsjonsformat
import no.nav.helse.etterlevelse.`fvl § 35 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-12 ledd 1 punktum 1`
import no.nav.helse.etterlevelse.`§ 8-12 ledd 2`
import no.nav.helse.etterlevelse.`§ 8-13 ledd 1`
import no.nav.helse.etterlevelse.`§ 8-13 ledd 2`
import no.nav.helse.etterlevelse.`§ 8-28 ledd 3 bokstav a`
import no.nav.helse.etterlevelse.`§ 8-29`
import no.nav.helse.etterlevelse.`§ 8-3 ledd 1 punktum 2`
import no.nav.helse.etterlevelse.`§ 8-51 ledd 3`
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
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidsledig.somArbeidstakerOrThrow
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Arbeidstaker
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Frilans
import no.nav.helse.hendelser.Behandlingsporing.Yrkesaktivitet.Selvstendig
import no.nav.helse.hendelser.BitAvArbeidsgiverperiode
import no.nav.helse.hendelser.DagerFraInntektsmelding
import no.nav.helse.hendelser.Forsikring
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Grunnbeløpsregulering
import no.nav.helse.hendelser.Hendelse
import no.nav.helse.hendelser.HendelseMetadata
import no.nav.helse.hendelser.Hendelseskilde
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingerReplay
import no.nav.helse.hendelser.InntektsopplysningerFraLagretInnteksmelding
import no.nav.helse.hendelser.KollektivJordbruksforsikring
import no.nav.helse.hendelser.KorrigerteArbeidsgiveropplysninger
import no.nav.helse.hendelser.Medlemskapsvurdering
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrInntektsgrunnlag
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Påminnelse.Predikat.Flagg
import no.nav.helse.hendelser.Revurderingseventyr
import no.nav.helse.hendelser.Revurderingseventyr.Companion.annullering
import no.nav.helse.hendelser.Revurderingseventyr.Companion.tidligsteEventyr
import no.nav.helse.hendelser.SelvstendigForsikring
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.SkjønnsmessigFastsettelse
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
import no.nav.helse.person.Behandlinger.Behandling.Endring
import no.nav.helse.person.Behandlinger.Behandlingkilde
import no.nav.helse.person.Behandlinger.Companion.berik
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingDager
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingInntekt
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingRefusjon
import no.nav.helse.person.Dokumentsporing.Companion.overstyrTidslinje
import no.nav.helse.person.Dokumentsporing.Companion.søknad
import no.nav.helse.person.Venteårsak.Companion.fordi
import no.nav.helse.person.aktivitetslogg.Aktivitet
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspengerV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsforhold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.dagpengerV2
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForBeregning
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForOpptjeningsvurdering
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.selvstendigForsikring
import no.nav.helse.person.aktivitetslogg.Aktivitetskontekst
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.SpesifikkKontekst
import no.nav.helse.person.aktivitetslogg.Varselkode
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad som delvis overlapper`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_24
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_25
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_27
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_8
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IV_11
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_7
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_23
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_17
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_9
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
import no.nav.helse.person.inntekt.ArbeidstakerFaktaavklarteInntekter
import no.nav.helse.person.inntekt.Arbeidstakerinntektskilde
import no.nav.helse.person.inntekt.Inntektsdata
import no.nav.helse.person.inntekt.Inntektsgrunnlag
import no.nav.helse.person.inntekt.Inntektshistorikk
import no.nav.helse.person.inntekt.Inntektssituasjon
import no.nav.helse.person.inntekt.Saksbehandler
import no.nav.helse.person.inntekt.SelvstendigFaktaavklartInntekt
import no.nav.helse.person.inntekt.SelvstendigInntektsopplysning
import no.nav.helse.person.inntekt.Skatteopplysning
import no.nav.helse.person.inntekt.Skatteopplysning.Companion.subsumsjonsformat
import no.nav.helse.person.inntekt.SkatteopplysningerForSykepengegrunnlag
import no.nav.helse.person.inntekt.harAvklartArbeidstakerinntekt
import no.nav.helse.person.refusjon.Refusjonsservitør
import no.nav.helse.person.tilstandsmaskin.ArbeidsledigAvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.ArbeidsledigAvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.ArbeidsledigStart
import no.nav.helse.person.tilstandsmaskin.ArbeidstakerStart
import no.nav.helse.person.tilstandsmaskin.Avsluttet
import no.nav.helse.person.tilstandsmaskin.AvsluttetUtenUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerAnnullering
import no.nav.helse.person.tilstandsmaskin.AvventerAnnulleringTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerAvsluttetUtenUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.AvventerGodkjenning
import no.nav.helse.person.tilstandsmaskin.AvventerGodkjenningRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerHistorikk
import no.nav.helse.person.tilstandsmaskin.AvventerHistorikkRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.AvventerInntektsmelding
import no.nav.helse.person.tilstandsmaskin.AvventerInntektsopplysningerForAnnenArbeidsgiver
import no.nav.helse.person.tilstandsmaskin.AvventerRefusjonsopplysningerAnnenPeriode
import no.nav.helse.person.tilstandsmaskin.AvventerRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerRevurderingTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.AvventerSimulering
import no.nav.helse.person.tilstandsmaskin.AvventerSimuleringRevurdering
import no.nav.helse.person.tilstandsmaskin.AvventerSøknadForOverlappendePeriode
import no.nav.helse.person.tilstandsmaskin.AvventerVilkårsprøving
import no.nav.helse.person.tilstandsmaskin.AvventerVilkårsprøvingRevurdering
import no.nav.helse.person.tilstandsmaskin.FrilansAvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.FrilansAvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.FrilansStart
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvsluttet
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerBlokkerendePeriode
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerGodkjenning
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerGodkjenningRevurdering
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerHistorikk
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerHistorikkRevurdering
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerInfotrygdHistorikk
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerRevurdering
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerRevurderingTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerSimulering
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerSimuleringRevurdering
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerVilkårsprøving
import no.nav.helse.person.tilstandsmaskin.SelvstendigAvventerVilkårsprøvingRevurdering
import no.nav.helse.person.tilstandsmaskin.SelvstendigStart
import no.nav.helse.person.tilstandsmaskin.SelvstendigTilUtbetaling
import no.nav.helse.person.tilstandsmaskin.TilAnnullering
import no.nav.helse.person.tilstandsmaskin.TilInfotrygd
import no.nav.helse.person.tilstandsmaskin.TilUtbetaling
import no.nav.helse.person.tilstandsmaskin.TilstandType
import no.nav.helse.person.tilstandsmaskin.Vedtaksperiodetilstand
import no.nav.helse.person.tilstandsmaskin.nesteTilstandEtterInntekt
import no.nav.helse.person.tilstandsmaskin.starttilstander
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidsledig.somString
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverberegningBuilder
import no.nav.helse.utbetalingstidslinje.Begrunnelse.MinimumSykdomsgrad
import no.nav.helse.utbetalingstidslinje.BeregnetPeriode
import no.nav.helse.utbetalingstidslinje.Maksdatoberegning.Companion.TILSTREKKELIG_OPPHOLD_I_SYKEDAGER
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Minsteinntektsvurdering
import no.nav.helse.utbetalingstidslinje.Minsteinntektsvurdering.Companion.lagMinsteinntektsvurdering
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.AvvistDag
import no.nav.helse.utbetalingstidslinje.Utbetalingsdag.NavDag
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjesubsumsjon
import no.nav.helse.utbetalingstidslinje.filtrerUtbetalingstidslinjer
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class Vedtaksperiode private constructor(
    internal val person: Person,
    internal val yrkesaktivitet: Yrkesaktivitet,
    internal val id: UUID,
    tilstand: Vedtaksperiodetilstand,
    internal val behandlinger: Behandlinger,
    internal val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet,
    private val regelverkslogg: Regelverkslogg
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    internal constructor(
        eventBus: EventBus,
        egenmeldingsperioder: List<Periode>,
        metadata: HendelseMetadata,
        person: Person,
        yrkesaktivitet: Yrkesaktivitet,
        sykdomstidslinje: Sykdomstidslinje,
        dokumentsporing: Dokumentsporing,
        sykmeldingsperiode: Periode,
        arbeidssituasjon: Endring.Arbeidssituasjon,
        faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
        regelverkslogg: Regelverkslogg
    ) : this(
        person = person,
        yrkesaktivitet = yrkesaktivitet,
        id = UUID.randomUUID(),
        tilstand = when (yrkesaktivitet.yrkesaktivitetstype) {
            Selvstendig -> SelvstendigStart
            Arbeidsledig -> ArbeidsledigStart
            is Arbeidstaker -> ArbeidstakerStart
            Frilans -> FrilansStart
        },
        behandlinger = Behandlinger(),
        opprettet = LocalDateTime.now(),
        regelverkslogg = regelverkslogg
    ) {
        val periode = checkNotNull(sykdomstidslinje.periode()) { "sykdomstidslinjen er tom" }
        eventBus.vedtaksperiodeOpprettet(id, yrkesaktivitet.yrkesaktivitetstype, periode, periode.start, opprettet)
        behandlinger.initiellBehandling(
            behandlingEventBus = BehandlingEventBus(eventBus, yrkesaktivitet.yrkesaktivitetstype, id, emptySet()),
            sykmeldingsperiode = sykmeldingsperiode,
            sykdomstidslinje = sykdomstidslinje,
            arbeidssituasjon = arbeidssituasjon,
            egenmeldingsdager = egenmeldingsperioder,
            faktaavklartInntekt = faktaavklartInntekt,
            dokumentsporing = dokumentsporing,
            behandlingkilde = metadata.behandlingkilde
        )
    }

    internal var tilstand: Vedtaksperiodetilstand = tilstand
        private set

    internal val sykmeldingsperiode get() = behandlinger.sykmeldingsperiode()
    internal val periode get() = behandlinger.periode()
    internal val sykdomstidslinje get() = behandlinger.sykdomstidslinje()
    internal val subsumsjonslogg get() = behandlinger.subsumsjonslogg(regelverkslogg, id, person.fødselsnummer, yrkesaktivitet.organisasjonsnummer)
    internal val skjæringstidspunkt get() = behandlinger.skjæringstidspunkt()
    internal val førsteFraværsdag get() = yrkesaktivitet.finnFørsteFraværsdag(this.periode)

    // 💡Må ikke forveksles med `førsteFraværsdag` 💡
    // F.eks. januar med agp 1-10 & 16-21 så er `førsteFraværsdag` 16.januar, mens `startdatoPåSammenhengendeVedtaksperioder` er 1.januar
    private val startdatoPåSammenhengendeVedtaksperioder
        get() = yrkesaktivitet.startdatoPåSammenhengendeVedtaksperioder(this)
    internal val vilkårsgrunnlag get() = person.vilkårsgrunnlagFor(skjæringstidspunkt)
    private val eksterneIderSet get() = behandlinger.eksterneIderUUID()
    internal val refusjonstidslinje get() = behandlinger.refusjonstidslinje()

    internal val EventBus.behandlingEventBus
        get() =
            BehandlingEventBus(this, yrkesaktivitet.yrkesaktivitetstype, id, behandlinger.søknadIder())

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
        annulleringskandidater = yrkesaktivitet.finnAnnulleringskandidater(this.id)
    )

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun nyBehandling(eventBus: EventBus, hendelse: Hendelse) {
        behandlinger.nyBehandling(
            behandlingEventBus = eventBus.behandlingEventBus,
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = hendelse.metadata.behandlingkilde
        )
    }

    internal fun sørgForNyBehandlingHvisIkkeÅpen(eventBus: EventBus, hendelse: Hendelse) {
        if (behandlinger.åpenForEndring()) return
        nyBehandling(eventBus, hendelse)
    }

    internal fun sørgForNyBehandlingHvisIkkeÅpenOgOppdaterSkjæringstidspunktOgDagerUtenNavAnsvar(eventBus: EventBus, hendelse: Hendelse) {
        if (behandlinger.åpenForEndring()) return
        nyBehandling(eventBus, hendelse)
        // det kan ha skjedd ting mens perioden var avsluttet som gjør at skjæringstidspunktet / agp kanskje må oppdateres
        behandlinger.oppdaterSkjæringstidspunkt(person.skjæringstidspunkter, yrkesaktivitet.perioderUtenNavAnsvar)
    }

    internal fun håndterSykmelding(sykmelding: Sykmelding) {
        sykmelding.trimLeft(periode.endInclusive)
    }

    private fun validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        check(tilstand !in starttilstander) { "en vedtaksperiode blir stående i Start-tilstanden" }
        if (!tilstand.erFerdigBehandlet) return behandlinger.validerIkkeFerdigBehandlet(hendelse.metadata.meldingsreferanseId, aktivitetslogg)

        behandlinger.validerFerdigBehandlet(hendelse.metadata.meldingsreferanseId, aktivitetslogg)
    }

    internal fun håndterSøknadFørsteGang(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        yrkesaktiviteter: List<Yrkesaktivitet>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        check(tilstand in starttilstander) { "Kan ikke håndtere søknad i tilstand $tilstand" }
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        eventBus.emitSøknadHåndtert(søknad.metadata.meldingsreferanseId.id, id, yrkesaktivitet.yrkesaktivitetstype)
        søknad.forUng(aktivitetsloggMedVedtaksperiodekontekst, person.alder)
        yrkesaktivitet.vurderOmSøknadIkkeKanHåndteres(aktivitetsloggMedVedtaksperiodekontekst, periode, yrkesaktiviteter)

        infotrygdhistorikk.validerMedFunksjonellFeil(aktivitetsloggMedVedtaksperiodekontekst, periode)
        håndterSøknad(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)
        aktivitetsloggMedVedtaksperiodekontekst.info("Fullført behandling av søknad")

        if (aktivitetsloggMedVedtaksperiodekontekst.harFunksjonelleFeil()) forkast(eventBus, søknad, aktivitetslogg)
        return Revurderingseventyr.nyPeriode(søknad, skjæringstidspunkt, behandlinger.egenmeldingsdager().plusElement(periode).periode()!!)
    }

    internal fun håndterKorrigertSøknad(eventBus: EventBus, søknad: Søknad, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!søknad.erRelevant(this.periode)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        eventBus.emitSøknadHåndtert(søknad.metadata.meldingsreferanseId.id, id, yrkesaktivitet.yrkesaktivitetstype)

        when (tilstand) {
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            SelvstendigAvventerInfotrygdHistorikk,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerSimulering,
            AvventerVilkårsprøving -> håndterOverlappendeSøknad(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)

            AvsluttetUtenUtbetaling,
            Avsluttet,
            TilUtbetaling,
            SelvstendigAvsluttet,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerRevurderingTilUtbetaling -> {
                nyBehandling(eventBus, søknad)
                håndterOverlappendeSøknadRevurdering(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)
            }

            AvventerGodkjenningRevurdering,
            AvventerHistorikkRevurdering,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøvingRevurdering -> {
                håndterOverlappendeSøknadRevurdering(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)
            }

            ArbeidstakerStart,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            TilAnnullering,
            TilInfotrygd,
            SelvstendigStart,
            FrilansStart,
            ArbeidsledigStart -> error("Kan ikke håndtere søknad mens perioden er i $tilstand")

            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving -> håndterOverlappendeSøknad(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)

            SelvstendigAvventerRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering -> håndterOverlappendeSøknadRevurdering(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)

        }
        if (aktivitetsloggMedVedtaksperiodekontekst.harFunksjonelleFeil()) forkast(eventBus, søknad, aktivitetsloggMedVedtaksperiodekontekst)
        return Revurderingseventyr.korrigertSøknad(søknad, skjæringstidspunkt, periode)
    }

    internal fun håndterKorrigertInntekt(eventBus: EventBus, hendelse: OverstyrArbeidsgiveropplysninger, korrigertInntekt: Saksbehandler, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (skjæringstidspunkt != hendelse.skjæringstidspunkt) return null

        when (tilstand) {
            Avsluttet,
            AvsluttetUtenUtbetaling,
            TilUtbetaling -> nyBehandling(eventBus, hendelse)

            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            ArbeidstakerStart,
            FrilansAvventerBlokkerendePeriode,
            FrilansAvventerInfotrygdHistorikk,
            FrilansStart,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> {
            }
        }
        behandlinger.håndterKorrigertInntekt(
            behandlingEventBus = eventBus.behandlingEventBus,
            korrigertInntekt = korrigertInntekt,
            aktivitetslogg = registrerKontekst(aktivitetslogg)
        )
        return Revurderingseventyr.arbeidsgiveropplysninger(hendelse, skjæringstidspunkt, periode.start)// TODO: Eget eventyr?
    }

    internal fun håndterOverstyrTidslinje(eventBus: EventBus, hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!hendelse.erRelevant(this.periode)) {
            hendelse.vurdertTilOgMed(periode.endInclusive)
            return null
        }
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val overstyring = when (tilstand) {
            Avsluttet,
            AvsluttetUtenUtbetaling,
            TilUtbetaling,
            SelvstendigTilUtbetaling,
            SelvstendigAvsluttet -> håndterHistorikkNyBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst)

            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,

            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,

            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,

            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,

            SelvstendigAvventerRevurdering,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering -> håndterHistorikkÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst)

            ArbeidstakerStart,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            TilAnnullering,
            TilInfotrygd,
            FrilansStart,
            ArbeidsledigStart,
            SelvstendigStart -> error("Kan ikke overstyre tidslinjen i $tilstand")
        }

        hendelse.vurdertTilOgMed(periode.endInclusive)
        return overstyring
    }

    private fun håndterHistorikkNyBehandling(eventBus: EventBus, hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr {
        nyBehandling(eventBus, hendelse)
        return håndterHistorikkÅpenBehandling(eventBus, hendelse, aktivitetslogg)
    }

    private fun håndterHistorikkÅpenBehandling(eventBus: EventBus, hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr {
        val dagerNavOvertarAnsvar = behandlinger.dagerNavOvertarAnsvar
        oppdaterHistorikk(eventBus, overstyrTidslinje(hendelse.metadata.meldingsreferanseId), hendelse.sykdomstidslinje, aktivitetslogg, hendelse.dagerNavOvertarAnsvar(dagerNavOvertarAnsvar)) {
            // ingen validering å gjøre :(
        }
        aktivitetslogg.info("Igangsetter overstyring av tidslinje")

        return Revurderingseventyr.sykdomstidslinje(hendelse, this.skjæringstidspunkt, this.periode)
    }

    internal fun håndterAnmodningOmForkasting(eventBus: EventBus, anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!anmodningOmForkasting.erRelevant(id)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Behandler anmodning om forkasting")

        if (!anmodningOmForkasting.force) {
            when (tilstand) {
                AvventerInntektsmelding,
                AvventerBlokkerendePeriode,
                AvventerSøknadForOverlappendePeriode,
                AvventerInntektsopplysningerForAnnenArbeidsgiver,
                AvventerRefusjonsopplysningerAnnenPeriode,
                AvsluttetUtenUtbetaling,
                AvventerAvsluttetUtenUtbetaling,
                SelvstendigAvventerBlokkerendePeriode,
                ArbeidsledigAvventerInfotrygdHistorikk,
                ArbeidsledigAvventerBlokkerendePeriode,
                FrilansAvventerInfotrygdHistorikk,
                FrilansAvventerBlokkerendePeriode,
                AvventerInfotrygdHistorikk -> {
                }

                Avsluttet,
                AvventerGodkjenning,
                AvventerGodkjenningRevurdering,
                AvventerHistorikk,
                AvventerHistorikkRevurdering,
                AvventerRevurdering,
                AvventerRevurderingTilUtbetaling,
                AvventerSimulering,
                AvventerSimuleringRevurdering,
                AvventerVilkårsprøving,
                AvventerVilkårsprøvingRevurdering,
                ArbeidstakerStart,
                FrilansStart,
                ArbeidsledigStart,
                TilInfotrygd,
                AvventerAnnullering,
                AvventerAnnulleringTilUtbetaling,
                TilAnnullering,
                TilUtbetaling,
                SelvstendigAvsluttet,
                SelvstendigAvventerGodkjenning,
                SelvstendigAvventerHistorikk,
                SelvstendigAvventerInfotrygdHistorikk,
                SelvstendigAvventerSimulering,
                SelvstendigAvventerRevurdering,
                SelvstendigAvventerVilkårsprøving,
                SelvstendigStart,
                SelvstendigTilUtbetaling,
                SelvstendigAvventerGodkjenningRevurdering,
                SelvstendigAvventerHistorikkRevurdering,
                SelvstendigAvventerRevurderingTilUtbetaling,
                SelvstendigAvventerSimuleringRevurdering,
                SelvstendigAvventerVilkårsprøvingRevurdering -> {
                    aktivitetsloggMedVedtaksperiodekontekst.info("Avslår anmodning om forkasting i $tilstand")
                    return null
                }
            }
        }

        forkast(eventBus, anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst, true)
        return Revurderingseventyr.forkasting(anmodningOmForkasting, skjæringstidspunkt, periode)
    }

    internal fun håndterInntektFraInntektsmelding(eventBus: EventBus, inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg): Revurderingseventyr {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        inntektsmeldingHåndtert(eventBus, inntektsmelding)

        val inntektPåBehandling = lagreInntektsmeldingInntektPåBehandling(eventBus, inntektsmelding, aktivitetsloggMedVedtaksperiodekontekst)

        val gammeltGrunnlag = vilkårsgrunnlag ?: return inntektPåBehandling

        // Skjæringstidspunktet har vært vilkårsprøvd før (revurdering)
        val arbeidstakerFaktaavklarteInntekter = checkNotNull(yrkesaktivitet.arbeidstakerFaktaavklarteInntekter(skjæringstidspunkt)) { "La akkurat til inntekt på skjæringstidspunktet, jo" }

        val nyttGrunnlag = gammeltGrunnlag.håndterArbeidstakerFaktaavklartInntekt(
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            arbeidstakerFaktaavklartInntekt = arbeidstakerFaktaavklarteInntekter.besteInntekt().faktaavklartInntekt,
            førsteFraværsdag = arbeidstakerFaktaavklarteInntekter.førsteFraværsdag
        ) ?: return inntektPåBehandling

        person.lagreVilkårsgrunnlag(nyttGrunnlag)
        aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
        return Revurderingseventyr.korrigertInntektsmeldingInntektsopplysninger(inntektsmelding, skjæringstidspunkt, skjæringstidspunkt)
    }

    private fun lagreInntektsmeldingInntektPåBehandling(eventBus: EventBus, inntektsmelding: Inntektsmelding, aktivitetslogg: IAktivitetslogg): Revurderingseventyr {
        when (tilstand) {
            AvsluttetUtenUtbetaling,
            Avsluttet -> sørgForNyBehandlingHvisIkkeÅpenOgOppdaterSkjæringstidspunktOgDagerUtenNavAnsvar(eventBus, inntektsmelding)

            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering -> {
            }

            TilUtbetaling -> check(behandlinger.åpenForEndring()) {
                "forventer at vedtaksperioden er åpen for endring når inntekt håndteres (tilstand $tilstand)"
            }

            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            ArbeidstakerStart,
            TilAnnullering,
            TilInfotrygd -> error("Forventer ikke å håndtere inntekt i tilstand $tilstand")
        }

        behandlinger.håndterFaktaavklartInntekt(
            behandlingEventBus = eventBus.behandlingEventBus,
            arbeidstakerFaktaavklartInntekt = inntektsmelding.faktaavklartInntekt,
            aktivitetslogg = aktivitetslogg,
            dokumentsporing = inntektsmeldingInntekt(inntektsmelding.metadata.meldingsreferanseId)
        )

        return Revurderingseventyr.inntektFraInntektsmelding(inntektsmelding, periode)
    }

    internal fun håndterInntektsopplysningerFraLagretInntektsmelding(eventBus: EventBus, inntektsopplysningerFraLagretInnteksmelding: InntektsopplysningerFraLagretInnteksmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (tilstand !is AvventerInntektsmelding) return aktivitetsloggMedVedtaksperiodekontekst.info("Håndterer ikke inntektsopplysninger fra lagret inntektsmelding i tilstand ${tilstand::class.simpleName}")

        behandlinger.håndterFaktaavklartInntekt(
            behandlingEventBus = eventBus.behandlingEventBus,
            arbeidstakerFaktaavklartInntekt = inntektsopplysningerFraLagretInnteksmelding.faktaavklartInntekt(skjæringstidspunkt),
            aktivitetslogg = aktivitetsloggMedVedtaksperiodekontekst,
            dokumentsporing = inntektsmeldingInntekt(inntektsopplysningerFraLagretInnteksmelding.metadata.meldingsreferanseId)
        )

        behandlinger.håndterRefusjonstidslinje(
            behandlingEventBus = eventBus.behandlingEventBus,
            dokumentsporing = inntektsmeldingRefusjon(inntektsopplysningerFraLagretInnteksmelding.inntektsmeldingMeldingsreferanseId),
            aktivitetslogg = aktivitetsloggMedVedtaksperiodekontekst,
            benyttetRefusjonsopplysninger = inntektsopplysningerFraLagretInnteksmelding.refusjonstidslinje(periode)
        )

        aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_27)

        eventBus.emitInntektsmeldingHåndtert(
            meldingsreferanseId = inntektsopplysningerFraLagretInnteksmelding.inntektsmeldingMeldingsreferanseId.id,
            vedtaksperiodeId = id,
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            vedtaksperioderMedSammeFørsteFraværsdag = yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(this).vedtaksperioder.map { it.id }
        )
        person.gjenopptaBehandling(aktivitetslogg)
    }

    private fun inntektsmeldingHåndtert(eventBus: EventBus, inntektsmelding: Inntektsmelding) {
        inntektsmelding.inntektHåndtert()
        eventBus.emitInntektsmeldingHåndtert(
            meldingsreferanseId = inntektsmelding.metadata.meldingsreferanseId.id,
            vedtaksperiodeId = id,
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            vedtaksperioderMedSammeFørsteFraværsdag = yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(this).vedtaksperioder.map { it.id }
        )
    }

    internal fun håndterReplayAvInntektsmelding(eventBus: EventBus, vedtaksperiodeIdForReplay: UUID, inntektsmeldinger: List<Inntektsmelding>, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (vedtaksperiodeIdForReplay != this.id) return null
        if (inntektsmeldinger.isEmpty()) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        when (tilstand) {
            AvventerInntektsmelding,
            AvsluttetUtenUtbetaling -> {
                val antallInntektsmeldinger = inntektsmeldinger.size
                aktivitetsloggMedVedtaksperiodekontekst.info("Replayer inntektsmeldinger ($antallInntektsmeldinger stk) i $tilstand.")

                if (antallInntektsmeldinger > 1) aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
                return inntektsmeldinger
                    .mapNotNull { yrkesaktivitet.håndterInntektsmelding(eventBus, it, aktivitetsloggMedVedtaksperiodekontekst) }
                    .tidligsteEventyr()
            }

            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            TilAnnullering,
            TilUtbetaling -> {
                aktivitetsloggMedVedtaksperiodekontekst.info("Replayer ikke inntektsmelding fordi tilstanden er $tilstand.")
                if (inntektsmeldinger.size > 1) aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
            }

            Avsluttet,
            ArbeidstakerStart,
            TilInfotrygd -> {
                aktivitetsloggMedVedtaksperiodekontekst.info("Replayer ikke inntektsmelding fordi tilstanden er $tilstand.")
            }

            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering -> error("Kan ikke håndtere replay av inntektsmelding i en selvstendigtilstand: $tilstand")
        }
        return null
    }

    internal fun håndterInntektsmeldingerReplay(eventBus: EventBus, replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg) {
        if (!replays.erRelevant(this.id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.replayUtført(this, eventBus, replays, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun inntektsmeldingFerdigbehandlet(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.inntektsmeldingFerdigbehandlet(this, eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun håndterArbeidsgiveropplysninger(eventBus: EventBus, eventyr: List<List<Revurderingseventyr>>, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        eventBus.emitInntektsmeldingHåndtert(
            hendelse.metadata.meldingsreferanseId.id,
            id,
            yrkesaktivitet.organisasjonsnummer,
            yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(this).vedtaksperioder.map { it.id }
        )
        val tidligsteEventyr = eventyr.flatten().tidligsteEventyr()
        if (aktivitetslogg.harFunksjonelleFeil()) forkast(eventBus, hendelse, aktivitetslogg)
        return tidligsteEventyr
    }

    internal fun håndterArbeidsgiveropplysninger(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, vedtaksperioder: List<Vedtaksperiode>, inntektshistorikk: Inntektshistorikk, ubrukteRefusjonsopplysninger: Refusjonsservitør): Revurderingseventyr? {
        if (arbeidsgiveropplysninger.vedtaksperiodeId != id) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        if (tilstand !is AvventerInntektsmelding) {
            aktivitetsloggMedVedtaksperiodekontekst.info("Mottok arbeidsgiveropplysninger i ${tilstand.type}")
            return null
        }

        val eventyr = listOf(
            nullstillEgenmeldingsdagerIArbeidsgiverperiode(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst, inntektsmeldingDager(arbeidsgiveropplysninger.metadata.meldingsreferanseId)),
            håndterOppgittArbeidsgiverperiode(eventBus, arbeidsgiveropplysninger, vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst),
            håndterOppgittRefusjon(eventBus, arbeidsgiveropplysninger, vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst, ubrukteRefusjonsopplysninger),
            håndterOppgittInntekt(eventBus, arbeidsgiveropplysninger, inntektshistorikk, aktivitetsloggMedVedtaksperiodekontekst),
            håndterIkkeNyArbeidsgiverperiode(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterIkkeUtbetaltArbeidsgiverperiode(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterRedusertUtbetaltBeløpIArbeidsgiverperioden(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterUtbetaltDelerAvArbeidsgiverperioden(eventBus, arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterOpphørAvNaturalytelser(arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
        )

        return håndterArbeidsgiveropplysninger(eventBus, eventyr, arbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndterKorrigerteArbeidsgiveropplysninger(eventBus: EventBus, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, vedtaksperioder: List<Vedtaksperiode>, inntektshistorikk: Inntektshistorikk, ubrukteRefusjonsopplysninger: Refusjonsservitør): Revurderingseventyr? {
        if (korrigerteArbeidsgiveropplysninger.vedtaksperiodeId != id) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        check(tilstand !is AvventerInntektsmelding) { "Mottok Korrigerende arbeidsgiveropplysninger i AvventerInntektsmelding " }

        val eventyr = listOf(
            håndterOppgittRefusjon(eventBus, korrigerteArbeidsgiveropplysninger, vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst, ubrukteRefusjonsopplysninger),
            håndterOppgittInntekt(eventBus, korrigerteArbeidsgiveropplysninger, inntektshistorikk, aktivitetsloggMedVedtaksperiodekontekst),
            håndterKorrigertArbeidsgiverperiode(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst),
            håndterKorrigertOpphørAvNaturalytelser(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
        )

        return håndterArbeidsgiveropplysninger(eventBus, eventyr, korrigerteArbeidsgiveropplysninger, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun håndterOppgittArbeidsgiverperiode(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val oppgittArbeidgiverperiode = arbeidsgiveropplysninger.filterIsInstance<OppgittArbeidgiverperiode>().singleOrNull() ?: return emptyList()
        val eventyr = mutableListOf<Revurderingseventyr>()
        val initiell = OppgittArbeidsgiverperiodehåndtering.opprett(oppgittArbeidgiverperiode.perioder, arbeidsgiveropplysninger.metadata)

        val rester = vedtaksperioder.fold(initiell) { acc, vedtaksperiode ->
            val arbeidsgiverperiodetidslinje = acc.sykdomstidslinje(vedtaksperiode.periode)
            if (arbeidsgiverperiodetidslinje != null) {
                eventyr.add(vedtaksperiode.håndterBitAvArbeidsgiverperiode(eventBus, arbeidsgiveropplysninger, aktivitetslogg, arbeidsgiverperiodetidslinje))
            }
            acc.håndterVedtaksperiode(vedtaksperiode.periode)
        }

        val antallDagerIgjen = rester.sykdomstidslinje.count()
        if (antallDagerIgjen > 0) {
            aktivitetslogg.info("Det er rester igjen etter håndtering av dager ($antallDagerIgjen dager)")
        }

        return eventyr
    }

    private fun håndterBitAvArbeidsgiverperiode(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, arbeidsgiverperiodetidslinje: Sykdomstidslinje): Revurderingseventyr {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val bitAvArbeidsgiverperiode = BitAvArbeidsgiverperiode(arbeidsgiveropplysninger.metadata, arbeidsgiverperiodetidslinje, emptyList())
        when (tilstand) {
            AvsluttetUtenUtbetaling -> {
                sørgForNyBehandlingHvisIkkeÅpen(eventBus, arbeidsgiveropplysninger)
                håndterDager(eventBus, arbeidsgiveropplysninger, bitAvArbeidsgiverperiode, aktivitetsloggMedVedtaksperiodekontekst) {}
            }

            AvventerInntektsmelding,
            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode -> {
                håndterDager(eventBus, arbeidsgiveropplysninger, bitAvArbeidsgiverperiode, aktivitetsloggMedVedtaksperiodekontekst) {}
            }

            TilUtbetaling,
            Avsluttet -> {
                nyBehandling(eventBus, arbeidsgiveropplysninger)
                // det er oppgitt arbeidsgiverperiode på uventede perioder; mest sannsynlig
                // har da ikke vedtaksperioden bedt om Arbeidsgiverperiode som opplysning, men vi har fått det likevel
                aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_24)
                aktivitetsloggMedVedtaksperiodekontekst.info("Håndterer ikke arbeidsgiverperiode i ${tilstand.type}")
            }

            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering -> {
                // det er oppgitt arbeidsgiverperiode på uventede perioder; mest sannsynlig
                // har da ikke vedtaksperioden bedt om Arbeidsgiverperiode som opplysning, men vi har fått det likevel
                aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_24)
                aktivitetsloggMedVedtaksperiodekontekst.info("Håndterer ikke arbeidsgiverperiode i ${tilstand.type}")
            }

            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,

            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            ArbeidstakerStart,
            TilAnnullering,
            TilInfotrygd -> error("forventer ikke å håndtere arbeidsgiverperiode i tilstand $tilstand")
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

    private fun <T> håndterOppgittRefusjon(eventBus: EventBus, hendelse: T, vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg, ubrukteRefusjonsopplysninger: Refusjonsservitør): List<Revurderingseventyr> where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
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
            vedtaksperiode.håndterRefusjon(
                eventBus = eventBus,
                hendelse = hendelse,
                dokumentsporing = inntektsmeldingRefusjon(hendelse.metadata.meldingsreferanseId),
                aktivitetslogg = vedtaksperiode.registrerKontekst(aktivitetslogg),
                servitør = servitør
            )
        }
        servitør.servér(ubrukteRefusjonsopplysninger, aktivitetslogg)
        return eventyr
    }

    private fun <T> håndterOppgittInntekt(eventBus: EventBus, hendelse: T, inntektshistorikk: Inntektshistorikk, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
        val oppgittInntekt = hendelse.filterIsInstance<OppgittInntekt>().singleOrNull() ?: return emptyList()

        val inntektsdata = Inntektsdata(
            hendelseId = hendelse.metadata.meldingsreferanseId,
            dato = skjæringstidspunkt, // Her skulle du kanskje tro at det riktige var å lagre på første fraværsdag, MEN siden dette er arbeidsgiveropplysninger fra HAG har de hensyntatt at man er syk i annen måned enn skjæringstidspunktet, så vi skal bare sluke det de opplyser om og lagre på skjæringstidspunktet.
            beløp = oppgittInntekt.inntekt,
            tidsstempel = LocalDateTime.now()
        )

        val arbeidstakerFaktaavklartInntekt = ArbeidstakerFaktaavklartInntekt(id = UUID.randomUUID(), inntektsdata = inntektsdata, inntektsopplysningskilde = Arbeidstakerinntektskilde.Arbeidsgiver)
        inntektshistorikk.leggTil(arbeidstakerFaktaavklartInntekt)

        when (tilstand) {
            AvsluttetUtenUtbetaling,
            Avsluttet,
            TilUtbetaling -> sørgForNyBehandlingHvisIkkeÅpen(eventBus, hendelse)

            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering -> {
            }

            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,

            FrilansStart,
            FrilansAvventerBlokkerendePeriode,
            FrilansAvventerInfotrygdHistorikk,

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            ArbeidstakerStart,
            TilAnnullering,
            TilInfotrygd -> error("Forventer ikke å håndtere inntekt i tilstand $tilstand")
        }

        if (hendelse is KorrigerteArbeidsgiveropplysninger) aktivitetslogg.varsel(RV_IM_4)

        behandlinger.håndterFaktaavklartInntekt(
            behandlingEventBus = eventBus.behandlingEventBus,
            arbeidstakerFaktaavklartInntekt = arbeidstakerFaktaavklartInntekt,
            aktivitetslogg = aktivitetslogg,
            dokumentsporing = inntektsmeldingInntekt(hendelse.metadata.meldingsreferanseId)
        )

        val grunnlag = vilkårsgrunnlag ?: return listOf(Revurderingseventyr.inntekt(hendelse, skjæringstidspunkt))

        // Skjæringstidspunktet har vært vilkårsprøvd før (revurdering)
        val arbeidstakerFaktaavklarteInntekter = checkNotNull(yrkesaktivitet.arbeidstakerFaktaavklarteInntekter(skjæringstidspunkt)) { "La akkurat til inntekt på skjæringstidspunktet, jo" }

        val nyttGrunnlag = grunnlag.håndterArbeidstakerFaktaavklartInntekt(
            organisasjonsnummer = yrkesaktivitet.organisasjonsnummer,
            arbeidstakerFaktaavklartInntekt = arbeidstakerFaktaavklarteInntekter.besteInntekt().faktaavklartInntekt,
            førsteFraværsdag = arbeidstakerFaktaavklarteInntekter.førsteFraværsdag
        ) ?: return emptyList()

        person.lagreVilkårsgrunnlag(nyttGrunnlag)
        return listOf(Revurderingseventyr.inntekt(hendelse, skjæringstidspunkt))
    }

    private fun håndterIkkeNyArbeidsgiverperiode(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        if (arbeidsgiveropplysninger.filterIsInstance<IkkeNyArbeidsgiverperiode>().isEmpty()) return emptyList()
        aktivitetslogg.info("Arbeidsgiver mener at det ikke er noen ny arbeidsgiverperiode")
        aktivitetslogg.varsel(RV_IM_25)
        return emptyList()
    }

    private fun håndterIkkeUtbetaltArbeidsgiverperiode(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val ikkeUbetaltArbeidsgiverperiode = arbeidsgiveropplysninger.filterIsInstance<IkkeUtbetaltArbeidsgiverperiode>().singleOrNull() ?: return emptyList()
        return håndterNavUtbetalerArbeidsgiverperiode(eventBus, aktivitetslogg, arbeidsgiveropplysninger) {
            ikkeUbetaltArbeidsgiverperiode.valider(aktivitetslogg)
        }
    }

    private fun håndterRedusertUtbetaltBeløpIArbeidsgiverperioden(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val redusertUtbetaltBeløpIArbeidsgiverperioden = arbeidsgiveropplysninger.filterIsInstance<RedusertUtbetaltBeløpIArbeidsgiverperioden>().singleOrNull() ?: return emptyList()
        return håndterNavUtbetalerArbeidsgiverperiode(eventBus, aktivitetslogg, arbeidsgiveropplysninger) {
            redusertUtbetaltBeløpIArbeidsgiverperioden.valider(aktivitetslogg)
        }
    }

    private fun håndterUtbetaltDelerAvArbeidsgiverperioden(eventBus: EventBus, arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        val utbetaltDelerAvArbeidsgiverperioden = arbeidsgiveropplysninger.filterIsInstance<UtbetaltDelerAvArbeidsgiverperioden>().singleOrNull() ?: return emptyList()
        val perioderNavUtbetaler = behandlinger.ventedager().dagerUtenNavAnsvar.dager.flatMap { it.uten(LocalDate.MIN til utbetaltDelerAvArbeidsgiverperioden.utbetaltTilOgMed) }
        return håndterNavUtbetalerArbeidsgiverperiode(eventBus, aktivitetslogg, arbeidsgiveropplysninger, perioderNavUtbetaler = perioderNavUtbetaler) {
            utbetaltDelerAvArbeidsgiverperioden.valider(aktivitetslogg)
        }
    }

    private fun håndterNavUtbetalerArbeidsgiverperiode(
        eventBus: EventBus,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgiveropplysninger: Arbeidsgiveropplysninger,
        perioderNavUtbetaler: List<Periode> = behandlinger.ventedager().dagerUtenNavAnsvar.dager,
        valider: () -> Unit
    ): List<Revurderingseventyr> {
        val bit = sykNavBit(arbeidsgiveropplysninger, perioderNavUtbetaler)
        if (bit == null) valider()
        else håndterDager(eventBus, arbeidsgiveropplysninger, bit, aktivitetslogg, valider)
        return listOf(Revurderingseventyr.arbeidsgiverperiode(arbeidsgiveropplysninger, skjæringstidspunkt, this.periode))
    }

    private fun håndterOpphørAvNaturalytelser(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        if (arbeidsgiveropplysninger.filterIsInstance<Arbeidsgiveropplysning.OpphørAvNaturalytelser>().isEmpty()) return emptyList()
        aktivitetslogg.funksjonellFeil(RV_IM_7)
        return emptyList()
    }

    private fun håndterKorrigertOpphørAvNaturalytelser(eventBus: EventBus, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        if (korrigerteArbeidsgiveropplysninger.filterIsInstance<Arbeidsgiveropplysning.OpphørAvNaturalytelser>().isEmpty()) return emptyList()
        sørgForNyBehandlingHvisIkkeÅpen(eventBus, korrigerteArbeidsgiveropplysninger)
        aktivitetslogg.varsel(RV_IM_7)
        return listOf(Revurderingseventyr.arbeidsgiverperiode(korrigerteArbeidsgiveropplysninger, skjæringstidspunkt, periode))
    }

    private fun håndterKorrigertArbeidsgiverperiode(eventBus: EventBus, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): List<Revurderingseventyr> {
        varselVedEndretArbeidsgiverperiode(eventBus, korrigerteArbeidsgiveropplysninger, aktivitetslogg)

        val korrigertUtbetalingIArbeidsgiverperiode =
            (korrigerteArbeidsgiveropplysninger.filterIsInstance<RedusertUtbetaltBeløpIArbeidsgiverperioden>() +
                korrigerteArbeidsgiveropplysninger.filterIsInstance<UtbetaltDelerAvArbeidsgiverperioden>() +
                korrigerteArbeidsgiveropplysninger.filterIsInstance<IkkeUtbetaltArbeidsgiverperiode>() +
                korrigerteArbeidsgiveropplysninger.filterIsInstance<IkkeNyArbeidsgiverperiode>())
                .singleOrNull()

        if (korrigertUtbetalingIArbeidsgiverperiode != null) {
            sørgForNyBehandlingHvisIkkeÅpen(eventBus, korrigerteArbeidsgiveropplysninger)
            aktivitetslogg.varsel(RV_IM_8)
        }

        return listOf(Revurderingseventyr.arbeidsgiverperiode(korrigerteArbeidsgiveropplysninger, skjæringstidspunkt, periode))
    }

    private fun varselVedEndretArbeidsgiverperiode(eventBus: EventBus, korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg) {
        val oppgittArbeidgiverperiode = korrigerteArbeidsgiveropplysninger.filterIsInstance<OppgittArbeidgiverperiode>().singleOrNull() ?: return
        val beregnetArbeidsgiverperiode = behandlinger.ventedager().dagerUtenNavAnsvar.periode
        if (beregnetArbeidsgiverperiode == null) {
            sørgForNyBehandlingHvisIkkeÅpen(eventBus, korrigerteArbeidsgiveropplysninger)
            return aktivitetslogg.varsel(RV_IM_24)
        }
        if (oppgittArbeidgiverperiode.perioder.periode()!! in beregnetArbeidsgiverperiode) return
        sørgForNyBehandlingHvisIkkeÅpen(eventBus, korrigerteArbeidsgiveropplysninger)
        aktivitetslogg.varsel(RV_IM_24)
    }

    private fun sykNavBit(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, perioderNavUtbetaler: List<Periode>): BitAvArbeidsgiverperiode? {
        val dagerNavOvertarAnsvar = perioderNavUtbetaler
            .filter { it.overlapperMed(this.periode) }
            .map { it.subset(this.periode) }

        if (dagerNavOvertarAnsvar.isEmpty()) return null
        return BitAvArbeidsgiverperiode(arbeidsgiveropplysninger.metadata, Sykdomstidslinje(), dagerNavOvertarAnsvar)
    }

    internal fun håndterDagerFraInntektsmelding(eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!skalHåndtereDagerFraInntektsmelding(dager) || dager.alleredeHåndtert(behandlinger))
            return dager.vurdertTilOgMed(periode.endInclusive)

        when (tilstand) {
            Avsluttet -> {
                sørgForNyBehandlingHvisIkkeÅpen(eventBus, dager.hendelse)
                håndterKorrigerendeInntektsmelding(eventBus, dager, FunksjonelleFeilTilVarsler(aktivitetsloggMedVedtaksperiodekontekst))
            }

            AvsluttetUtenUtbetaling -> {
                sørgForNyBehandlingHvisIkkeÅpen(eventBus, dager.hendelse)
                if (this.kanForkastes()) håndterDagerFørstegang(eventBus, dager, aktivitetsloggMedVedtaksperiodekontekst)
                else håndterDagerFørstegang(eventBus, dager, FunksjonelleFeilTilVarsler(aktivitetsloggMedVedtaksperiodekontekst))
            }

            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerBlokkerendePeriode -> håndterKorrigerendeInntektsmelding(eventBus, dager, aktivitetsloggMedVedtaksperiodekontekst)

            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerAvsluttetUtenUtbetaling -> {
                if (this.kanForkastes()) håndterDagerFørstegang(eventBus, dager, aktivitetsloggMedVedtaksperiodekontekst)
                else håndterDagerFørstegang(eventBus, dager, FunksjonelleFeilTilVarsler(aktivitetsloggMedVedtaksperiodekontekst))
            }

            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering -> håndterKorrigerendeInntektsmelding(eventBus, dager, aktivitetsloggMedVedtaksperiodekontekst)

            TilUtbetaling,
            ArbeidsledigStart,
            ArbeidstakerStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            TilAnnullering,
            TilInfotrygd -> error("Forventer ikke å håndtere inntektsmelding i $tilstand")
        }


        dager.vurdertTilOgMed(periode.endInclusive)
    }

    private fun håndterDagerFørstegang(eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        håndterDager(eventBus, dager, aktivitetslogg)

        if (aktivitetslogg.harFunksjonelleFeil() && kanForkastes()) {
            forkast(eventBus, dager.hendelse, aktivitetslogg)
        }
    }

    private fun skalHåndtereDagerFraInntektsmelding(dager: DagerFraInntektsmelding): Boolean {
        val sammenhengende = yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(this).periode

        return when (tilstand) {
            Avsluttet,
            AvventerGodkjenningRevurdering,
            AvventerHistorikkRevurdering,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøvingRevurdering -> dager.skalHåndteresAvRevurdering(periode, sammenhengende, behandlinger.ventedager().dagerUtenNavAnsvar.dager)

            AvventerInntektsmelding -> dager.skalHåndteresAv(sammenhengende)

            AvsluttetUtenUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerInfotrygdHistorikk,
            AvventerSimulering,
            AvventerVilkårsprøving -> dager.skalHåndteresAv(periode)

            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidstakerStart,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> false
        }
    }

    internal fun håndterDager(eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val periodeÅVurdereDagerUtenNavAnsvar = when (behandlinger.dagerNavOvertarAnsvar.isEmpty()) {
            true -> periode
            false -> null
        }

        val bit = dager.bitAvInntektsmelding(
            aktivitetslogg = aktivitetslogg,
            vedtaksperiode = periode,
            periodeÅVurdereDagerUtenNavAnsvar = periodeÅVurdereDagerUtenNavAnsvar
        )

        håndterDager(eventBus, dager.hendelse, bit, aktivitetslogg) {
            dager.valider(aktivitetslogg, vedtaksperiodeId = id)
            dager.validerArbeidsgiverperiode(aktivitetslogg, periode, behandlinger.ventedager().dagerUtenNavAnsvar.dager)
        }
    }

    private fun håndterDagerUtenEndring(eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val hendelse = dager.tomBitAvInntektsmelding(aktivitetslogg, periode)
        håndterDager(eventBus, dager.hendelse, hendelse, aktivitetslogg) {
            dager.valider(aktivitetslogg, behandlinger.ventedager().dagerUtenNavAnsvar.dager, vedtaksperiodeId = id)
        }
    }

    private fun håndterDager(
        eventBus: EventBus,
        hendelse: Hendelse,
        bit: BitAvArbeidsgiverperiode,
        aktivitetslogg: IAktivitetslogg,
        validering: () -> Unit
    ) {
        oppdaterHistorikk(
            eventBus = eventBus,
            dokumentsporing = inntektsmeldingDager(hendelse.metadata.meldingsreferanseId),
            hendelseSykdomstidslinje = bit.sykdomstidslinje,
            aktivitetslogg = aktivitetslogg,
            dagerNavOvertarAnsvar = bit.dagerNavOvertarAnsvar,
            validering = validering
        )
    }

    internal fun håndterHistorikkFraInfotrygd(
        eventBus: EventBus,
        hendelse: Utbetalingshistorikk,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (hendelse.vedtaksperiodeId != this.id) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        when (tilstand) {
            AvventerInfotrygdHistorikk -> tilstand(eventBus, aktivitetsloggMedVedtaksperiodekontekst, AvventerInntektsmelding)
            ArbeidsledigAvventerInfotrygdHistorikk -> tilstand(eventBus, aktivitetsloggMedVedtaksperiodekontekst, ArbeidsledigAvventerBlokkerendePeriode)
            FrilansAvventerInfotrygdHistorikk -> tilstand(eventBus, aktivitetsloggMedVedtaksperiodekontekst, FrilansAvventerBlokkerendePeriode)
            SelvstendigAvventerInfotrygdHistorikk -> tilstand(eventBus, aktivitetsloggMedVedtaksperiodekontekst, SelvstendigAvventerBlokkerendePeriode)

            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerInntektsmelding,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikkRevurdering,
            AvventerHistorikk,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            ArbeidstakerStart,
            FrilansStart,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerBlokkerendePeriode,
            TilUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            TilAnnullering,
            TilInfotrygd,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering -> {
                /* gjør ingenting */
            }
        }
    }

    internal fun håndter(
        eventBus: EventBus,
        ytelser: Ytelser,
        aktivitetslogg: IAktivitetslogg,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        if (!ytelser.erRelevant(id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val (nesteSimuleringtilstand, nesteGodkjenningtilstand) = when (tilstand) {
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            ArbeidstakerStart,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke ytelsehistorikk i %s".format(tilstand.type))

            AvventerHistorikk -> (AvventerSimulering to AvventerGodkjenning)
            AvventerHistorikkRevurdering -> (AvventerSimuleringRevurdering to AvventerGodkjenningRevurdering)
            SelvstendigAvventerHistorikk -> (SelvstendigAvventerSimulering to SelvstendigAvventerGodkjenning)
            SelvstendigAvventerHistorikkRevurdering -> (SelvstendigAvventerSimuleringRevurdering to SelvstendigAvventerGodkjenningRevurdering)
        }

        håndterYtelser(eventBus, ytelser, aktivitetsloggMedVedtaksperiodekontekst.medFeilSomVarslerHvisNødvendig(), infotrygdhistorikk, nesteSimuleringtilstand, nesteGodkjenningtilstand)
    }

    private fun harOpptjening(grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement): Boolean {
        return (grunnlagsdata as? VilkårsgrunnlagHistorikk.Grunnlagsdata)?.opptjening?.harTilstrekkeligAntallOpptjeningsdager() ?: true
    }

    private fun kanskjeKollektivForsikring(): Forsikring? = when (behandlinger.arbeidssituasjon) {
        Endring.Arbeidssituasjon.JORDBRUKER -> KollektivJordbruksforsikring
        Endring.Arbeidssituasjon.ARBEIDSTAKER,
        Endring.Arbeidssituasjon.ARBEIDSLEDIG,
        Endring.Arbeidssituasjon.FRILANSER,
        Endring.Arbeidssituasjon.SELVSTENDIG_NÆRINGSDRIVENDE,
        Endring.Arbeidssituasjon.BARNEPASSER,
        Endring.Arbeidssituasjon.FISKER,
        Endring.Arbeidssituasjon.ANNET -> null
    }

    private fun håndterYtelser(eventBus: EventBus, ytelser: Ytelser, aktivitetslogg: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk, nesteSimuleringtilstand: Vedtaksperiodetilstand, nesteGodkjenningtilstand: Vedtaksperiodetilstand) {
        val grunnlagsdata = checkNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for ${skjæringstidspunkt}, men har ikke. Lages det utbetaling for en periode som ikke skal lage utbetaling?"
        }
        // steg 1: sett sammen alle inntekter som skal brukes i beregning
        // steg 2: lag utbetalingstidslinjer for alle vedtaksperiodene
        val (beregningsperiode, perioderSomMåHensyntasVedBeregning) = perioderSomMåHensyntasVedBeregning()
        val inntektsperioder = ytelser.inntektsendringer()
        val forsikring = ytelser.selvstendigForsikring() ?: kanskjeKollektivForsikring()
        val uberegnetTidslinjePerArbeidsgiver = lagArbeidsgiverberegning(
            beregningsperiode = beregningsperiode,
            vedtaksperioder = perioderSomMåHensyntasVedBeregning,
            vilkårsgrunnlag = grunnlagsdata,
            inntektsperioder = inntektsperioder,
            forsikring = forsikring
        )
        // steg 3: beregn alle utbetalingstidslinjer (avslå dager, beregne maksdato og utbetalingsbeløp)
        val harOpptjening = harOpptjening(grunnlagsdata)
        val sykepengegrunnlag = grunnlagsdata.inntektsgrunnlag.sykepengegrunnlag
        val beregningsgrunnlag = grunnlagsdata.inntektsgrunnlag.beregningsgrunnlag
        val medlemskapstatus = (grunnlagsdata as? VilkårsgrunnlagHistorikk.Grunnlagsdata)?.medlemskapstatus
        val minsteinntektsvurdering = lagMinsteinntektsvurdering(skjæringstidspunkt, sykepengegrunnlag)
        // grunnlaget for maksdatoberegning er alt som har skjedd før
        // - alle vedtaksperioder som slutter før vedtaksperioden som beregnes
        // - pluss alle som overlapper: Dette er bare for å få med "mursteinssnutene" (f.eks 10.-31.jan på A1 beregener, og 1.-31.jan på A2, så må vi vå med oss 1-9.jan)
        // ettersom vi etterpå kun beholder det som gjelder frem til dagen før vedtaksperioden som beregnes
        // .. også klaskes alt fra Infotrygd inn også da
        val historisktidslinje = person.vedtaksperioder { it.periode.endInclusive < periode.start || it.periode.overlapperMed(periode) }
            .map { it.behandlinger.utbetalingstidslinje() }
            .fold(person.infotrygdhistorikk.utbetalingstidslinje(), Utbetalingstidslinje::plus)
            .fremTilOgMed(periode.start.forrigeDag)

        val andreYtelserTidslinje = ytelser.andreYtelser()
        val beregnetTidslinjePerVedtaksperiode = filtrerUtbetalingstidslinjer(
            uberegnetTidslinjePerArbeidsgiver = uberegnetTidslinjePerArbeidsgiver,
            harOpptjening = harOpptjening,
            sykepengegrunnlagBegrenset6G = sykepengegrunnlag,
            erMedlemAvFolketrygden = medlemskapstatus != Medlemskapsvurdering.Medlemskapstatus.Nei,
            sekstisyvårsdagen = person.alder.redusertYtelseAlder,
            syttiårsdagen = person.alder.syttiårsdagen,
            dødsdato = person.alder.dødsdato,
            erUnderMinsteinntektskravTilFylte67 = minsteinntektsvurdering.erSykepengegrunnlagetUnderHalvG,
            erUnderMinsteinntektEtterFylte67 = minsteinntektsvurdering.erSykepengegrunnlagetUnder2G,
            historisktidslinje = historisktidslinje,
            perioderMedMinimumSykdomsgradVurdertOK = person.minimumSykdomsgradsvurdering.perioder,
            regler = person.regler,
            andreYtelser = { dato -> andreYtelserTidslinje[dato] ?: 0.prosent }
        )
        // steg 4.1: lag beregnede behandlinger
        val perioderDetSkalBeregnesUtbetalingFor = perioderDetSkalBeregnesUtbetalingFor()
        lagBeregnetBehandlinger(perioderDetSkalBeregnesUtbetalingFor, grunnlagsdata, beregnetTidslinjePerVedtaksperiode, inntektsperioder, forsikring)

        // gir beskjed om hvilken grunnlagsdata som ble brukt i beregningen for behandlingen
        eventBus.benyttetGrunnlagsdataForBeregning(behandlinger.sisteBehandlingId, id, yrkesaktivitet.yrkesaktivitetstype, periode, behandlinger.sisteBehandlingOpprettTidspunkt, forsikring)

        /* steg 4.2 lag utbetalinger */
        perioderDetSkalBeregnesUtbetalingFor.forEach { other ->
            other.lagUtbetaling(eventBus, other.registrerKontekst(aktivitetslogg))
        }

        // steg 5: lage varsler ved gitte situasjoner
        vurderVarsler(aktivitetslogg, ytelser, infotrygdhistorikk, perioderDetSkalBeregnesUtbetalingFor, grunnlagsdata, minsteinntektsvurdering, harOpptjening, beregnetTidslinjePerVedtaksperiode, forsikring)
        // steg 6: subsummere ting
        subsummering(beregningsgrunnlag, minsteinntektsvurdering, uberegnetTidslinjePerArbeidsgiver, beregnetTidslinjePerVedtaksperiode, historisktidslinje)

        if (aktivitetslogg.harFunksjonelleFeil()) return forkast(eventBus, ytelser, aktivitetslogg)

        when {
            behandlinger.harUtbetalinger() -> tilstand(eventBus, aktivitetslogg, nesteSimuleringtilstand) { aktivitetslogg.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""") }
            else -> tilstand(eventBus, aktivitetslogg, nesteGodkjenningtilstand) { aktivitetslogg.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""") }
        }
    }

    private fun lagBeregnetBehandlinger(
        perioderDetSkalBeregnesUtbetalingFor: List<Vedtaksperiode>,
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        beregnetTidslinjePerVedtaksperiode: List<BeregnetPeriode>,
        inntektsperioder: Map<Arbeidsgiverberegning.Inntektskilde, Beløpstidslinje>,
        forsikring: Forsikring?
    ): List<BeregnetBehandling> {
        if (perioderDetSkalBeregnesUtbetalingFor.isEmpty()) return emptyList()

        check(perioderDetSkalBeregnesUtbetalingFor.all { it.skjæringstidspunkt == this.skjæringstidspunkt }) {
            "ugyldig situasjon: skal beregne utbetaling for vedtaksperioder med ulike skjæringstidspunkter"
        }
        val alleInntektjusteringer = inntektsperioder
            .mapKeys { (inntektskilde, _) ->
                Inntektskilde(inntektskilde.somString)
            }

        return perioderDetSkalBeregnesUtbetalingFor
            .map { other ->
                val beregningsutfall = beregnetTidslinjePerVedtaksperiode.single { it.vedtaksperiodeId == other.id }
                other.lagBeregnetBehandling(
                    beregning = beregningsutfall,
                    grunnlagsdata = grunnlagsdata,
                    alleInntektjusteringer = alleInntektjusteringer
                        .mapValues { (_, inntektjustering) -> inntektjustering.subset(periode).medBeløp() }
                        .filterValues { it.isNotEmpty() },
                    forsikring = forsikring
                )
            }
    }

    private fun lagUtbetaling(eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        val utbetaling = behandlinger.lagUtbetaling(
            aktivitetslogg = aktivitetslogg,
            utbetalinger = yrkesaktivitet.utbetalinger,
            mottakerRefusjon = yrkesaktivitet.organisasjonsnummer,
            mottakerBruker = person.fødselsnummer
        )
        leggTilNyUtbetaling(eventBus, aktivitetslogg, utbetaling)
        eventBus.nyVedtaksperiodeUtbetaling(yrkesaktivitet.yrkesaktivitetstype, utbetaling.id, this.id)
    }

    internal fun leggTilNyUtbetaling(eventBus: EventBus, aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling) {
        yrkesaktivitet.leggTilNyUtbetaling(utbetaling)
        utbetaling.opprett(eventBus.behandlingEventBus, aktivitetslogg)
    }

    private fun vurderVarsler(
        aktivitetslogg: IAktivitetslogg,
        ytelser: Ytelser,
        infotrygdhistorikk: Infotrygdhistorikk,
        perioderDetSkalBeregnesUtbetalingFor: List<Vedtaksperiode>,
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        minsteinntektsvurdering: Minsteinntektsvurdering,
        harOpptjening: Boolean,
        beregnetTidslinjePerVedtaksperiode: List<BeregnetPeriode>,
        forsikring: Forsikring?
    ) {
        perioderDetSkalBeregnesUtbetalingFor
            .filter {
                val forrigeUtbetalingstidslinje = it.behandlinger.utbetalingstidslinjeFraForrigeVedtak() ?: Utbetalingstidslinje()
                it.behandlinger.utbetalingstidslinje().negativEndringIBeløp(forrigeUtbetalingstidslinje)
            }
            .onEach {
                it.registrerKontekst(aktivitetslogg).varsel(RV_UT_23)
            }

        if (beregnetTidslinjePerVedtaksperiode.any { it.utbetalingstidslinje.any { dag -> dag.dato in periode && dag is NavDag && dag.økonomi.totalSykdomsgrad.erUnderGrensen() } })
            aktivitetslogg.varsel(RV_VV_17)
        if (beregnetTidslinjePerVedtaksperiode.any { it.utbetalingstidslinje.any { dag -> dag.dato in periode && dag is AvvistDag && MinimumSykdomsgrad in dag.begrunnelser } })
            aktivitetslogg.varsel(RV_VV_4)
        else
            aktivitetslogg.info("Ingen avviste dager på grunn av 20 % samlet sykdomsgrad-regel for denne perioden")

        if (minsteinntektsvurdering.erUnderMinsteinntektskrav(person.alder.redusertYtelseAlder, periode))
            aktivitetslogg.varsel(RV_SV_1)
        else
            aktivitetslogg.info("Krav til minste sykepengegrunnlag er oppfylt")

        if (person.alder.dødsdato != null && person.alder.dødsdato in periode) {
            aktivitetslogg.info("Utbetaling stoppet etter ${person.alder.dødsdato} grunnet dødsfall")
        }

        if (behandlinger.maksdato.fremdelesSykEtterTilstrekkeligOpphold)
            aktivitetslogg.funksjonellFeil(RV_VV_9)
        if (behandlinger.maksdato.avslåtteDager.any { periode.overlapperMed(it) })
            aktivitetslogg.info("Maks antall sykepengedager er nådd i perioden")
        else
            aktivitetslogg.info("Maksimalt antall sykedager overskrides ikke i perioden")

        when (yrkesaktivitet.yrkesaktivitetstype) {
            is Arbeidstaker -> grunnlagsdata.valider(aktivitetslogg, yrkesaktivitet.organisasjonsnummer)
            Arbeidsledig,
            Frilans -> {
            }

            Selvstendig ->
                when (forsikring) {
                    is SelvstendigForsikring ->
                        if (Toggle.SelvstendigForsikring.enabled) aktivitetslogg.varsel(Varselkode.RV_AN_6)
                        else aktivitetslogg.funksjonellFeil(Varselkode.RV_AN_6)

                    KollektivJordbruksforsikring,
                    null -> {
                    }
                }
        }
        if (!harOpptjening) aktivitetslogg.varsel(RV_OV_1)

        if (grunnlagsdata.inntektsgrunnlag.er6GBegrenset())
            aktivitetslogg.info("Redusert utbetaling minst én dag på grunn av inntekt over 6G")
        else
            aktivitetslogg.info("Utbetaling har ikke blitt redusert på grunn av 6G")

        infotrygdhistorikk.validerMedVarsel(aktivitetslogg, periode)
        infotrygdhistorikk.validerNyereOpplysninger(aktivitetslogg, periode)
        ytelser.valider(aktivitetslogg, periode, skjæringstidspunkt, behandlinger.maksdato.maksdato, forlengerVedtak())
    }

    private fun subsummering(
        beregningsgrunnlag: Inntekt,
        minsteinntektsvurdering: Minsteinntektsvurdering,
        uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
        beregnetTidslinjePerVedtaksperiode: List<BeregnetPeriode>,
        historisktidslinje: Utbetalingstidslinje
    ) {
        val subsumsjonen = Utbetalingstidslinjesubsumsjon(subsumsjonslogg, sykdomstidslinje, behandlinger.utbetalingstidslinje())
        subsumsjonen.subsummer(periode, yrkesaktivitet.yrkesaktivitetstype)

        sykdomsgradsubsummering(subsumsjonslogg, periode, uberegnetTidslinjePerArbeidsgiver, beregnetTidslinjePerVedtaksperiode)
        minsteinntektsvurdering.subsummere(subsumsjonslogg, skjæringstidspunkt, beregningsgrunnlag, person.alder.redusertYtelseAlder, periode)
        maksdatosubsummering(
            subsumsjonslogg = subsumsjonslogg,
            periode = periode,
            syttiårsdagen = person.alder.syttiårsdagen,
            uberegnetTidslinjePerArbeidsgiver = uberegnetTidslinjePerArbeidsgiver,
            historisktidslinje = historisktidslinje,
            resultat = behandlinger.maksdato
        )
    }

    internal fun håndterUtbetalingsavgjørelse(eventBus: EventBus, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (utbetalingsavgjørelse.vedtaksperiodeId != this.id) return null

        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        return when (tilstand) {
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering -> behandleAvgjørelseForVedtak(eventBus, utbetalingsavgjørelse, aktivitetsloggMedVedtaksperiodekontekst, TilUtbetaling, Avsluttet)

            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerGodkjenningRevurdering -> behandleAvgjørelseForVedtak(eventBus, utbetalingsavgjørelse, aktivitetsloggMedVedtaksperiodekontekst, SelvstendigTilUtbetaling, SelvstendigAvsluttet)

            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            ArbeidstakerStart,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> {
                error("Forventet ikke utbetalingsavgjørelse i ${tilstand.type.name}")
            }
        }
    }

    private fun behandleAvgjørelseForVedtak(eventBus: EventBus, utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg, nesteTilUtbetalingtilstand: Vedtaksperiodetilstand, nesteAvsluttettilstand: Vedtaksperiodetilstand): Revurderingseventyr? {
        check(utbetalingsavgjørelse.behandlingId == behandlinger.sisteBehandlingId) { "Utbetalingsavgjørelse gjelder en annen behandling" }
        check(utbetalingsavgjørelse.utbetalingId == behandlinger.utbetaling?.id) { "Utbetalingsavgjørelse gjelder en annen utbetaling" }

        with(utbetalingsavgjørelse) {
            when (godkjent) {
                true -> vedtakFattet(eventBus, aktivitetslogg, nesteTilUtbetalingtilstand, nesteAvsluttettilstand)
                false -> return vedtakAvvist(eventBus, aktivitetslogg)
            }
        }
        return null
    }

    private fun Behandlingsavgjørelse.vedtakAvvist(eventBus: EventBus, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (automatisert) aktivitetslogg.info("Utbetaling markert som ikke godkjent automatisk $avgjørelsestidspunkt")
        else aktivitetslogg.info("Utbetaling markert som ikke godkjent av saksbehandler ${saksbehandler()} $avgjørelsestidspunkt")

        if (behandlinger.vedtakAvvist(eventBus.behandlingEventBus, yrkesaktivitet, this, aktivitetslogg)) {
            forkast(eventBus, this, aktivitetslogg, tvingForkasting = true)
            return Revurderingseventyr.forkasting(this, skjæringstidspunkt, periode)
        }

        aktivitetslogg.info("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
        return null
    }

    private fun Behandlingsavgjørelse.vedtakFattet(eventBus: EventBus, aktivitetslogg: IAktivitetslogg, nesteTilUtbetalingtilstand: Vedtaksperiodetilstand, nesteAvsluttettilstand: Vedtaksperiodetilstand) {
        if (automatisert) aktivitetslogg.info("Utbetaling markert som godkjent automatisk $avgjørelsestidspunkt")
        else aktivitetslogg.info("Utbetaling markert som godkjent av saksbehandler ${saksbehandler()} $avgjørelsestidspunkt")

        val erVedtakIverksatt = behandlinger
            .vedtakFattet(eventBus.behandlingEventBus, yrkesaktivitet, this, aktivitetslogg)
            .erAvsluttet()

        when (erVedtakIverksatt) {
            true -> vedtakIverksatt(eventBus, aktivitetslogg, nesteAvsluttettilstand)
            false -> tilstand(eventBus, aktivitetslogg, nesteTilUtbetalingtilstand)
        }
    }

    internal fun nullKronerRefusjonOmViManglerRefusjonsopplysninger(eventBus: EventBus, hendelseMetadata: HendelseMetadata, aktivitetslogg: IAktivitetslogg, dokumentsporing: Dokumentsporing? = null) {
        if (refusjonstidslinje.isNotEmpty()) return

        val ingenRefusjon = Beløpstidslinje.fra(
            periode = periode,
            beløp = INGEN,
            kilde = Kilde(
                meldingsreferanseId = hendelseMetadata.meldingsreferanseId,
                avsender = Avsender.SYSTEM,
                tidsstempel = hendelseMetadata.innsendt
            )
        )
        behandlinger.håndterRefusjonstidslinje(
            behandlingEventBus = eventBus.behandlingEventBus,
            dokumentsporing = dokumentsporing,
            aktivitetslogg = aktivitetslogg,
            benyttetRefusjonsopplysninger = ingenRefusjon
        )
    }

    internal fun håndterVilkårsgrunnlag(eventBus: EventBus, vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
        if (!vilkårsgrunnlag.erRelevant(aktivitetslogg, id, skjæringstidspunkt)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val nesteTilstand = when (tilstand) {
            AvventerVilkårsprøving -> AvventerHistorikk
            AvventerVilkårsprøvingRevurdering -> AvventerHistorikkRevurdering
            SelvstendigAvventerVilkårsprøving -> SelvstendigAvventerHistorikk
            SelvstendigAvventerVilkårsprøvingRevurdering -> SelvstendigAvventerHistorikkRevurdering

            ArbeidsledigAvventerBlokkerendePeriode,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigStart,
            ArbeidstakerStart,
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerRevurdering,
            SelvstendigAvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerSøknadForOverlappendePeriode,
            FrilansAvventerBlokkerendePeriode,
            FrilansAvventerInfotrygdHistorikk,
            FrilansStart,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerRevurderingTilUtbetaling,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke vilkårsgrunnlag i %s".format(tilstand.type))
        }
        håndterVilkårsgrunnlag(eventBus, vilkårsgrunnlag, aktivitetsloggMedVedtaksperiodekontekst.medFeilSomVarslerHvisNødvendig(), nesteTilstand)
    }

    internal fun håndterSimulering(eventBus: EventBus, simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
        if (simulering.vedtaksperiodeId != this.id.toString()) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val nesteTilstand = when (tilstand) {
            AvventerSimulering -> AvventerGodkjenning
            AvventerSimuleringRevurdering -> AvventerGodkjenningRevurdering
            SelvstendigAvventerSimulering -> SelvstendigAvventerGodkjenning
            SelvstendigAvventerSimuleringRevurdering -> SelvstendigAvventerGodkjenningRevurdering

            ArbeidsledigAvventerBlokkerendePeriode,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigStart,
            ArbeidstakerStart,
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSøknadForOverlappendePeriode,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            FrilansAvventerBlokkerendePeriode,
            FrilansAvventerInfotrygdHistorikk,
            FrilansStart,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerRevurdering,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerRevurderingTilUtbetaling,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke simulering i %s".format(tilstand.type.name))
        }

        val wrapper = aktivitetsloggMedVedtaksperiodekontekst.medFeilSomVarslerHvisNødvendig()
        with(checkNotNull(behandlinger.utbetaling)) {
            valider(simulering, wrapper)
            if (!erKlarForGodkjenning()) return wrapper.info("Kan ikke gå videre da begge oppdragene ikke er simulert.")
            tilstand(eventBus, wrapper, nesteTilstand)
        }
    }

    internal fun håndterUtbetalingHendelse(eventBus: EventBus, hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (hendelse.vedtaksperiodeId != this.id) return

        behandlinger.forrigeBehandling.håndterUtbetalinghendelse(eventBus.behandlingEventBus, hendelse, aktivitetslogg)

        if (!behandlinger.forrigeBehandling.erAvsluttet()) return

        when (tilstand) {
            TilAnnullering -> vedtakAnnullert(eventBus, hendelse, aktivitetslogg)

            AvventerAnnulleringTilUtbetaling -> vedtakIverksatt(eventBus, aktivitetsloggMedVedtaksperiodekontekst, AvventerAnnullering)
            AvventerRevurderingTilUtbetaling -> vedtakIverksatt(eventBus, aktivitetsloggMedVedtaksperiodekontekst, AvventerRevurdering)
            SelvstendigAvventerRevurderingTilUtbetaling -> vedtakIverksatt(eventBus, aktivitetsloggMedVedtaksperiodekontekst, SelvstendigAvventerRevurdering)
            SelvstendigTilUtbetaling -> vedtakIverksatt(eventBus, aktivitetsloggMedVedtaksperiodekontekst, SelvstendigAvsluttet)
            TilUtbetaling -> vedtakIverksatt(eventBus, aktivitetsloggMedVedtaksperiodekontekst, Avsluttet)

            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            AvventerRevurdering,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerRevurdering,
            ArbeidstakerStart,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            TilInfotrygd -> error("forventer ikke utbetalinghendelse i $tilstand")
        }
    }

    internal fun vedtakAnnullert(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        eventBus.vedtaksperiodeAnnullert(
            EventSubscription.VedtaksperiodeAnnullertEvent(
                fom = periode.start,
                tom = periode.endInclusive,
                vedtaksperiodeId = id,
                yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                behandlingId = behandlinger.sisteBehandlingId
            )
        )
        forkast(eventBus, hendelse, aktivitetslogg)

        // lager varsel perioider etter som ikke har blitt forkastet enda
        person
            .vedtaksperioder(OVERLAPPENDE_OG_ETTERGØLGENDE(this))
            .filterNot { it.behandlinger.skalAnnulleres() }
            .forEach {
                it.registrerKontekst(aktivitetslogg).varsel(RV_RV_7)
            }
    }

    private fun selvstendigUtgåendeEventer(eventBus: EventBus) {
        if (yrkesaktivitet.yrkesaktivitetstype != Selvstendig) return

        if (behandlinger.maksdato.gjenståendeDager == 0) {
            eventBus.selvstendigIngenDagerIgjen(
                EventSubscription.SelvstendigIngenDagerIgjenEvent(
                    behandlingId = behandlinger.sisteBehandlingId,
                    skjæringstidspunkt = this.skjæringstidspunkt
                )
            )
        }

        val datoForFørsteNavDag = yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(this).vedtaksperioder
            .flatMap { it.behandlinger.utbetalingstidslinje() }
            .filterIsInstance<NavDag>()
            .minOfOrNull { it.dato }

        if (behandlinger.dagerNavOvertarAnsvar.isNotEmpty() && datoForFørsteNavDag in periode) {
            eventBus.selvstendigUtbetaltEtterVentetid(
                EventSubscription.SelvstendigUtbetaltEtterVentetidEvent(
                    behandlingId = behandlinger.sisteBehandlingId,
                    skjæringstidspunkt = this.skjæringstidspunkt
                )
            )
        }
    }

    private fun vedtakIverksatt(eventBus: EventBus, aktivitetslogg: IAktivitetslogg, nesteTilstand: Vedtaksperiodetilstand) {
        tilstand(eventBus, aktivitetslogg, nesteTilstand)
        val utkastTilVedtakBuilder = utkastTilVedtakBuilder(behandlinger.forrigeBehandling)
        eventBus.avsluttetMedVedtak(utkastTilVedtakBuilder.buildAvsluttedMedVedtak())
        eventBus.analytiskDatapakke(behandlinger.analytiskDatapakke(yrkesaktivitet.yrkesaktivitetstype, this@Vedtaksperiode.id))
        selvstendigUtgåendeEventer(eventBus)
    }

    internal fun håndterAnnullerUtbetaling(
        eventBus: EventBus,
        hendelse: AnnullerUtbetaling,
        aktivitetslogg: IAktivitetslogg,
        annulleringskandidater: List<Vedtaksperiode>
    ): Revurderingseventyr? {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        if (!annulleringskandidater.contains(this)) return null

        val sisteVedtaksperiodeFørMegSelvMedSammenhengendeUtbetaling = yrkesaktivitet.finnSisteVedtaksperiodeFørMedSammenhengendeUtbetaling(this)
        val periodeForEndring = sisteVedtaksperiodeFørMegSelvMedSammenhengendeUtbetaling?.periode ?: periode

        return when (tilstand) {
            Avsluttet,
            SelvstendigAvsluttet -> håndterAnnulleringNyBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)

            TilUtbetaling,
            SelvstendigTilUtbetaling -> håndterAnnulleringNyBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnulleringTilUtbetaling)

            AvventerGodkjenningRevurdering -> when {
                behandlinger.erAvvist() -> håndterAnnulleringNyBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)
                else -> håndterAnnulleringÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)
            }

            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,

            AvventerSimuleringRevurdering,
            AvventerVilkårsprøvingRevurdering,
            AvventerHistorikkRevurdering,
            AvventerRevurdering -> håndterAnnulleringÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)

            AvventerRevurderingTilUtbetaling -> håndterAnnulleringÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnulleringTilUtbetaling)

            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerRevurdering -> håndterAnnulleringÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)

            SelvstendigAvventerGodkjenningRevurdering -> when {
                behandlinger.erAvvist() -> håndterAnnulleringNyBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)
                else -> håndterAnnulleringÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnullering)
            }

            SelvstendigAvventerRevurderingTilUtbetaling -> håndterAnnulleringÅpenBehandling(eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst, periodeForEndring, AvventerAnnulleringTilUtbetaling)

            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidstakerStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            SelvstendigStart,
            AvsluttetUtenUtbetaling,
            AvventerAvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerSimulering,
            AvventerVilkårsprøving,
            TilAnnullering -> null

            TilInfotrygd -> error("Forventet ikke annulleringshendelse i tilstand $tilstand for vedtaksperiodeId $id")
        }
    }

    private fun håndterAnnulleringNyBehandling(eventBus: EventBus, hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg, periodeForEndring: Periode, nesteTilstand: Vedtaksperiodetilstand): Revurderingseventyr {
        behandlinger.nyAnnulleringBehandling(
            behandlingEventBus = eventBus.behandlingEventBus,
            yrkesaktivitet = yrkesaktivitet,
            behandlingkilde = hendelse.metadata.behandlingkilde
        )
        tilstand(eventBus, aktivitetslogg, nesteTilstand)
        return annullering(hendelse, periodeForEndring)
    }

    private fun håndterAnnulleringÅpenBehandling(eventBus: EventBus, hendelse: AnnullerUtbetaling, aktivitetslogg: IAktivitetslogg, periodeForEndring: Periode, nesteTilstand: Vedtaksperiodetilstand): Revurderingseventyr {
        behandlinger.håndterAnnullering(
            behandlingEventBus = eventBus.behandlingEventBus,
            aktivitetslogg = aktivitetslogg
        )
        tilstand(eventBus, aktivitetslogg, nesteTilstand)
        return annullering(hendelse, periodeForEndring)
    }

    internal fun håndterPåminnelse(eventBus: EventBus, påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!påminnelse.erRelevant(id)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        return tilstand.påminnelse(eventBus, this, påminnelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndterOverstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!overstyrArbeidsgiveropplysninger.erRelevant(skjæringstidspunkt)) return null
        if (vilkårsgrunnlag?.erArbeidsgiverRelevant(yrkesaktivitet.organisasjonsnummer) != true) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val grunnlag = vilkårsgrunnlag ?: return null
        val (nyttGrunnlag, endredeArbeidsgivere) = grunnlag.håndterKorrigerteInntekter(overstyrArbeidsgiveropplysninger, subsumsjonslogg) ?: return null
        person.lagreVilkårsgrunnlag(nyttGrunnlag)

        endredeArbeidsgivere.forEach { endretArbeidsgiver ->
            overstyrArbeidsgiveropplysninger.subsummer(subsumsjonslogg, endretArbeidsgiver.startdato, endretArbeidsgiver.organisasjonsnummer)
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

    internal fun håndterRefusjonLPSEllerOverstyring(eventBus: EventBus, hendelse: Hendelse, dokumentsporing: Dokumentsporing, aktivitetslogg: IAktivitetslogg, servitør: Refusjonsservitør): Revurderingseventyr? {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        return håndterRefusjon(eventBus, hendelse, dokumentsporing, aktivitetsloggMedVedtaksperiodekontekst, servitør)
    }

    private fun håndterRefusjon(eventBus: EventBus, hendelse: Hendelse, dokumentsporing: Dokumentsporing, aktivitetslogg: IAktivitetslogg, servitør: Refusjonsservitør): Revurderingseventyr? {
        val refusjonstidslinje = servitør.servér(startdatoPåSammenhengendeVedtaksperioder, periode)
        if (refusjonstidslinje.isEmpty()) return null

        // refusjonshåndteringen er litt spesiell i og med at vi bare behandler den hvis det er en funksjonell endring
        val benyttetRefusjonsopplysninger = behandlinger.endretRefusjonstidslinje(refusjonstidslinje) ?: return null

        when (tilstand) {
            AvsluttetUtenUtbetaling,
            Avsluttet,
            TilUtbetaling -> sørgForNyBehandlingHvisIkkeÅpenOgOppdaterSkjæringstidspunktOgDagerUtenNavAnsvar(eventBus, hendelse)

            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerAvsluttetUtenUtbetaling,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering -> {
            }

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilUtbetaling,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            ArbeidstakerStart,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            TilAnnullering,
            TilInfotrygd -> error("Forventer ikke å håndtere refusjon i tilstand $tilstand")
        }

        behandlinger.håndterRefusjonstidslinje(
            behandlingEventBus = eventBus.behandlingEventBus,
            dokumentsporing = dokumentsporing,
            aktivitetslogg = aktivitetslogg,
            benyttetRefusjonsopplysninger = benyttetRefusjonsopplysninger
        )
        return Revurderingseventyr.refusjonsopplysninger(hendelse, skjæringstidspunkt, periode)
    }

    override fun compareTo(other: Vedtaksperiode): Int {
        val delta = this.periode.start.compareTo(other.periode.start)
        if (delta != 0) return delta
        return this.periode.endInclusive.compareTo(other.periode.endInclusive)
    }

    internal infix fun før(other: Vedtaksperiode) = this < other
    internal infix fun etter(other: Vedtaksperiode) = this > other

    private fun forlengerVedtak(): Boolean = yrkesaktivitet
        .vedtaksperioderMedSammeFørsteFraværsdag(this).før.lastOrNull()
        ?.behandlinger?.harFattetVedtak() == true

    internal fun forkast(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, tvingForkasting: Boolean = false) {
        val forkastinger = forkastingskandidater(aktivitetslogg, tvingForkasting)
        person.søppelbøtte(eventBus, hendelse, aktivitetslogg, forkastinger)
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
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg
    ): VedtaksperiodeForkastetEventBuilder {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Forkaster vedtaksperiode: %s", this.id.toString())

        val vedtaksperiodeForkastetEventBuilder = VedtaksperiodeForkastetEventBuilder()


        if (tilstand in setOf(AvventerInfotrygdHistorikk, ArbeidstakerStart)) {
            vedtaksperiodeForkastetEventBuilder.trengerArbeidsgiveropplysninger(yrkesaktivitet.trengerArbeidsgiveropplysninger(periode))
        }

        when (this.tilstand) {
            Avsluttet,
            SelvstendigAvsluttet,
            AvventerGodkjenningRevurdering,
            AvventerHistorikkRevurdering,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigTilUtbetaling,
            TilUtbetaling,
            TilInfotrygd -> {
                error("Kan ikke forkaste i $tilstand")
            }

            AvsluttetUtenUtbetaling -> {
                if (!behandlinger.åpenForEndring()) {
                    behandlinger.nyForkastetBehandling(
                        behandlingEventBus = eventBus.behandlingEventBus,
                        yrkesaktivitet = yrkesaktivitet,
                        behandlingkilde = hendelse.metadata.behandlingkilde,
                        automatiskBehandling = hendelse.metadata.automatiskBehandling
                    )
                } else {
                    this.behandlinger.forkastÅpenBehandling(eventBus, eventBus.behandlingEventBus, yrkesaktivitet, hendelse.metadata.behandlingkilde, hendelse.metadata.automatiskBehandling, aktivitetsloggMedVedtaksperiodekontekst)
                }
            }

            AvventerAvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerSimulering,
            AvventerVilkårsprøving,
            ArbeidstakerStart,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            SelvstendigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode -> {
                this.behandlinger.forkastÅpenBehandling(eventBus, eventBus.behandlingEventBus, yrkesaktivitet, hendelse.metadata.behandlingkilde, hendelse.metadata.automatiskBehandling, aktivitetsloggMedVedtaksperiodekontekst)
            }

            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving -> {
                check(!this.behandlinger.harFattetVedtak()) { "kan ikke forkaste en utbetalt vedtaksperiode uten å annullere først" }
                this.behandlinger.forkastÅpenBehandling(eventBus, eventBus.behandlingEventBus, yrkesaktivitet, hendelse.metadata.behandlingkilde, hendelse.metadata.automatiskBehandling, aktivitetsloggMedVedtaksperiodekontekst)
            }

            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            TilAnnullering -> {
                check(behandlinger.erAnnullert()) { "må være annullert for å forkastes" }
            }
        }

        tilstand(eventBus, aktivitetsloggMedVedtaksperiodekontekst, TilInfotrygd)
        return vedtaksperiodeForkastetEventBuilder
    }

    internal inner class VedtaksperiodeForkastetEventBuilder {
        private val gjeldendeTilstand = tilstand.type
        private var sykmeldingsperioder: List<Periode> = emptyList()
        internal fun trengerArbeidsgiveropplysninger(sykmeldingsperioder: List<Periode>) {
            this.sykmeldingsperioder = sykmeldingsperioder
        }

        internal fun buildAndEmit(eventBus: EventBus) {
            eventBus.vedtaksperiodeForkastet(
                EventSubscription.VedtaksperiodeForkastetEvent(
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
        eventBus: EventBus,
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

        emitVedtaksperiodeEndret(eventBus, previousState)
        tilstand.entering(this, eventBus, event)
    }

    private fun oppdaterHistorikk(
        eventBus: EventBus,
        dokumentsporing: Dokumentsporing,
        hendelseSykdomstidslinje: Sykdomstidslinje,
        aktivitetslogg: IAktivitetslogg,
        dagerNavOvertarAnsvar: List<Periode>? = null,
        validering: () -> Unit
    ) {
        val haddeFlereSkjæringstidspunkt = behandlinger.harFlereSkjæringstidspunkt()
        behandlinger.håndterSykdomstidslinje(
            behandlingEventBus = eventBus.behandlingEventBus,
            yrkesaktivitet = yrkesaktivitet,
            dokumentsporing = dokumentsporing,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            egenmeldingsdagerAndrePerioder = yrkesaktivitet.egenmeldingsperioderUnntatt(this),
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            aktivitetslogg = aktivitetslogg,
            validering = validering
        )
        if (!haddeFlereSkjæringstidspunkt && behandlinger.harFlereSkjæringstidspunkt()) {
            aktivitetslogg.varsel(RV_IV_11)
        }
    }

    private fun nullstillEgenmeldingsdager(eventBus: EventBus, hendelse: Hendelse, dokumentsporing: Dokumentsporing?, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        // Trenger ikke gjøre det om det ikke er noen egenmeldingsdager fra før
        if (behandlinger.egenmeldingsdager().isEmpty()) return null

        when (tilstand) {
            Avsluttet,
            AvsluttetUtenUtbetaling,
            TilUtbetaling -> nyBehandling(eventBus, hendelse)

            AvventerAvsluttetUtenUtbetaling,
            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            AvventerBlokkerendePeriode,
            AvventerSøknadForOverlappendePeriode,
            AvventerInntektsopplysningerForAnnenArbeidsgiver,
            AvventerRefusjonsopplysningerAnnenPeriode,
            AvventerGodkjenning,
            AvventerGodkjenningRevurdering,
            AvventerHistorikk,
            AvventerHistorikkRevurdering,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerRevurdering,
            AvventerRevurderingTilUtbetaling,
            AvventerSimulering,
            AvventerSimuleringRevurdering,
            AvventerVilkårsprøving,
            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigAvventerGodkjenningRevurdering,
            SelvstendigAvventerHistorikkRevurdering,
            SelvstendigAvventerSimuleringRevurdering,
            SelvstendigAvventerRevurderingTilUtbetaling,
            SelvstendigAvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerRevurdering,
            FrilansStart,
            FrilansAvventerInfotrygdHistorikk,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigStart,
            ArbeidsledigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerBlokkerendePeriode,
            SelvstendigTilUtbetaling,
            ArbeidstakerStart,
            TilAnnullering,
            TilInfotrygd,
            TilUtbetaling -> {
            }
        }

        behandlinger.nullstillEgenmeldingsdager(
            behandlingEventBus = eventBus.behandlingEventBus,
            yrkesaktivitet = yrkesaktivitet,
            dokumentsporing = dokumentsporing,
            aktivitetslogg = aktivitetslogg,
        )
        return Revurderingseventyr.arbeidsgiverperiode(hendelse, skjæringstidspunkt, periode)
    }

    internal fun nullstillEgenmeldingsdagerIArbeidsgiverperiode(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, dokumentsporing: Dokumentsporing?): List<Revurderingseventyr> {
        val arbeidsgiverperiode = behandlinger.ventedager().dagerUtenNavAnsvar.periode ?: return emptyList()
        return yrkesaktivitet.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode)
            .mapNotNull { it.nullstillEgenmeldingsdager(eventBus, hendelse, dokumentsporing, it.registrerKontekst(aktivitetslogg)) }
    }

    private fun håndterSøknad(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg
    ) {
        oppdaterHistorikk(eventBus, søknad(søknad.metadata.meldingsreferanseId), søknad.sykdomstidslinje, aktivitetslogg) {
            videreførEksisterendeRefusjonsopplysninger(eventBus, søknad(søknad.metadata.meldingsreferanseId), aktivitetslogg)
            søknad.valider(aktivitetslogg, vilkårsgrunnlag, refusjonstidslinje, subsumsjonslogg, skjæringstidspunkt)
        }
    }

    private fun håndterOverlappendeSøknad(
        eventBus: EventBus,
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (søknad.delvisOverlappende) return aktivitetslogg.funksjonellFeil(`Mottatt søknad som delvis overlapper`)
        aktivitetslogg.info("Håndterer overlappende søknad")
        håndterSøknad(eventBus, søknad, aktivitetslogg)
    }

    private fun håndterOverlappendeSøknadRevurdering(eventBus: EventBus, søknad: Søknad, aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Søknad har trigget en revurdering")
        oppdaterHistorikk(eventBus, søknad(søknad.metadata.meldingsreferanseId), søknad.sykdomstidslinje, aktivitetslogg) {
            if (søknad.delvisOverlappende) aktivitetslogg.varsel(`Mottatt søknad som delvis overlapper`)
            søknad.valider(FunksjonelleFeilTilVarsler(aktivitetslogg), vilkårsgrunnlag, refusjonstidslinje, subsumsjonslogg, skjæringstidspunkt)
        }
    }

    internal fun håndterKorrigerendeInntektsmelding(eventBus: EventBus, dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val opprinneligAgp = behandlinger.ventedager()
        if (dager.erKorrigeringForGammel(aktivitetslogg, opprinneligAgp.dagerUtenNavAnsvar.dager)) {
            håndterDagerUtenEndring(eventBus, dager, aktivitetslogg)
        } else {
            håndterDager(eventBus, dager, aktivitetslogg)
        }
        val nyAgp = behandlinger.ventedager()
        if (opprinneligAgp != nyAgp) aktivitetslogg.varsel(RV_IM_24, "Ny agp er utregnet til å være ulik tidligere utregnet agp i ${tilstand.type.name}")

        if (aktivitetslogg.harFunksjonelleFeil()) return forkast(eventBus, dager.hendelse, aktivitetslogg)
    }

    private fun korrigertInntektForArbeidsgiver(alleForSammeArbeidsgiver: List<Vedtaksperiode>) = alleForSammeArbeidsgiver.mapNotNull { it.behandlinger.korrigertInntekt }.maxByOrNull { it.inntektsdata.tidsstempel }

    private fun inntektssituasjon(alleForSammeArbeidsgiver: List<Vedtaksperiode>): Inntektssituasjon {
        val inntektFraArbeidsgiver = alleForSammeArbeidsgiver.arbeidstakerFaktaavklarteInntekter()?.let { Inntektssituasjon.HarInntektFraArbeidsgiver(it) }

        return when {
            inntektFraArbeidsgiver != null -> inntektFraArbeidsgiver
            alleForSammeArbeidsgiver.none { it.skalArbeidstakerBehandlesISpeil() } -> Inntektssituasjon.TrengerIkkeInntektFraArbeidsgiver
            alleForSammeArbeidsgiver.any { it.behandlinger.erTidligereVilkårsprøvd() } -> Inntektssituasjon.TidligereVilkårsprøvd
            else -> {
                // Vi vet at vi skal "Behandles i speil", at vi ikke er tidligere vilkårsprøvd (så ikke noe revurderingscase) - så da er vi enten den som vilkårsprøver eller en annen arbeidsgiver som venter på vilkårsprøvingen
                val periodenSomGaOpp = alleForSammeArbeidsgiver.first { it.tilstand in setOf(AvventerVilkårsprøving, AvventerBlokkerendePeriode, AvventerInntektsopplysningerForAnnenArbeidsgiver) }
                Inntektssituasjon.GaOppÅVentePåArbeidsgiver(periodenSomGaOpp)
            }
        }
    }

    private fun SkatteopplysningerForSykepengegrunnlag?.somFaktaavklartInntekt(hendelse: Hendelse) = when {
        this != null -> ArbeidstakerFaktaavklartInntekt(UUID.randomUUID(), this.inntektsdata, Arbeidstakerinntektskilde.AOrdningen(this.treMånederFørSkjæringstidspunkt))
        else -> ArbeidstakerFaktaavklartInntekt(UUID.randomUUID(), Inntektsdata.ingen(hendelse.metadata.meldingsreferanseId, skjæringstidspunkt), Arbeidstakerinntektskilde.AOrdningen(emptyList()))
    }

    private fun faktaavklartInntektForArbeidsgiver(
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetsloggTilDenSomVilkårsprøver: IAktivitetslogg,
        skatteopplysning: SkatteopplysningerForSykepengegrunnlag?,
        alleForSammeArbeidsgiver: List<Vedtaksperiode>,
        flereArbeidsgivere: Boolean
    ): ArbeidstakerFaktaavklartInntekt {
        val inntektssituasjon = inntektssituasjon(alleForSammeArbeidsgiver)
        aktivitetsloggTilDenSomVilkårsprøver.info("Arbeidsgiver ${yrkesaktivitet.organisasjonsnummer} har inntektssituasjon ${inntektssituasjon::class.simpleName} på skjæringstidspunktet $skjæringstidspunkt")

        val benyttetFaktaavklartInntekt = when (inntektssituasjon) {
            is Inntektssituasjon.HarInntektFraArbeidsgiver -> inntektssituasjon.avklarInntekt(skjæringstidspunkt, skatteopplysning.somFaktaavklartInntekt(hendelse), flereArbeidsgivere, aktivitetsloggTilDenSomVilkårsprøver)

            Inntektssituasjon.TrengerIkkeInntektFraArbeidsgiver -> skatteopplysning.somFaktaavklartInntekt(hendelse)

            Inntektssituasjon.TidligereVilkårsprøvd -> {
                // Her legger vi også skatt til grunn, men for å unngå at sykmeldte får gjentatte meldinger om skatteinntekter lagt til grunn blir det her kun varsel til saksbehandler
                aktivitetsloggTilDenSomVilkårsprøver.varsel(RV_IV_10)
                skatteopplysning.somFaktaavklartInntekt(hendelse)
            }

            is Inntektssituasjon.GaOppÅVentePåArbeidsgiver -> {
                val faktaavklartSkatteinntekt = skatteopplysning.somFaktaavklartInntekt(hendelse)
                val skatteinntekter = (faktaavklartSkatteinntekt.inntektsopplysningskilde as Arbeidstakerinntektskilde.AOrdningen).inntektsopplysninger
                val omregnetÅrsinntekt = Skatteopplysning.omregnetÅrsinntekt(skatteinntekter)

                aktivitetsloggTilDenSomVilkårsprøver.varsel(RV_IV_10)
                val event = EventSubscription.SkatteinntekterLagtTilGrunnEvent(
                    arbeidstaker = yrkesaktivitet.yrkesaktivitetstype.somArbeidstakerOrThrow,
                    vedtaksperiodeId = inntektssituasjon.periodenSomGaOpp.id,
                    behandlingId = inntektssituasjon.periodenSomGaOpp.behandlinger.sisteBehandlingId,
                    skjæringstidspunkt = skjæringstidspunkt,
                    skatteinntekter = skatteinntekter.map {
                        EventSubscription.SkatteinntekterLagtTilGrunnEvent.Skatteinntekt(it.måned, it.beløp.månedlig)
                    },
                    omregnetÅrsinntekt = omregnetÅrsinntekt.årlig
                )
                eventBus.sendSkatteinntekterLagtTilGrunn(event)
                faktaavklartSkatteinntekt
            }
        }

        if (benyttetFaktaavklartInntekt.inntektsopplysningskilde is Arbeidstakerinntektskilde.AOrdningen)
            subsummerBrukAvSkatteopplysninger(yrkesaktivitet.organisasjonsnummer, benyttetFaktaavklartInntekt.inntektsdata, skatteopplysning?.treMånederFørSkjæringstidspunkt ?: emptyList())

        return benyttetFaktaavklartInntekt
    }

    private fun avklarSykepengegrunnlagArbeidstaker(
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetsloggTilDenSomVilkårsprøver: IAktivitetslogg,
        skatteopplysning: SkatteopplysningerForSykepengegrunnlag?,
        vedtaksperioderMedSammeSkjæringstidspunkt: List<Vedtaksperiode>,
        flereArbeidsgivere: Boolean
    ): ArbeidsgiverInntektsopplysning {
        check(yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker) {
            "Skal kun avklare sykepengegrunnlag for arbeidstakere"
        }
        val alleForSammeArbeidsgiver = vedtaksperioderMedSammeSkjæringstidspunkt
            .filter { it.yrkesaktivitet === this.yrkesaktivitet }

        return ArbeidsgiverInntektsopplysning(
            orgnummer = yrkesaktivitet.organisasjonsnummer,
            faktaavklartInntekt = faktaavklartInntektForArbeidsgiver(eventBus, hendelse, aktivitetsloggTilDenSomVilkårsprøver, skatteopplysning, alleForSammeArbeidsgiver, flereArbeidsgivere),
            korrigertInntekt = korrigertInntektForArbeidsgiver(alleForSammeArbeidsgiver),
            skjønnsmessigFastsatt = null
        )
    }

    private fun avklarSykepengegrunnlagForSelvstendig(): SelvstendigInntektsopplysning? {
        return person
            .vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .firstOrNull {
                when (it.yrkesaktivitet.yrkesaktivitetstype) {
                    Arbeidsledig,
                    is Arbeidstaker,
                    Frilans -> false

                    Selvstendig -> true
                }
            }
            ?.inntektForSelvstendig()
    }

    private fun inntektForSelvstendig(): SelvstendigInntektsopplysning {
        val faktaavklartInntekt = checkNotNull(behandlinger.faktaavklartInntekt as? SelvstendigFaktaavklartInntekt) { "Forventer å ha en inntekt for selvstendig" }
        val anvendtGrunnbeløp = `1G`.beløp(skjæringstidspunkt)
        val inntektsgrunnlag = faktaavklartInntekt.normalinntekt(anvendtGrunnbeløp)

        val inntekt = faktaavklartInntekt.copy(
            inntektsdata = faktaavklartInntekt.inntektsdata.copy(beløp = inntektsgrunnlag),
            anvendtGrunnbeløp = anvendtGrunnbeløp
        )

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
        eventBus: EventBus,
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
        val førsteVedtaksperiodePerArbeidsgiver = perioderMedSammeSkjæringstidspunkt
            .distinctBy { it.yrkesaktivitet }

        return førsteVedtaksperiodePerArbeidsgiver
            .map { vedtaksperiode ->
                val skatteopplysningForArbeidsgiver = skatteopplysninger.firstOrNull { it.arbeidsgiver == vedtaksperiode.yrkesaktivitet.organisasjonsnummer }
                vedtaksperiode.avklarSykepengegrunnlagArbeidstaker(
                    eventBus = eventBus,
                    hendelse = hendelse,
                    aktivitetsloggTilDenSomVilkårsprøver = aktivitetslogg,
                    skatteopplysning = skatteopplysningForArbeidsgiver,
                    vedtaksperioderMedSammeSkjæringstidspunkt = perioderMedSammeSkjæringstidspunkt,
                    flereArbeidsgivere = førsteVedtaksperiodePerArbeidsgiver.size > 1
                )
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
        eventBus: EventBus,
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        skatteopplysninger: List<SkatteopplysningerForSykepengegrunnlag>
    ): Inntektsgrunnlag {
        val inntektsgrunnlagArbeidsgivere = inntektsgrunnlagArbeidsgivere(eventBus, hendelse, aktivitetslogg, skatteopplysninger)
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
        eventBus: EventBus,
        vilkårsgrunnlag: Vilkårsgrunnlag,
        aktivitetslogg: IAktivitetslogg,
        nesteTilstand: Vedtaksperiodetilstand
    ) {
        val skatteopplysninger = vilkårsgrunnlag.skatteopplysninger()

        val sykepengegrunnlag = avklarSykepengegrunnlag(
            eventBus = eventBus,
            hendelse = vilkårsgrunnlag,
            aktivitetslogg = aktivitetslogg,
            skatteopplysninger = skatteopplysninger
        )

        vilkårsgrunnlag.valider(aktivitetslogg, sykepengegrunnlag, subsumsjonslogg)
        val grunnlagsdata = vilkårsgrunnlag.grunnlagsdata()

        when (yrkesaktivitet.yrkesaktivitetstype) {
            is Arbeidstaker -> grunnlagsdata.validerFørstegangsvurderingArbeidstaker(aktivitetslogg)
            Selvstendig -> grunnlagsdata.validerFørstegangsvurderingSelvstendig(subsumsjonslogg)
            Arbeidsledig,
            Frilans -> error("Støtter ikke Arbeidsledig/Frilans")
        }
        person.lagreVilkårsgrunnlag(grunnlagsdata)
        aktivitetslogg.info("Vilkårsgrunnlag vurdert")
        if (aktivitetslogg.harFunksjonelleFeil()) return forkast(eventBus, vilkårsgrunnlag, aktivitetslogg)
        tilstand(eventBus, aktivitetslogg, nesteTilstand)
    }

    internal fun trengerYtelser(aktivitetslogg: IAktivitetslogg) {
        val søkevinduFamilieytelser = periode.familieYtelserPeriode
        foreldrepenger(aktivitetslogg, søkevinduFamilieytelser)
        pleiepenger(aktivitetslogg, søkevinduFamilieytelser)
        omsorgspenger(aktivitetslogg, søkevinduFamilieytelser)
        opplæringspenger(aktivitetslogg, søkevinduFamilieytelser)
        institusjonsopphold(aktivitetslogg, periode)
        arbeidsavklaringspengerV2(aktivitetslogg, periode.start.minusMonths(6), periode.endInclusive)
        dagpengerV2(aktivitetslogg, periode.start.minusMonths(2), periode.endInclusive)
        val (beregningsperiode, _) = perioderSomMåHensyntasVedBeregning()
        inntekterForBeregning(aktivitetslogg, beregningsperiode)

        when (yrkesaktivitet.yrkesaktivitetstype) {
            Arbeidsledig,
            is Arbeidstaker,
            Frilans -> {
            }

            Selvstendig -> selvstendigForsikring(aktivitetslogg, this.skjæringstidspunkt)
        }
    }

    internal fun trengerVilkårsgrunnlag(aktivitetslogg: IAktivitetslogg, eventBus: EventBus) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntekterForSykepengegrunnlag(aktivitetslogg, skjæringstidspunkt, beregningSlutt.minusMonths(2), beregningSlutt)
        inntekterForOpptjeningsvurdering(aktivitetslogg, skjæringstidspunkt, beregningSlutt, beregningSlutt)
        arbeidsforhold(aktivitetslogg, skjæringstidspunkt)
        medlemskap(aktivitetslogg, skjæringstidspunkt, periode.start, periode.endInclusive)
    }

    private fun emitVedtaksperiodeEndret(eventBus: EventBus, previousState: Vedtaksperiodetilstand) {
        val event = EventSubscription.VedtaksperiodeEndretEvent(
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

        eventBus.vedtaksperiodeEndret(event)
    }

    private fun Vedtaksperiodetilstand.påminnelse(
        eventBus: EventBus,
        vedtaksperiode: Vedtaksperiode,
        påminnelse: Påminnelse,
        aktivitetslogg: IAktivitetslogg
    ): Revurderingseventyr? {
        if (!påminnelse.gjelderTilstand(aktivitetslogg, type)) {
            eventBus.vedtaksperiodeIkkePåminnet(id, yrkesaktivitet.yrkesaktivitetstype, type)
            return null
        }
        eventBus.vedtaksperiodePåminnet(
            vedtaksperiodeId = id,
            behandlingsporing = påminnelse.behandlingsporing,
            tilstand = påminnelse.tilstand,
            antallGangerPåminnet = påminnelse.antallGangerPåminnet,
            tilstandsendringstidspunkt = påminnelse.tilstandsendringstidspunkt,
            påminnelsestidspunkt = påminnelse.påminnelsestidspunkt,
            nestePåminnelsestidspunkt = påminnelse.nestePåminnelsestidspunkt
        )
        val beregnetMakstid = { tilstandsendringstidspunkt: LocalDateTime -> makstid(tilstandsendringstidspunkt) }
        if (påminnelse.nåddMakstid(beregnetMakstid)) {
            håndterMakstid(vedtaksperiode, eventBus, påminnelse, aktivitetslogg)
            return null
        }

        val overstyring = when {
            påminnelse.når(Flagg("nullstillEgenmeldingsdager")) -> nullstillEgenmeldingsdagerIArbeidsgiverperiode(eventBus, påminnelse, aktivitetslogg, null).tidligsteEventyr()
            påminnelse.når(Flagg("ønskerReberegning"), Flagg("knertVilkårsgrunnlag")) -> {
                vedtaksperiode.person.fjernVilkårsgrunnlagPå(vedtaksperiode.skjæringstidspunkt, aktivitetslogg)
                Revurderingseventyr.reberegning(påminnelse, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            }

            påminnelse.når(Flagg("ønskerReberegning")) -> Revurderingseventyr.reberegning(påminnelse, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            else -> null
        }

        if (overstyring != null) {
            aktivitetslogg.info("Reberegner perioden ettersom det er ønsket")
            return overstyring
        }

        return håndterPåminnelse(vedtaksperiode, eventBus, påminnelse, aktivitetslogg)
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    internal fun trengerGodkjenning(eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        val utkastTilVedtakBuilder = utkastTilVedtakBuilder()
        eventBus.utkastTilVedtak(utkastTilVedtakBuilder.buildUtkastTilVedtak())

        val utbetaling = checkNotNull(behandlinger.utbetaling) { "Forventer å ha en utbetaling når vi skal sende godkjenningsbehov" }
        val aktivitetsloggMedUtbetalingkontekst = aktivitetslogg.kontekst(utbetaling)
        Aktivitet.Behov.godkjenning(aktivitetsloggMedUtbetalingkontekst, utkastTilVedtakBuilder.buildGodkjenningsbehov())
    }

    private fun utkastTilVedtakBuilder(behandling: Behandlinger.Behandling? = null): UtkastTilVedtakBuilder {
        val builder = UtkastTilVedtakBuilder(
            yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
            vedtaksperiodeId = id,
            kanForkastes = kanForkastes(),
            forlengerVedtak = forlengerVedtak(),
            harPeriodeRettFør = yrkesaktivitet.vedtaksperioderMedSammeFørsteFraværsdag(this).før.lastOrNull() != null,
            overlapperMedInfotrygd = person.erBehandletIInfotrygd(periode)
        )
        person.vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .sorted()
            .associate { it.id to it.behandlinger }
            .berik(builder)

        return behandlinger.byggUtkastTilVedtak(builder, behandling)
    }

    internal fun gjenopptaBehandling(eventBus: EventBus, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Forsøker å gjenoppta $this")
        tilstand.gjenopptaBehandling(this, eventBus, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun igangsettOverstyringPåBehandlingen(eventBus: EventBus, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        when (tilstand) {
            Avsluttet,
            TilUtbetaling,
            AvsluttetUtenUtbetaling,
            SelvstendigAvsluttet,
            SelvstendigTilUtbetaling,
            AvventerGodkjenningRevurdering -> {
                sørgForNyBehandlingHvisIkkeÅpen(eventBus, revurdering.hendelse)
                subsumsjonslogg.logg(`fvl § 35 ledd 1`())
            }

            AvventerAnnullering,
            AvventerAnnulleringTilUtbetaling,
            TilAnnullering -> return

            TilInfotrygd -> error("Forventer ikke å håndtere overstyring når vi skal til infotrygd")

            else -> {}
        }
        behandlinger.oppdaterSkjæringstidspunkt(person.skjæringstidspunkter, yrkesaktivitet.perioderUtenNavAnsvar)
        behandlinger.forkastBeregning(eventBus.behandlingEventBus, aktivitetsloggMedVedtaksperiodekontekst)
        videreførEksisterendeRefusjonsopplysninger(eventBus, null, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun igangsettOverstyringEndreTilstand(eventBus: EventBus, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        // send oppdatert forespørsel
        (tilstand as? AvventerInntektsmelding)?.sendTrengerArbeidsgiveropplysninger(this, eventBus)

        val nesteTilstand = nesteTilstandEtterIgangsattOverstyring(person.infotrygdhistorikk, this, tilstand)
        tilstand(eventBus, aktivitetsloggMedVedtaksperiodekontekst, nesteTilstand)
    }

    // gitt at du står i tilstand X, hva/hvem henter du på og hvorfor?
    internal val venterPå
        get() = when (val t = tilstand) {
            AvsluttetUtenUtbetaling -> when (skalArbeidstakerBehandlesISpeil()) {
                true -> VenterPå.SegSelv(Venteårsak.HJELP fordi Venteårsak.Hvorfor.VIL_OMGJØRES)
                false -> null
            }

            AvventerGodkjenning,
            SelvstendigAvventerGodkjenning -> VenterPå.SegSelv(Venteårsak.GODKJENNING)

            AvventerGodkjenningRevurdering,
            SelvstendigAvventerGodkjenningRevurdering -> when (behandlinger.erAvvist()) {
                true -> VenterPå.SegSelv(Venteårsak.HJELP)
                false -> VenterPå.SegSelv(Venteårsak.GODKJENNING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)
            }

            // denne er litt spesiell, fordi tilstanden er både en ventetilstand og en "det er min tur"-tilstand
            is AvventerRevurdering -> t.venterpå(this)
            // TODO: HMM, man skulle jo tro det var noe lignende arbeidstaker-versjonen, men så lenge det ikke er det så venter den vel bare på nestemann
            SelvstendigAvventerRevurdering -> VenterPå.Nestemann

            AvventerInntektsopplysningerForAnnenArbeidsgiver -> when (val annenPeriode = førstePeriodeSomVenterPåInntektAnnenArbeidsgiver()) {
                null -> VenterPå.Nestemann
                else -> VenterPå.AnnenPeriode(annenPeriode.venter(), Venteårsak.INNTEKTSMELDING)
            }

            AvventerRefusjonsopplysningerAnnenPeriode -> when (val annenPeriode = førstePeriodeSomVenterPåRefusjonsopplysninger()) {
                null -> VenterPå.Nestemann
                else -> VenterPå.AnnenPeriode(annenPeriode.venter(), Venteårsak.INNTEKTSMELDING)
            }

            // Perioder hvor det ikke er hens tur til å gjenoppta behandling, man står bare i kø
            AvventerBlokkerendePeriode,
            AvventerAvsluttetUtenUtbetaling,
            FrilansAvventerBlokkerendePeriode,
            ArbeidsledigAvventerBlokkerendePeriode,
            SelvstendigAvventerBlokkerendePeriode,
            AvventerAnnullering -> VenterPå.Nestemann

            // Disse er ganske forståelige da!
            AvventerInntektsmelding -> VenterPå.SegSelv(Venteårsak.INNTEKTSMELDING)
            AvventerSøknadForOverlappendePeriode -> VenterPå.SegSelv(Venteårsak.SØKNAD)

            // *AvventerHistorikk* -familien venter på beregning.
            //  Blir man stående her skyldes det som oftes at det er enkelte behov som ikke er besvart,
            //  men kan også være feil i hånderingen av beregningen i Spleis
            AvventerHistorikk,
            SelvstendigAvventerHistorikk -> VenterPå.SegSelv(Venteårsak.BEREGNING)

            AvventerHistorikkRevurdering,
            SelvstendigAvventerHistorikkRevurdering -> VenterPå.SegSelv(Venteårsak.BEREGNING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)

            // *AvventerSimulering- og *TilUtbetaling- venter perioden selv på svar fra utbetalingssystemet
            AvventerSimulering,
            SelvstendigAvventerSimulering,
            SelvstendigTilUtbetaling,
            TilUtbetaling -> VenterPå.SegSelv(Venteårsak.UTBETALING)

            // Denne brokete familen venter på svar fra utbetalingssystemet i en revurdering
            TilAnnullering,
            AvventerAnnulleringTilUtbetaling,
            SelvstendigAvventerRevurderingTilUtbetaling,
            AvventerRevurderingTilUtbetaling,
            AvventerSimuleringRevurdering,
            SelvstendigAvventerSimuleringRevurdering -> VenterPå.SegSelv(Venteårsak.UTBETALING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)

            // *AvventerVilkårsprøving* -familien venter på at vilkårsprøving.
            //  Blir man stående her skyldes det som oftes at det er enkelte behov som ikke er besvart,
            //  men kan også være feil i hånderingen av vilkårsgrunnlaget i Spleis
            AvventerVilkårsprøving,
            SelvstendigAvventerVilkårsprøving -> VenterPå.SegSelv(Venteårsak.VILKÅRSPRØVING)

            AvventerVilkårsprøvingRevurdering,
            SelvstendigAvventerVilkårsprøvingRevurdering -> VenterPå.SegSelv(Venteårsak.VILKÅRSPRØVING fordi Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT)

            // *AvventerInfotrygdHistorikk -familien venter på at infotrygd-historikken skal bli hentet første gang
            AvventerInfotrygdHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            ArbeidsledigAvventerInfotrygdHistorikk,
            FrilansAvventerInfotrygdHistorikk -> VenterPå.SegSelv(Venteårsak.INFOTRYGDHISTORIKK)

            // *Start -familien venter ikke på noe som helst
            ArbeidstakerStart,
            SelvstendigStart,
            ArbeidsledigStart,
            FrilansStart -> null

            // Ferdigstilte tilstander (herunder forkastede) venter ikke på noe
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

    private fun lagBeregnetBehandling(beregning: BeregnetPeriode, grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, alleInntektjusteringer: Map<Inntektskilde, Beløpstidslinje>, forsikring: Forsikring?): BeregnetBehandling {
        val beregnetBehandling = BeregnetBehandling(
            maksdatoresultat = Maksdatoresultat.oversettFra(beregning.maksdatoresultat),
            utbetalingstidslinje = beregning.utbetalingstidslinje,
            grunnlagsdata = grunnlagsdata,
            alleInntektjusteringer = alleInntektjusteringer,
            forsikring = forsikring
        )
        behandlinger.beregnetBehandling(beregnetBehandling, this.yrkesaktivitet.yrkesaktivitetstype)
        return beregnetBehandling
    }

    private fun perioderDetSkalBeregnesUtbetalingFor(): List<Vedtaksperiode> {
        // lag utbetaling for seg selv + andre overlappende perioder hos andre arbeidsgivere (som ikke er utbetalt/avsluttet allerede)
        return person
            .nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
            .filter {
                val skalBehandlesISpeil = it.yrkesaktivitet.yrkesaktivitetstype !is Arbeidstaker || it.skalArbeidstakerBehandlesISpeil()
                it.behandlinger.forventerUtbetaling(periode, skjæringstidspunkt, skalBehandlesISpeil)
            }
    }

    private fun mursteinsperioderMedSammeSkjæringstidspunkt(): List<Vedtaksperiode> {
        // lager en liste av alle vedtaksperioder (inkludert this) som har samme skjæringstidspunkt,
        // og som overlapper med hverandre
        val skjæringstidspunkt = this.skjæringstidspunkt
        return person.mursteinsperioder(this)
            .filter { it.skjæringstidspunkt == skjæringstidspunkt }
    }

    /**
     * Finner alle perioder som må beregnes sammen for at vi skal kunne vurdere alle aktuelle vilkår.
     *
     * Unngår eldre perioder som slutter før this da de skal ha blitt beregnet før this
     *
     * For eksempel kan listen returnere senere perioder som ikke overlapper med this i det hele tatt,
     * men som overlapper med en periode som overlapper med this
     */
    private fun perioderSomMåHensyntasVedBeregning(): Pair<Periode, List<Vedtaksperiode>> {
        val perioderSomMåHensyntasVedBeregning = mursteinsperioderMedSammeSkjæringstidspunkt()
            .filterNot { it.periode.endInclusive < this.periode.start }
        val tom = perioderSomMåHensyntasVedBeregning.maxOf { it.periode.endInclusive }
        val beregningsperiode = periode.start til tom
        return beregningsperiode to perioderSomMåHensyntasVedBeregning
    }

    internal fun skalArbeidstakerBehandlesISpeil(): Boolean {
        check(yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker) { "gir bare mening å kalle denne funksjonen for arbeidstakere" }
        return behandlinger.ventedager().skalFatteVedtak
    }

    internal fun harInntektOgRefusjon(): Boolean {
        check(yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker) { "gir bare mening å kalle denne funksjonen for arbeidstakere" }
        return refusjonstidslinje.isNotEmpty() && harEksisterendeInntekt()
    }

    internal fun harEksisterendeInntekt(): Boolean {
        // inntekt kreves så lenge det ikke finnes et vilkårsgrunnlag.
        // hvis det finnes et vilkårsgrunnlag så antas det at inntekten er representert der (vil vi slå ut på tilkommen inntekt-error senere hvis ikke)
        val vilkårsgrunnlag = vilkårsgrunnlag
        return vilkårsgrunnlag != null || kanAvklareInntekt()
    }

    internal fun kanAvklareInntekt(): Boolean {
        val perioderMedSammeSkjæringstidspunkt = person
            .vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .filter { it.yrkesaktivitet === this.yrkesaktivitet }

        return perioderMedSammeSkjæringstidspunkt.harArbeidstakerFaktaavklartInntekt()
    }

    internal fun lagreArbeidstakerFaktaavklartInntektPåPeriode(eventBus: EventBus, aktivitetslogg: IAktivitetslogg, skalBrukeSkatt: () -> Unit = {}) {
        check(yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker) { "gir bare mening å kalle denne funksjonen for arbeidstakere" }
        if ((behandlinger.faktaavklartInntekt as? ArbeidstakerFaktaavklartInntekt) != null) return

        check(behandlinger.åpenForEndring()) { "Hva holder du på med? Denne funksjonen skal ikke brukes av på lukkede perioder" }

        val grunnlag = vilkårsgrunnlag

        val faktaavklartInntektFraVilkårsgrunnlag = grunnlag
            ?.inntektsgrunnlag
            ?.arbeidsgiverInntektsopplysninger
            ?.firstOrNull { it.orgnummer == yrkesaktivitet.organisasjonsnummer }
            ?.faktaavklartInntekt
            ?.takeIf { it.inntektsopplysningskilde is Arbeidstakerinntektskilde.Arbeidsgiver }

        val faktaavklartInntektFraArbeidsgiver by lazy {
            yrkesaktivitet.arbeidstakerFaktaavklarteInntekter(skjæringstidspunkt)?.besteInntekt()?.faktaavklartInntekt
        }

        val benyttetFaktaavklartInntekt = faktaavklartInntektFraVilkårsgrunnlag ?: faktaavklartInntektFraArbeidsgiver

        if (benyttetFaktaavklartInntekt == null && grunnlag == null) return skalBrukeSkatt()
        if (benyttetFaktaavklartInntekt == null) return // Her har vi allerede lagt skatt til grunn/RV_SV_2-situasjon, så sånn er det med den saken..

        behandlinger.håndterFaktaavklartInntekt(
            behandlingEventBus = eventBus.behandlingEventBus,
            arbeidstakerFaktaavklartInntekt = benyttetFaktaavklartInntekt,
            aktivitetslogg = aktivitetslogg,
            dokumentsporing = inntektsmeldingInntekt(benyttetFaktaavklartInntekt.inntektsdata.hendelseId)
        )
    }

    internal fun førstePeriodeSomVenterPåRefusjonsopplysninger(): Vedtaksperiode? {
        return perioderSomMåHensyntasVedBeregning().second
            .filter { it.yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker }
            .filter { it.tilstand in setOf(ArbeidstakerStart, AvventerInntektsmelding) }
            .filterNot { it === this }
            .minOrNull()
    }

    internal fun førstePeriodeSomVenterPåInntektAnnenArbeidsgiver(): Vedtaksperiode? {
        return person.vedtaksperioder(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt))
            .filter { it.yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker }
            .filter { it.yrkesaktivitet !== this.yrkesaktivitet }
            .groupBy { it.yrkesaktivitet }
            .mapValues { (_, perArbeidsgiver) ->
                val førsteSomVenterPåInntektsmelding = perArbeidsgiver.filter { it.tilstand in setOf(ArbeidstakerStart, AvventerInntektsmelding) }.minOrNull() ?: return@mapValues null
                // Om en annen periode på samme skjæringstidspunkt & arbeidsgiver har kommet seg videre forbi AvventerInntektsmelding så har vi avklart inntekt
                førsteSomVenterPåInntektsmelding.takeUnless { perArbeidsgiver.harAvklartArbeidstakerinntekt() }
            }
            .filter { it.value != null }.mapValues { it.value!! }
            .values.minOrNull()
    }

    private fun harSammeUtbetalingSom(annenVedtaksperiode: Vedtaksperiode) = behandlinger.harSammeUtbetalingSom(annenVedtaksperiode)

    internal fun videreførEksisterendeRefusjonsopplysninger(
        eventBus: EventBus,
        dokumentsporing: Dokumentsporing?,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (refusjonstidslinje.isNotEmpty()) return

        val refusjonstidslinjeFraNabolaget = yrkesaktivitet.refusjonsopplysningerFraNabolaget(this)

        val refusjonstidslinjeFraArbeidsgiver =
            yrkesaktivitet.refusjonstidslinje(this).takeUnless { it.isEmpty() }?.also { ubrukte ->
                val unikeKilder = ubrukte.filterIsInstance<Beløpsdag>().map { it.kilde.meldingsreferanseId }.toSet()
                aktivitetslogg.info("Fant ubrukte refusjonsopplysninger for $periode fra kildene ${unikeKilder.joinToString()}")
            } ?: Beløpstidslinje()

        val benyttetRefusjonstidslinje = (refusjonstidslinjeFraArbeidsgiver + refusjonstidslinjeFraNabolaget).fyll(periode)
        if (benyttetRefusjonstidslinje.isEmpty()) return
        this.behandlinger.håndterRefusjonstidslinje(
            eventBus.behandlingEventBus,
            dokumentsporing,
            aktivitetslogg,
            benyttetRefusjonstidslinje
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
            val (utenFørsteFraværsdag, medFørsteFraværsdag) = partition { it.førsteFraværsdag == null }

            val startdatoerPåPerioderUtenFørsteFraværsdag = utenFørsteFraværsdag.map { it.periode.start }

            val startdatoerPåPerioderMedFørsteFraværsdag = medFørsteFraværsdag
                .groupBy { it.førsteFraværsdag!! }
                .mapValues { (_, perioder) -> perioder.minOf { it.periode.start } }
                .values

            return (startdatoerPåPerioderUtenFørsteFraværsdag + startdatoerPåPerioderMedFørsteFraværsdag).toSet()
        }

        internal fun List<Vedtaksperiode>.harArbeidstakerFaktaavklartInntekt() = any { (it.behandlinger.faktaavklartInntekt as? ArbeidstakerFaktaavklartInntekt) != null }

        internal fun List<Vedtaksperiode>.arbeidstakerFaktaavklarteInntekter(): ArbeidstakerFaktaavklarteInntekter? {
            val førsteFraværsdag = firstNotNullOfOrNull { it.førsteFraværsdag } ?: first().periode.start
            val vurderbareInntekter = mapNotNull { it.behandlinger.vurderbarArbeidstakerFaktaavklartInntekt() }.takeUnless { it.isEmpty() } ?: return null
            return ArbeidstakerFaktaavklarteInntekter(førsteFraværsdag, vurderbareInntekter)
        }

        internal fun List<Vedtaksperiode>.medSammeUtbetaling(vedtaksperiodeSomForsøkesAnnullert: Vedtaksperiode) = this.filter { it.harSammeUtbetalingSom(vedtaksperiodeSomForsøkesAnnullert) }.toSet()

        internal fun List<Vedtaksperiode>.aktiv(vedtaksperiodeId: UUID) = any { it.id == vedtaksperiodeId }

        internal fun List<Vedtaksperiode>.igangsettOverstyring(eventBus: EventBus, revurdering: Revurderingseventyr, aktivitetslogg: IAktivitetslogg) {
            this
                .filterNot { revurdering.erIkkeRelevantFor(it.periode) }
                .onEach {
                    revurdering.inngå(
                        EventSubscription.OverstyringIgangsatt.VedtaksperiodeData(
                            yrkesaktivitetssporing = it.yrkesaktivitet.yrkesaktivitetstype,
                            vedtaksperiodeId = it.id,
                            skjæringstidspunkt = it.skjæringstidspunkt,
                            periode = it.periode,
                            typeEndring = when {
                                it.behandlinger.harFattetVedtak() -> EventSubscription.OverstyringIgangsatt.TypeEndring.REVURDERING
                                else -> EventSubscription.OverstyringIgangsatt.TypeEndring.OVERSTYRING
                            }
                        )
                    )
                }
                .onEach { it.igangsettOverstyringPåBehandlingen(eventBus, revurdering, aktivitetslogg) }
                .onEach { it.igangsettOverstyringEndreTilstand(eventBus, aktivitetslogg) }
                // Akkurat disse to tilstandene er litt kilne. Vurderinger på om de kan gå videre sjekker på tilstander
                // Så derfor er vi først avhengig av at alle perioder har gått til rett tilstand, deretter ta en ny runde
                .filter { it.tilstand in setOf(AvventerInntektsopplysningerForAnnenArbeidsgiver, AvventerRefusjonsopplysningerAnnenPeriode) }
                .onEach { it.igangsettOverstyringEndreTilstand(eventBus, aktivitetslogg) }
        }

        // Fredet funksjonsnavn
        internal val OVERLAPPENDE_OG_ETTERGØLGENDE = fun(segSelv: Vedtaksperiode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode): Boolean {
                return segSelv.periode.overlapperEllerStarterFør(other.periode)
            }
        }

        internal val SAMME_ARBEIDSGIVERPERIODE = fun(yrkesaktivitet: Yrkesaktivitet, arbeidsgiverperiode: Periode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode): Boolean {
                return other.yrkesaktivitet.organisasjonsnummer == yrkesaktivitet.organisasjonsnummer && (other.behandlinger.ventedager().dagerUtenNavAnsvar.periode?.overlapperMed(arbeidsgiverperiode) == true)
            }
        }

        private val HAR_AVVENTENDE_GODKJENNING: VedtaksperiodeFilter = {
            it.tilstand in setOf(AvventerGodkjenning, AvventerGodkjenningRevurdering, SelvstendigAvventerGodkjenning, SelvstendigAvventerGodkjenningRevurdering)
        }

        private val HAR_PÅGÅENDE_UTBETALING: VedtaksperiodeFilter = { it.behandlinger.utbetales() }
        private val IKKE_FERDIG_BEHANDLET: VedtaksperiodeFilter = { !it.tilstand.erFerdigBehandlet }

        internal val MED_SKJÆRINGSTIDSPUNKT = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.skjæringstidspunkt == skjæringstidspunkt }
        }

        internal val AUU_SOM_VIL_UTBETALES: VedtaksperiodeFilter = {
            it.tilstand == AvsluttetUtenUtbetaling && it.skalArbeidstakerBehandlesISpeil()
        }

        internal fun SPEILRELATERT(vararg perioder: Periode): VedtaksperiodeFilter {
            return fun(vedtaksperiode: Vedtaksperiode): Boolean {
                if (vedtaksperiode.yrkesaktivitet.yrkesaktivitetstype is Arbeidstaker && !vedtaksperiode.skalArbeidstakerBehandlesISpeil()) return false // Om vedtaksperioden er en AUU skal den ikke hensyntas ved vurdering på avstand mellom perioder & vedtaksperiode
                return perioder.any { periode ->
                    // Om avstand mellom vedtaksperioden og en av periodene er mindre enn 18 dager er det speilrelatert.
                    // Når det ikke er noen periode mellom (?: 0) så er det kant-i-kant/overlapp som også er speilrelatert
                    (Periode.mellom(periode, vedtaksperiode.periode)?.count() ?: 0) < MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
                }
            }
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
                    VedtaksperiodetilstandDto.ARBEIDSTAKER_START -> ArbeidstakerStart
                    VedtaksperiodetilstandDto.AVSLUTTET -> Avsluttet
                    VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING -> AvsluttetUtenUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_AVSLUTTET_UTEN_UTBETALING -> AvventerAvsluttetUtenUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> AvventerBlokkerendePeriode
                    VedtaksperiodetilstandDto.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE -> AvventerSøknadForOverlappendePeriode
                    VedtaksperiodetilstandDto.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER -> AvventerInntektsopplysningerForAnnenArbeidsgiver
                    VedtaksperiodetilstandDto.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE -> AvventerRefusjonsopplysningerAnnenPeriode
                    VedtaksperiodetilstandDto.AVVENTER_GODKJENNING -> AvventerGodkjenning
                    VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING -> AvventerGodkjenningRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_HISTORIKK -> AvventerHistorikk
                    VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING -> AvventerHistorikkRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK -> AvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING -> AvventerInntektsmelding
                    VedtaksperiodetilstandDto.AVVENTER_REVURDERING -> AvventerRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_REVURDERING_TIL_UTBETALING -> AvventerRevurderingTilUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_SIMULERING -> AvventerSimulering
                    VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING -> AvventerSimuleringRevurdering
                    VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING -> AvventerVilkårsprøving
                    VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING -> AvventerVilkårsprøvingRevurdering
                    VedtaksperiodetilstandDto.TIL_INFOTRYGD -> TilInfotrygd
                    VedtaksperiodetilstandDto.TIL_UTBETALING -> TilUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_ANNULLERING -> AvventerAnnullering
                    VedtaksperiodetilstandDto.AVVENTER_ANNULLERING_TIL_UTBETALING -> AvventerAnnulleringTilUtbetaling
                    VedtaksperiodetilstandDto.TIL_ANNULLERING -> TilAnnullering

                    VedtaksperiodetilstandDto.FRILANS_START -> FrilansStart
                    VedtaksperiodetilstandDto.FRILANS_AVVENTER_INFOTRYGDHISTORIKK -> FrilansAvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.FRILANS_AVVENTER_BLOKKERENDE_PERIODE -> FrilansAvventerBlokkerendePeriode

                    VedtaksperiodetilstandDto.ARBEIDSLEDIG_START -> ArbeidsledigStart
                    VedtaksperiodetilstandDto.ARBEIDSLEDIG_AVVENTER_INFOTRYGDHISTORIKK -> ArbeidsledigAvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.ARBEIDSLEDIG_AVVENTER_BLOKKERENDE_PERIODE -> ArbeidsledigAvventerBlokkerendePeriode

                    VedtaksperiodetilstandDto.SELVSTENDIG_START -> SelvstendigStart
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK -> SelvstendigAvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE -> SelvstendigAvventerBlokkerendePeriode
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING -> SelvstendigAvventerVilkårsprøving
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK -> SelvstendigAvventerHistorikk
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING -> SelvstendigAvventerSimulering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING -> SelvstendigAvventerGodkjenning

                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING -> SelvstendigAvventerGodkjenningRevurdering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING -> SelvstendigAvventerHistorikkRevurdering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING -> SelvstendigAvventerSimuleringRevurdering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING -> SelvstendigAvventerRevurderingTilUtbetaling
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING_REVURDERING -> SelvstendigAvventerVilkårsprøvingRevurdering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_REVURDERING -> SelvstendigAvventerRevurdering

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
        result: EventSubscription.OverlappendeInfotrygdperioder,
        perioder: List<Infotrygdperiode>
    ): EventSubscription.OverlappendeInfotrygdperioder {
        val overlappende = perioder.filter { it.overlapperMed(this.periode) }
        if (overlappende.isEmpty()) return result
        return result.copy(
            overlappendeInfotrygdperioder = result.overlappendeInfotrygdperioder.plusElement(
                EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring(
                    yrkesaktivitetssporing = yrkesaktivitet.yrkesaktivitetstype,
                    vedtaksperiodeId = this.id,
                    kanForkastes = kanForkastes(),
                    vedtaksperiodeFom = this.periode.start,
                    vedtaksperiodeTom = this.periode.endInclusive,
                    vedtaksperiodetilstand = tilstand.type.name,
                    infotrygdperioder = overlappende.map {
                        when (it) {
                            is Friperiode -> EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
                                fom = it.periode.start,
                                tom = it.periode.endInclusive,
                                type = "FRIPERIODE",
                                orgnummer = null
                            )

                            is ArbeidsgiverUtbetalingsperiode -> EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
                                fom = it.periode.start,
                                tom = it.periode.endInclusive,
                                type = "ARBEIDSGIVERUTBETALING",
                                orgnummer = it.orgnr
                            )

                            is PersonUtbetalingsperiode -> EventSubscription.OverlappendeInfotrygdperiodeEtterInfotrygdendring.Infotrygdperiode(
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
            AvventerAvsluttetUtenUtbetaling -> VedtaksperiodetilstandDto.AVVENTER_AVSLUTTET_UTEN_UTBETALING
            AvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE
            AvventerSøknadForOverlappendePeriode -> VedtaksperiodetilstandDto.AVVENTER_SØKNAD_FOR_OVERLAPPENDE_PERIODE
            AvventerInntektsopplysningerForAnnenArbeidsgiver -> VedtaksperiodetilstandDto.AVVENTER_INNTEKTSOPPLYSNINGER_FOR_ANNEN_ARBEIDSGIVER
            AvventerRefusjonsopplysningerAnnenPeriode -> VedtaksperiodetilstandDto.AVVENTER_REFUSJONSOPPLYSNINGER_ANNEN_PERIODE
            AvventerGodkjenning -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING
            AvventerGodkjenningRevurdering -> VedtaksperiodetilstandDto.AVVENTER_GODKJENNING_REVURDERING
            AvventerHistorikk -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK
            AvventerHistorikkRevurdering -> VedtaksperiodetilstandDto.AVVENTER_HISTORIKK_REVURDERING
            AvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.AVVENTER_INFOTRYGDHISTORIKK
            AvventerInntektsmelding -> VedtaksperiodetilstandDto.AVVENTER_INNTEKTSMELDING
            AvventerRevurdering -> VedtaksperiodetilstandDto.AVVENTER_REVURDERING
            AvventerRevurderingTilUtbetaling -> VedtaksperiodetilstandDto.AVVENTER_REVURDERING_TIL_UTBETALING
            AvventerSimulering -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING
            AvventerSimuleringRevurdering -> VedtaksperiodetilstandDto.AVVENTER_SIMULERING_REVURDERING
            AvventerVilkårsprøving -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING
            AvventerVilkårsprøvingRevurdering -> VedtaksperiodetilstandDto.AVVENTER_VILKÅRSPRØVING_REVURDERING
            ArbeidstakerStart -> VedtaksperiodetilstandDto.ARBEIDSTAKER_START
            TilInfotrygd -> VedtaksperiodetilstandDto.TIL_INFOTRYGD
            TilUtbetaling -> VedtaksperiodetilstandDto.TIL_UTBETALING
            AvventerAnnullering -> VedtaksperiodetilstandDto.AVVENTER_ANNULLERING
            AvventerAnnulleringTilUtbetaling -> VedtaksperiodetilstandDto.AVVENTER_ANNULLERING_TIL_UTBETALING
            TilAnnullering -> VedtaksperiodetilstandDto.TIL_ANNULLERING

            FrilansStart -> VedtaksperiodetilstandDto.FRILANS_START
            FrilansAvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.FRILANS_AVVENTER_INFOTRYGDHISTORIKK
            FrilansAvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.FRILANS_AVVENTER_BLOKKERENDE_PERIODE

            ArbeidsledigStart -> VedtaksperiodetilstandDto.ARBEIDSLEDIG_START
            ArbeidsledigAvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.ARBEIDSLEDIG_AVVENTER_INFOTRYGDHISTORIKK
            ArbeidsledigAvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.ARBEIDSLEDIG_AVVENTER_BLOKKERENDE_PERIODE

            SelvstendigAvsluttet -> VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET
            SelvstendigAvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
            SelvstendigAvventerGodkjenning -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING
            SelvstendigAvventerHistorikk -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK
            SelvstendigAvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
            SelvstendigAvventerSimulering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING
            SelvstendigAvventerVilkårsprøving -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
            SelvstendigAvventerRevurdering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_REVURDERING
            SelvstendigStart -> VedtaksperiodetilstandDto.SELVSTENDIG_START
            SelvstendigTilUtbetaling -> VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING

            SelvstendigAvventerGodkjenningRevurdering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING_REVURDERING
            SelvstendigAvventerHistorikkRevurdering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK_REVURDERING
            SelvstendigAvventerSimuleringRevurdering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING_REVURDERING
            SelvstendigAvventerRevurderingTilUtbetaling -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_REVURDERING_TIL_UTBETALING
            SelvstendigAvventerVilkårsprøvingRevurdering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING_REVURDERING
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
        annulleringskandidater = yrkesaktivitet.finnAnnulleringskandidater(this.id).map { AnnulleringskandidatDto(it.id, it.yrkesaktivitet.organisasjonsnummer, it.periode.start, it.periode.endInclusive) }
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
    val annulleringskandidater: Set<Vedtaksperiode>
) {
    val sykdomstidslinje = behandlinger.behandlinger.last().endringer.last().sykdomstidslinje
    val refusjonstidslinje = behandlinger.behandlinger.last().endringer.last().refusjonstidslinje
    val dagerNavOvertarAnsvar = behandlinger.behandlinger.last().endringer.last().dagerNavOvertarAnsvar
}

internal val HendelseMetadata.behandlingkilde
    get() =
        Behandlingkilde(meldingsreferanseId, innsendt, registrert, avsender)

private fun sykdomsgradsubsummering(
    subsumsjonslogg: Subsumsjonslogg,
    periode: Periode,
    uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
    beregnetTidslinjePerVedtaksperiode: List<BeregnetPeriode>
) {
    val tidslinjerForSubsumsjon = uberegnetTidslinjePerArbeidsgiver.map { it.samletVedtaksperiodetidslinje }.subsumsjonsformat()

    val avvistePerioder = beregnetTidslinjePerVedtaksperiode
        .map { it.utbetalingstidslinje.subset(periode) }
        .flatMap { tidslinje ->
            tidslinje
                .filter { dag ->
                    dag is AvvistDag && MinimumSykdomsgrad in dag.begrunnelser
                }
        }
        .map { it.dato }
        .grupperSammenhengendePerioder()

    subsumsjonslogg.logg(`§ 8-13 ledd 2`(periode, tidslinjerForSubsumsjon, Prosentdel.GRENSE.toDouble(), avvistePerioder))
    `§ 8-13 ledd 1`(periode, avvistePerioder, tidslinjerForSubsumsjon).forEach {
        subsumsjonslogg.logg(it)
    }
}

private fun maksdatosubsummering(
    subsumsjonslogg: Subsumsjonslogg,
    periode: Periode,
    syttiårsdagen: LocalDate,
    uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
    historisktidslinje: Utbetalingstidslinje,
    resultat: Maksdatoresultat
) {
    val tidslinjegrunnlag = uberegnetTidslinjePerArbeidsgiver.map { it.samletVedtaksperiodetidslinje }.plusElement(historisktidslinje)
    val tidslinjegrunnlagsubsumsjon = tidslinjegrunnlag.subsumsjonsformat()
    val beregnetTidslinjesubsumsjon = tidslinjegrunnlag.reduce(Utbetalingstidslinje::plus).subsumsjonsformat()

    subsumsjonslogg.logg(
        `§ 8-12 ledd 2`(
            oppfylt = resultat.startdatoSykepengerettighet != null,
            dato = resultat.vurdertTilOgMed,
            tilstrekkeligOppholdISykedager = TILSTREKKELIG_OPPHOLD_I_SYKEDAGER,
            tidslinjegrunnlag = tidslinjegrunnlagsubsumsjon,
            beregnetTidslinje = beregnetTidslinjesubsumsjon,
        )
    )

    val førSyttiårsdagen = fun(subsumsjonslogg: Subsumsjonslogg, utfallTom: LocalDate) {
        subsumsjonslogg.logg(
            `§ 8-3 ledd 1 punktum 2`(
                oppfylt = true,
                syttiårsdagen = syttiårsdagen,
                utfallFom = periode.start,
                utfallTom = utfallTom,
                tidslinjeFom = periode.start,
                tidslinjeTom = periode.endInclusive,
                avvistePerioder = emptyList()
            )
        )
    }

    when (resultat.bestemmelse) {
        Maksdatoresultat.Bestemmelse.IKKE_VURDERT -> error("ugyldig situasjon ${resultat.bestemmelse}")
        Maksdatoresultat.Bestemmelse.ORDINÆR_RETT -> {
            `§ 8-12 ledd 1 punktum 1`(periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, resultat.gjenståendeDager, resultat.antallForbrukteDager, resultat.maksdato, resultat.startdatoSykepengerettighet ?: LocalDate.MIN).forEach {
                subsumsjonslogg.logg(it)
            }
            førSyttiårsdagen(subsumsjonslogg, periode.endInclusive)
        }

        Maksdatoresultat.Bestemmelse.BEGRENSET_RETT -> {
            `§ 8-51 ledd 3`(periode, tidslinjegrunnlagsubsumsjon, beregnetTidslinjesubsumsjon, resultat.gjenståendeDager, resultat.antallForbrukteDager, resultat.maksdato, resultat.startdatoSykepengerettighet ?: LocalDate.MIN).forEach {
                subsumsjonslogg.logg(it)
            }
            førSyttiårsdagen(subsumsjonslogg, syttiårsdagen.forrigeDag)
        }

        Maksdatoresultat.Bestemmelse.SYTTI_ÅR -> {
            if (periode.start < syttiårsdagen) {
                førSyttiårsdagen(subsumsjonslogg, syttiårsdagen.forrigeDag)
            }

            val avvisteDagerFraOgMedSøtti = resultat.avslåtteDager.flatten().filter { it >= syttiårsdagen }
            if (avvisteDagerFraOgMedSøtti.isNotEmpty()) {
                subsumsjonslogg.logg(
                    `§ 8-3 ledd 1 punktum 2`(
                        oppfylt = false,
                        syttiårsdagen = syttiårsdagen,
                        utfallFom = maxOf(syttiårsdagen, periode.start),
                        utfallTom = periode.endInclusive,
                        tidslinjeFom = periode.start,
                        tidslinjeTom = periode.endInclusive,
                        avvistePerioder = avvisteDagerFraOgMedSøtti.grupperSammenhengendePerioder()
                    )
                )
            }
        }
    }
}

internal fun lagArbeidsgiverberegning(
    beregningsperiode: Periode,
    vedtaksperioder: List<Vedtaksperiode>,
    vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement? = null,
    inntektsperioder: Map<Arbeidsgiverberegning.Inntektskilde, Beløpstidslinje> = emptyMap(),
    forsikring: Forsikring? = null,
): List<Arbeidsgiverberegning> {
    return with(ArbeidsgiverberegningBuilder(beregningsperiode)) {
        vilkårsgrunnlag?.inntektsgrunnlag?.arbeidsgiverInntektsopplysninger?.forEach {
            fastsattÅrsinntekt(Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker(it.orgnummer), it.fastsattÅrsinntekt)
        }
        vilkårsgrunnlag?.inntektsgrunnlag?.deaktiverteArbeidsforhold?.forEach {
            fastsattÅrsinntekt(Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker(it.orgnummer), INGEN)
        }
        vilkårsgrunnlag?.inntektsgrunnlag?.selvstendigInntektsopplysning?.also {
            selvstendigNæringsdrivende(it.fastsattÅrsinntekt)
        }
        vilkårsgrunnlag?.inntektsgrunnlag?.sykepengegrunnlag?.also {
            sykepengegrunnlag(it)
        }
        inntektsperioder.forEach { (inntektskilde, inntektsjusteringer) ->
            inntektsjusteringer(inntektskilde, inntektsjusteringer)
        }
        vedtaksperioder.forEach { it.medVedtaksperiode(this, forsikring) }
        build()
    }
}

private fun Vedtaksperiode.medVedtaksperiode(builder: ArbeidsgiverberegningBuilder, forsikring: Forsikring?) {
    val yrkesaktivitetstype = when (yrkesaktivitet.yrkesaktivitetstype) {
        Arbeidsledig -> Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidsledig
        is Arbeidstaker -> Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Arbeidstaker(yrkesaktivitet.yrkesaktivitetstype.organisasjonsnummer)
        Frilans -> Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Frilans
        Selvstendig -> Arbeidsgiverberegning.Inntektskilde.Yrkesaktivitet.Selvstendig
    }
    val utbetalingstidslinjeBuilder = when (yrkesaktivitet.yrkesaktivitetstype) {
        is Arbeidstaker -> behandlinger.utbetalingstidslinjeBuilderForArbeidstaker()
        Selvstendig -> behandlinger.utbetalingstidslinjeBuilderForSelvstendig(forsikring)

        Arbeidsledig,
        Frilans -> error("Forventer ikke å lage utbetalingstidslinje for ${yrkesaktivitet.yrkesaktivitetstype::class.simpleName}")
    }
    builder.vedtaksperiode(yrkesaktivitetstype, id, sykdomstidslinje, utbetalingstidslinjeBuilder)
}

private fun nesteTilstandEtterIgangsattOverstyring(
    infotrygdhistorikk: Infotrygdhistorikk,
    vedtaksperiode: Vedtaksperiode,
    tilstand: Vedtaksperiodetilstand
) = when (tilstand) {
    SelvstendigStart -> when {
        !infotrygdhistorikk.harHistorikk() -> SelvstendigAvventerInfotrygdHistorikk
        else -> SelvstendigAvventerBlokkerendePeriode
    }

    ArbeidstakerStart -> when {
        !infotrygdhistorikk.harHistorikk() -> AvventerInfotrygdHistorikk
        else -> AvventerInntektsmelding
    }

    ArbeidsledigStart -> when {
        !infotrygdhistorikk.harHistorikk() -> ArbeidsledigAvventerInfotrygdHistorikk
        else -> ArbeidsledigAvventerBlokkerendePeriode
    }

    FrilansStart -> when {
        !infotrygdhistorikk.harHistorikk() -> FrilansAvventerInfotrygdHistorikk
        else -> FrilansAvventerBlokkerendePeriode
    }

    SelvstendigAvventerBlokkerendePeriode,
    SelvstendigAvventerGodkjenning,
    SelvstendigAvventerHistorikk,
    SelvstendigAvventerSimulering,
    SelvstendigAvventerVilkårsprøving -> {
        SelvstendigAvventerBlokkerendePeriode
    }

    SelvstendigAvsluttet,
    SelvstendigAvventerGodkjenningRevurdering,
    SelvstendigAvventerHistorikkRevurdering,
    SelvstendigAvventerSimuleringRevurdering,
    SelvstendigAvventerVilkårsprøvingRevurdering,
    SelvstendigAvventerRevurdering -> {
        SelvstendigAvventerRevurdering
    }

    SelvstendigTilUtbetaling,
    SelvstendigAvventerRevurderingTilUtbetaling -> SelvstendigAvventerRevurderingTilUtbetaling

    TilUtbetaling,
    AvventerRevurderingTilUtbetaling -> AvventerRevurderingTilUtbetaling

    Avsluttet,
    AvventerGodkjenningRevurdering,
    AvventerHistorikkRevurdering,
    AvventerSimuleringRevurdering,
    AvventerVilkårsprøvingRevurdering,
    AvventerRevurdering -> {
        AvventerRevurdering
    }

    ArbeidsledigAvventerBlokkerendePeriode -> ArbeidsledigAvventerBlokkerendePeriode

    FrilansAvventerBlokkerendePeriode -> FrilansAvventerBlokkerendePeriode

    AvventerAnnullering,
    AvventerInfotrygdHistorikk,
    FrilansAvventerInfotrygdHistorikk,
    ArbeidsledigAvventerInfotrygdHistorikk,
    TilAnnullering,
    AvventerAnnulleringTilUtbetaling,
    SelvstendigAvventerInfotrygdHistorikk -> tilstand

    AvventerAvsluttetUtenUtbetaling,
    AvsluttetUtenUtbetaling,
    AvventerInntektsmelding -> when {
        vedtaksperiode.skalArbeidstakerBehandlesISpeil() -> when {
            vedtaksperiode.harInntektOgRefusjon() -> nesteTilstandEtterInntekt(vedtaksperiode)
            else -> AvventerInntektsmelding
        }

        else -> AvventerAvsluttetUtenUtbetaling
    }

    AvventerBlokkerendePeriode,
    AvventerSøknadForOverlappendePeriode,
    AvventerInntektsopplysningerForAnnenArbeidsgiver,
    AvventerRefusjonsopplysningerAnnenPeriode,
    AvventerGodkjenning,
    AvventerHistorikk,
    AvventerSimulering,
    AvventerVilkårsprøving -> when {
        vedtaksperiode.skalArbeidstakerBehandlesISpeil() -> nesteTilstandEtterInntekt(vedtaksperiode)
        else -> AvventerAvsluttetUtenUtbetaling
    }

    TilInfotrygd -> error("Revurdering håndteres av en periode i til_infotrygd")
}
