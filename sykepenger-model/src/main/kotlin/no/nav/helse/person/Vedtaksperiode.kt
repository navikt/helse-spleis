package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.Grunnbeløp.Companion.`1G`
import no.nav.helse.dto.LazyVedtaksperiodeVenterDto
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
import no.nav.helse.person.Behandlinger.Behandlingkilde
import no.nav.helse.person.Behandlinger.Companion.berik
import no.nav.helse.person.Dokumentsporing.Companion.inntektFraAOrdingen
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingDager
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingInntekt
import no.nav.helse.person.Dokumentsporing.Companion.inntektsmeldingRefusjon
import no.nav.helse.person.Dokumentsporing.Companion.overstyrTidslinje
import no.nav.helse.person.Dokumentsporing.Companion.søknad
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
import no.nav.helse.person.aktivitetslogg.Aktivitetslogg
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
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Skjæringstidspunkt
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.AvvisDagerEtterDødsdatofilter
import no.nav.helse.utbetalingstidslinje.AvvisInngangsvilkårfilter
import no.nav.helse.utbetalingstidslinje.BeregnetPeriode
import no.nav.helse.utbetalingstidslinje.Maksdatoresultat
import no.nav.helse.utbetalingstidslinje.Maksdatovurdering
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter
import no.nav.helse.utbetalingstidslinje.MaksimumUtbetalingFilter
import no.nav.helse.utbetalingstidslinje.Sykdomsgradfilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinjesubsumsjon
import no.nav.helse.utbetalingstidslinje.Vedtaksperiodeberegning
import no.nav.helse.yearMonth
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Prosentdel.Companion.prosent

internal class Vedtaksperiode private constructor(
    internal val person: Person,
    internal val arbeidsgiver: Arbeidsgiver,
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
        aktivitetslogg: IAktivitetslogg,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        sykdomstidslinje: Sykdomstidslinje,
        dokumentsporing: Dokumentsporing,
        sykmeldingsperiode: Periode,
        faktaavklartInntekt: SelvstendigFaktaavklartInntekt?,
        inntektsendringer: Beløpstidslinje = Beløpstidslinje(),
        venteperiode: Periode?,
        regelverkslogg: Regelverkslogg
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = UUID.randomUUID(),
        tilstand = when (arbeidsgiver.yrkesaktivitetssporing) {
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigStart
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            is Arbeidstaker,
            Behandlingsporing.Yrkesaktivitet.Frilans -> Start
        },
        behandlinger = Behandlinger(),
        opprettet = LocalDateTime.now(),
        regelverkslogg = regelverkslogg
    ) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val periode = checkNotNull(sykdomstidslinje.periode()) { "sykdomstidslinjen er tom" }
        person.vedtaksperiodeOpprettet(id, arbeidsgiver.yrkesaktivitetssporing, periode, periode.start, opprettet)
        behandlinger.initiellBehandling(sykmeldingsperiode, sykdomstidslinje, egenmeldingsperioder, faktaavklartInntekt, inntektsendringer, venteperiode, dokumentsporing, metadata.behandlingkilde)
    }

    private val sykmeldingsperiode get() = behandlinger.sykmeldingsperiode()
    internal val periode get() = behandlinger.periode()
    internal val sykdomstidslinje get() = behandlinger.sykdomstidslinje()
    internal val subsumsjonslogg get() = behandlinger.subsumsjonslogg(regelverkslogg, id, person.fødselsnummer, arbeidsgiver.organisasjonsnummer)
    internal val skjæringstidspunkt get() = behandlinger.skjæringstidspunkt()
    internal val førsteFraværsdag get() = arbeidsgiver.finnFørsteFraværsdag(this.periode)

    // 💡Må ikke forveksles med `førsteFraværsdag` 💡
    // F.eks. januar med agp 1-10 & 16-21 så er `førsteFraværsdag` 16.januar, mens `startdatoPåSammenhengendeVedtaksperioder` er 1.januar
    private val startdatoPåSammenhengendeVedtaksperioder
        get() = arbeidsgiver.startdatoPåSammenhengendeVedtaksperioder(
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
        skalBehandlesISpeil = skalBehandlesISpeil()
    )

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun håndter(sykmelding: Sykmelding) {
        sykmelding.trimLeft(periode.endInclusive)
    }

    private fun validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        check(tilstand != Start || tilstand != SelvstendigStart) { "en vedtaksperiode blir stående i Start-tilstanden" }
        if (!tilstand.erFerdigBehandlet) return
        behandlinger.validerFerdigBehandlet(hendelse.metadata.meldingsreferanseId, aktivitetslogg)
    }

    internal fun håndterSøknadFørsteGang(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        arbeidsgivere: List<Arbeidsgiver>,
        infotrygdhistorikk: Infotrygdhistorikk
    ): Revurderingseventyr {
        check(tilstand is Start || tilstand is SelvstendigStart) { "Kan ikke håndtere søknad i tilstand $tilstand" }
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        person.emitSøknadHåndtert(søknad.metadata.meldingsreferanseId.id, id, arbeidsgiver.organisasjonsnummer)
        søknad.forUng(aktivitetsloggMedVedtaksperiodekontekst, person.alder)
        arbeidsgiver.vurderOmSøknadIkkeKanHåndteres(aktivitetsloggMedVedtaksperiodekontekst, periode, arbeidsgivere)

        infotrygdhistorikk.validerMedFunksjonellFeil(aktivitetsloggMedVedtaksperiodekontekst, periode)
        håndterSøknad(søknad, aktivitetsloggMedVedtaksperiodekontekst)
        aktivitetsloggMedVedtaksperiodekontekst.info("Fullført behandling av søknad")

        if (aktivitetsloggMedVedtaksperiodekontekst.harFunksjonelleFeilEllerVerre()) forkast(søknad, aktivitetslogg)
        return Revurderingseventyr.nyPeriode(søknad, skjæringstidspunkt, behandlinger.egenmeldingsdager().plusElement(periode).periode()!!)
    }

    internal fun håndterKorrigertSøknad(søknad: Søknad, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!søknad.erRelevant(this.periode)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        person.emitSøknadHåndtert(søknad.metadata.meldingsreferanseId.id, id, arbeidsgiver.organisasjonsnummer)

        when (tilstand) {
            AvventerBlokkerendePeriode,
            AvventerInfotrygdHistorikk,
            AvventerInntektsmelding,
            AvventerGodkjenning,
            AvventerHistorikk,
            AvventerSimulering,
            AvventerVilkårsprøving -> {
                val nesteTilstand = when (tilstand) {
                    AvventerBlokkerendePeriode,
                    AvventerInfotrygdHistorikk,
                    AvventerInntektsmelding -> null

                    else -> AvventerBlokkerendePeriode
                }
                håndterOverlappendeSøknad(søknad, aktivitetsloggMedVedtaksperiodekontekst, nesteTilstand)
            }

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
            TilInfotrygd -> error("Kan ikke håndtere søknad mens perioden er i $tilstand")

            SelvstendigStart,
            SelvstendigTilInfotrygd -> error("Kan ikke håndtere søknad mens perioden er i $tilstand")

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigTilUtbetaling -> {
                val nesteTilstand = when (tilstand) {
                    SelvstendigAvventerBlokkerendePeriode,
                    SelvstendigAvventerInfotrygdHistorikk -> null

                    else -> SelvstendigAvventerBlokkerendePeriode
                }
                håndterOverlappendeSøknad(søknad, aktivitetsloggMedVedtaksperiodekontekst, nesteTilstand)
            }
        }
        if (aktivitetsloggMedVedtaksperiodekontekst.harFunksjonelleFeilEllerVerre()) forkast(søknad, aktivitetsloggMedVedtaksperiodekontekst)
        return Revurderingseventyr.korrigertSøknad(søknad, skjæringstidspunkt, periode)
    }

    internal fun håndter(hendelse: OverstyrTidslinje, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!hendelse.erRelevant(this.periode)) {
            hendelse.vurdertTilOgMed(periode.endInclusive)
            return null
        }
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val overstyring = when (tilstand) {
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
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
            TilUtbetaling -> {
                val dagerNavOvertarAnsvar = behandlinger.dagerNavOvertarAnsvar
                oppdaterHistorikk(hendelse.metadata.behandlingkilde, overstyrTidslinje(hendelse.metadata.meldingsreferanseId), hendelse.sykdomstidslinje, aktivitetsloggMedVedtaksperiodekontekst, hendelse.dagerNavOvertarAnsvar(dagerNavOvertarAnsvar)) {
                    // ingen validering å gjøre :(
                }
                aktivitetsloggMedVedtaksperiodekontekst.info("Igangsetter overstyring av tidslinje")

                Revurderingseventyr.sykdomstidslinje(hendelse, this.skjæringstidspunkt, this.periode)
            }

            RevurderingFeilet,
            Start,
            TilInfotrygd -> error("Kan ikke overstyre tidslinjen i $tilstand")

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilInfotrygd,
            SelvstendigTilUtbetaling -> error("Kan ikke overstyre tidslinjen i $tilstand")
        }

        hendelse.vurdertTilOgMed(periode.endInclusive)
        return overstyring
    }

    internal fun håndter(anmodningOmForkasting: AnmodningOmForkasting, aktivitetslogg: IAktivitetslogg) {
        if (!anmodningOmForkasting.erRelevant(id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        aktivitetsloggMedVedtaksperiodekontekst.info("Behandler anmodning om forkasting")
        when (tilstand) {
            AvventerInntektsmelding,
            AvventerBlokkerendePeriode,
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
            TilUtbetaling -> {
                if (anmodningOmForkasting.force) return forkast(anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst)
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
            SelvstendigTilInfotrygd,
            SelvstendigTilUtbetaling -> {
                if (anmodningOmForkasting.force) return forkast(anmodningOmForkasting, aktivitetsloggMedVedtaksperiodekontekst)
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

        aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_IM_4)
        return Revurderingseventyr.korrigertInntektsmeldingInntektsopplysninger(inntektsmelding, skjæringstidspunkt, skjæringstidspunkt)
    }

    private fun inntektsmeldingHåndtert(inntektsmelding: Inntektsmelding) {
        inntektsmelding.inntektHåndtert()
        behandlinger.oppdaterDokumentsporing(inntektsmelding.dokumentsporing)
        person.emitInntektsmeldingHåndtert(
            meldingsreferanseId = inntektsmelding.metadata.meldingsreferanseId.id,
            vedtaksperiodeId = id,
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer
        )
    }

    private fun oppdaterVilkårsgrunnlagMedInntekt(korrigertInntekt: ArbeidstakerFaktaavklartInntekt): Boolean {
        val grunnlag = vilkårsgrunnlag ?: return false
        /* fest setebeltet. nå skal vi prøve å endre vilkårsgrunnlaget */
        val resultat = grunnlag.nyeArbeidsgiverInntektsopplysninger(
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
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
                    .mapNotNull { arbeidsgiver.håndter(it, aktivitetsloggMedVedtaksperiodekontekst, trengerRefusjonsopplysninger) }
                    .tidligsteEventyr()
            }

            AvventerBlokkerendePeriode,
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
            SelvstendigTilInfotrygd,
            SelvstendigTilUtbetaling -> error("Kan ikke håndtere replay av inntektsmelding i en selvstendigtilstand: $tilstand")
        }
        return null
    }

    internal fun håndter(replays: InntektsmeldingerReplay, aktivitetslogg: IAktivitetslogg) {
        if (!replays.erRelevant(this.id)) return
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.replayUtført(this, replays, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun inntektsmeldingFerdigbehandlet(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.inntektsmeldingFerdigbehandlet(this, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    private fun håndterArbeidsgiveropplysninger(eventyr: List<List<Revurderingseventyr>>, hendelse: Hendelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        person.emitInntektsmeldingHåndtert(hendelse.metadata.meldingsreferanseId.id, id, arbeidsgiver.organisasjonsnummer)
        val tidligsteEventyr = eventyr.flatten().tidligsteEventyr()
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) forkast(hendelse, aktivitetslogg)
        return tidligsteEventyr
    }

    internal fun håndter(arbeidsgiveropplysninger: Arbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, vedtaksperioder: List<Vedtaksperiode>, inntektshistorikk: Inntektshistorikk, ubrukteRefusjonsopplysninger: Refusjonsservitør): Revurderingseventyr? {
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

    internal fun håndter(korrigerteArbeidsgiveropplysninger: KorrigerteArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg, vedtaksperioder: List<Vedtaksperiode>, inntektshistorikk: Inntektshistorikk, ubrukteRefusjonsopplysninger: Refusjonsservitør): Revurderingseventyr? {
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
            acc.håndter(vedtaksperiode.periode)
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

        fun håndter(vedtaksperiode: Periode) = this.copy(
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
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
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
        val perioderNavUtbetaler = behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.flatMap { it.uten(LocalDate.MIN til utbetaltDelerAvArbeidsgiverperioden.utbetaltTilOgMed) }
        return håndterNavUtbetalerArbeidsgiverperiode(aktivitetslogg, arbeidsgiveropplysninger, perioderNavUtbetaler = perioderNavUtbetaler) {
            utbetaltDelerAvArbeidsgiverperioden.valider(aktivitetslogg)
        }
    }

    private fun håndterNavUtbetalerArbeidsgiverperiode(
        aktivitetslogg: IAktivitetslogg,
        arbeidsgiveropplysninger: Arbeidsgiveropplysninger,
        perioderNavUtbetaler: List<Periode> = behandlinger.arbeidsgiverperiode().arbeidsgiverperioder,
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
        val beregnetArbeidsgiverperiode = behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.periode() ?: return varselFraArbeidsgiveropplysning(korrigerteArbeidsgiveropplysninger, aktivitetslogg, RV_IM_24)
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
            arbeidsgiver = arbeidsgiver,
            behandlingkilde = hendelse.metadata.behandlingkilde,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = arbeidsgiver.beregnArbeidsgiverperiode()
        )
        behandlinger.oppdaterDokumentsporing(dokumentsporing(hendelse.metadata.meldingsreferanseId))
    }

    private fun <T> varselFraArbeidsgiveropplysning(hendelse: T, aktivitetslogg: IAktivitetslogg, varselkode: Varselkode) where T : Hendelse, T : Collection<Arbeidsgiveropplysning> {
        dokumentsporingFraArbeidsgiveropplysning(hendelse, ::inntektsmeldingDager)
        aktivitetslogg.varsel(varselkode)
    }

    internal fun håndter(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!tilstand.skalHåndtereDager(this, dager, aktivitetsloggMedVedtaksperiodekontekst) || dager.alleredeHåndtert(behandlinger))
            return dager.vurdertTilOgMed(periode.endInclusive)
        tilstand.håndter(this, dager, aktivitetsloggMedVedtaksperiodekontekst)
        dager.vurdertTilOgMed(periode.endInclusive)
    }

    internal fun skalHåndtereDagerRevurdering(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg): Boolean {
        return skalHåndtereDager(dager, aktivitetslogg) { sammenhengende ->
            dager.skalHåndteresAvRevurdering(periode, sammenhengende, behandlinger.arbeidsgiverperiode().arbeidsgiverperioder)
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
        val sammenhengende = arbeidsgiver.finnSammenhengendeVedtaksperioder(this)
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
            dager.validerArbeidsgiverperiode(aktivitetslogg, periode, behandlinger.arbeidsgiverperiode().arbeidsgiverperioder)
        }
    }

    private fun håndterDagerUtenEndring(dager: DagerFraInntektsmelding, aktivitetslogg: IAktivitetslogg) {
        val hendelse = dager.tomBitAvInntektsmelding(aktivitetslogg, periode)
        håndterDager(dager.hendelse, hendelse, aktivitetslogg) {
            dager.valider(aktivitetslogg, behandlinger.arbeidsgiverperiode().arbeidsgiverperioder, vedtaksperiodeId = id)
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
            AvventerInfotrygdHistorikk -> when (arbeidsgiver.yrkesaktivitetssporing) {
                is Arbeidstaker -> tilstand(aktivitetsloggMedVedtaksperiodekontekst, AvventerInntektsmelding)
                Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
                Behandlingsporing.Yrkesaktivitet.Frilans -> tilstand(aktivitetsloggMedVedtaksperiodekontekst, AvventerBlokkerendePeriode)

                Behandlingsporing.Yrkesaktivitet.Selvstendig ->
                    error("Forventer ikke tilstanden AvventerInfotrygdHistorikk for vedtaksperiode opprettet av en søknad for Selvstendig næringsdrivende")
            }

            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
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
            SelvstendigTilInfotrygd,
            SelvstendigTilUtbetaling -> {
                /* gjør ingenting */
            }

            SelvstendigAvventerInfotrygdHistorikk -> when (val yrkesaktivitet = arbeidsgiver.yrkesaktivitetssporing) {
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

    private fun håndterYtelser(ytelser: Ytelser, aktivitetslogg: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk) {
        val inntekterForBeregningBuilder = InntekterForBeregning.Builder(beregningsperiode).apply {
            ytelser.inntektsendringer(this)
        }

        val maksdatoresultat = beregnUtbetalinger(aktivitetslogg, inntekterForBeregningBuilder)

        when (arbeidsgiver.yrkesaktivitetssporing) {
            is Arbeidstaker -> {
                checkNotNull(vilkårsgrunnlag).valider(aktivitetslogg, arbeidsgiver.organisasjonsnummer)
                checkNotNull(vilkårsgrunnlag).opptjening?.validerOpptjeningsdager(aktivitetslogg)
            }

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans,
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> {
            }
        }

        infotrygdhistorikk.validerMedVarsel(aktivitetslogg, periode)
        infotrygdhistorikk.validerNyereOpplysninger(aktivitetslogg, periode)
        ytelser.valider(aktivitetslogg, periode, skjæringstidspunkt, maksdatoresultat.maksdato, erForlengelse())

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

    internal fun håndter(utbetalingsavgjørelse: Behandlingsavgjørelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!utbetalingsavgjørelse.relevantVedtaksperiode(id)) return
        if (behandlinger.gjelderIkkeFor(utbetalingsavgjørelse)) return aktivitetsloggMedVedtaksperiodekontekst.info("Ignorerer løsning på utbetalingsavgjørelse, utbetalingid på løsningen matcher ikke vedtaksperiodens nåværende utbetaling")

        if (tilstand !in setOf(AvventerGodkjenning, AvventerGodkjenningRevurdering, SelvstendigAvventerGodkjenning)) return aktivitetsloggMedVedtaksperiodekontekst.info("Forventet ikke utbetalingsavgjørelse i %s".format(tilstand.type.name))

        val erAvvist = behandlinger.erAvvist()
        if (erAvvist) {
            if (arbeidsgiver.kanForkastes(this, aktivitetsloggMedVedtaksperiodekontekst)) return forkast(utbetalingsavgjørelse, aktivitetsloggMedVedtaksperiodekontekst)
            if (utbetalingsavgjørelse.automatisert) aktivitetsloggMedVedtaksperiodekontekst.info("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
            aktivitetsloggMedVedtaksperiodekontekst.varsel(RV_UT_24)
        }

        behandlinger.vedtakFattet(arbeidsgiver, utbetalingsavgjørelse, aktivitetsloggMedVedtaksperiodekontekst)

        if (erAvvist) return // er i limbo
        tilstand(
            aktivitetsloggMedVedtaksperiodekontekst,
            when {
                behandlinger.harUtbetalinger() && arbeidsgiver.yrkesaktivitetssporing == Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigTilUtbetaling
                behandlinger.harUtbetalinger() -> TilUtbetaling
                arbeidsgiver.yrkesaktivitetssporing == Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigAvsluttet
                else -> Avsluttet
            }
        )
    }

    internal fun håndter(sykepengegrunnlagForArbeidsgiver: SykepengegrunnlagForArbeidsgiver, aktivitetslogg: IAktivitetslogg): Boolean {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        return when (tilstand) {
            AvventerInntektsmelding -> {
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
            TilUtbetaling,

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilInfotrygd,
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
            arbeidsgiver = arbeidsgiver,
            behandlingkilde = sykepengegrunnlagForArbeidsgiver.metadata.behandlingkilde,
            dokumentsporing = inntektFraAOrdingen(sykepengegrunnlagForArbeidsgiver.metadata.meldingsreferanseId),
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = arbeidsgiver.beregnArbeidsgiverperiode(),
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

        arbeidsgiver.lagreInntektFraAOrdningen(
            meldingsreferanseId = sykepengegrunnlagForArbeidsgiver.metadata.meldingsreferanseId,
            skjæringstidspunkt = skjæringstidspunkt,
            omregnetÅrsinntekt = omregnetÅrsinntekt
        )

        videreførEllerIngenRefusjon(sykepengegrunnlagForArbeidsgiver, aktivitetslogg)

        val event = PersonObserver.SkatteinntekterLagtTilGrunnEvent(
            yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, aktivitetslogg: IAktivitetslogg) {
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

    internal fun håndter(simulering: Simulering, aktivitetslogg: IAktivitetslogg) {
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

    internal fun håndter(hendelse: UtbetalingHendelse, aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!behandlinger.håndterUtbetalinghendelse(hendelse, aktivitetsloggMedVedtaksperiodekontekst)) return
        tilstand.håndter(this, hendelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndter(
        hendelse: AnnullerUtbetaling,
        aktivitetslogg: IAktivitetslogg,
        vedtaksperioder: List<Vedtaksperiode>
    ): Revurderingseventyr? {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        val annullering = behandlinger.håndterAnnullering(
            arbeidsgiver,
            hendelse,
            hendelse.metadata.behandlingkilde,
            aktivitetsloggMedVedtaksperiodekontekst,
            vedtaksperioder.map { it.behandlinger }) ?: return null
        aktivitetsloggMedVedtaksperiodekontekst.info("Forkaster denne, og senere perioder, som følge av annullering.")
        forkast(hendelse, aktivitetsloggMedVedtaksperiodekontekst)
        return Revurderingseventyr.annullering(hendelse, annullering.periode())
    }

    internal fun håndter(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!påminnelse.erRelevant(id)) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        return tilstand.påminnelse(this, påminnelse, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun nyAnnullering(aktivitetslogg: IAktivitetslogg) {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        tilstand.nyAnnullering(this, aktivitetsloggMedVedtaksperiodekontekst)
    }

    internal fun håndter(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!overstyrArbeidsgiveropplysninger.erRelevant(skjæringstidspunkt)) return null
        if (vilkårsgrunnlag?.erArbeidsgiverRelevant(arbeidsgiver.organisasjonsnummer) != true) return null
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)

        val grunnlag = vilkårsgrunnlag ?: return null
        val (nyttGrunnlag, endretInntektsgrunnlag) = grunnlag.overstyrArbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger, subsumsjonslogg) ?: return null
        person.lagreVilkårsgrunnlag(nyttGrunnlag)

        endretInntektsgrunnlag.inntekter
            .forEach {
                val opptjeningFom = nyttGrunnlag.opptjening!!.startdatoFor(it.inntektEtter.orgnummer)
                overstyrArbeidsgiveropplysninger.subsummer(subsumsjonslogg, opptjeningFom, it.inntektEtter.orgnummer)
            }

        val eventyr = Revurderingseventyr.arbeidsgiveropplysninger(overstyrArbeidsgiveropplysninger, skjæringstidspunkt, skjæringstidspunkt)
        return eventyr
    }

    internal fun håndter(overstyrInntektsgrunnlag: OverstyrInntektsgrunnlag, aktivitetslogg: IAktivitetslogg): Revurderingseventyr? {
        if (!overstyrInntektsgrunnlag.erRelevant(skjæringstidspunkt)) return null
        val grunnlag = vilkårsgrunnlag ?: return null
        if (grunnlag.erArbeidsgiverRelevant(arbeidsgiver.organisasjonsnummer) != true) return null
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
                arbeidsgiver,
                hendelse.metadata.behandlingkilde,
                dokumentsporing,
                aktivitetsloggMedVedtaksperiodekontekst,
                person.beregnSkjæringstidspunkt(),
                arbeidsgiver.beregnArbeidsgiverperiode(),
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

    private fun erForlengelse(): Boolean = arbeidsgiver
        .finnVedtaksperiodeRettFør(this)
        ?.takeIf { it.skalBehandlesISpeil() } != null

    internal fun manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(): Boolean {
        return vilkårsgrunnlag?.harNødvendigInntektForVilkårsprøving(arbeidsgiver.organisasjonsnummer) == false
    }

    internal fun kanForkastes(vedtaksperioder: List<Vedtaksperiode>, aktivitetslogg: IAktivitetslogg, hendelse: Hendelse? = null): Boolean {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (behandlinger.kanForkastes(vedtaksperioder.map { it.behandlinger })) {
            aktivitetsloggMedVedtaksperiodekontekst.info("Kan forkastes fordi evt. overlappende utbetalinger er annullerte/forkastet")
            return true
        }
        if (hendelse is AnmodningOmForkasting && hendelse.force) {
            aktivitetsloggMedVedtaksperiodekontekst.info("Behandlingene sier at denne _ikke_ kan forkastes. Men ettersom 'force'-flagget i anmodningen er satt forkastes perioden læll. Ta en god titt på at det ikke blir hengende noen utbetalinger her!")
            return true
        }
        aktivitetsloggMedVedtaksperiodekontekst.info("Kan ikke forkastes fordi behandlinger nekter det")
        return false
    }

    internal fun forkast(
        hendelse: Hendelse,
        aktivitetslogg: IAktivitetslogg,
        vedtaksperioder: List<Vedtaksperiode>
    ): VedtaksperiodeForkastetEventBuilder? {
        val aktivitetsloggMedVedtaksperiodekontekst = registrerKontekst(aktivitetslogg)
        if (!kanForkastes(vedtaksperioder, aktivitetsloggMedVedtaksperiodekontekst, hendelse)) return null
        aktivitetsloggMedVedtaksperiodekontekst.info("Forkaster vedtaksperiode: %s", this.id.toString())
        this.behandlinger.forkast(arbeidsgiver, hendelse.metadata.behandlingkilde, hendelse.metadata.automatiskBehandling, aktivitetsloggMedVedtaksperiodekontekst)
        val vedtaksperiodeForkastetEventBuilder = when (tilstand) {
            // Vedtaksperioder i disse tilstandene har rukket å sende ut egne forespørsler før de ble forkastet
            Avsluttet,
            AvsluttetUtenUtbetaling,
            AvventerBlokkerendePeriode,
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
            TilUtbetaling,

            SelvstendigAvsluttet,
            SelvstendigAvventerBlokkerendePeriode,
            SelvstendigAvventerGodkjenning,
            SelvstendigAvventerHistorikk,
            SelvstendigAvventerInfotrygdHistorikk,
            SelvstendigAvventerSimulering,
            SelvstendigAvventerVilkårsprøving,
            SelvstendigStart,
            SelvstendigTilInfotrygd,
            SelvstendigTilUtbetaling -> VedtaksperiodeForkastetEventBuilder()

            AvventerInfotrygdHistorikk,
            Start -> {
                VedtaksperiodeForkastetEventBuilder().apply {
                    arbeidsgiver.trengerArbeidsgiveropplysninger(periode, ::trengerArbeidsgiveropplysninger)
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
                    yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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

    internal fun forkast(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) {
        person.søppelbøtte(hendelse, aktivitetslogg, OVERLAPPENDE_OG_ETTERGØLGENDE(this))
    }

    private fun registrerKontekst(aktivitetslogg: IAktivitetslogg): IAktivitetslogg {
        return aktivitetslogg.kontekst(arbeidsgiver).kontekst(this)
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
            arbeidsgiver = arbeidsgiver,
            behandlingkilde = behandlingkilde,
            dokumentsporing = dokumentsporing,
            hendelseSykdomstidslinje = hendelseSykdomstidslinje,
            dagerNavOvertarAnsvar = dagerNavOvertarAnsvar,
            egenmeldingsdager = egenmeldingsdager,
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = arbeidsgiver.beregnArbeidsgiverperiode(),
            validering = validering
        )
        if (!haddeFlereSkjæringstidspunkt && behandlinger.harFlereSkjæringstidspunkt()) {
            aktivitetslogg.varsel(RV_IV_11)
        }
    }

    private fun håndterEgenmeldsingsdager(hendelse: Hendelse, dokumentsporing: Dokumentsporing?, aktivitetslogg: IAktivitetslogg, egenmeldingsdager: List<Periode>) = behandlinger.håndterEgenmeldingsdager(
        person = person,
        arbeidsgiver = arbeidsgiver,
        behandlingkilde = hendelse.metadata.behandlingkilde,
        dokumentsporing = dokumentsporing,
        aktivitetslogg = aktivitetslogg,
        beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
        beregnArbeidsgiverperiode = arbeidsgiver.beregnArbeidsgiverperiode(),
        egenmeldingsdager = egenmeldingsdager
    )

    internal fun nullstillEgenmeldingsdagerIArbeidsgiverperiode(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg, dokumentsporing: Dokumentsporing?): List<Revurderingseventyr> {
        val arbeidsgiverperiode = behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.periode() ?: return emptyList()
        return arbeidsgiver.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode)
            .filter { it.håndterEgenmeldsingsdager(hendelse, dokumentsporing, it.registrerKontekst(aktivitetslogg), emptyList()) }
            .map { Revurderingseventyr.arbeidsgiverperiode(hendelse, it.skjæringstidspunkt, it.periode) }
    }

    private fun håndterSøknad(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        nesteTilstand: () -> Vedtaksperiodetilstand? = { null }
    ) {
        videreførEksisterendeRefusjonsopplysninger(søknad.metadata.behandlingkilde, søknad(søknad.metadata.meldingsreferanseId), aktivitetslogg)
        oppdaterHistorikk(søknad.metadata.behandlingkilde, søknad(søknad.metadata.meldingsreferanseId), søknad.sykdomstidslinje, aktivitetslogg) {
            søknad.valider(aktivitetslogg, vilkårsgrunnlag, refusjonstidslinje, subsumsjonslogg)
        }
        if (aktivitetslogg.harFunksjonelleFeilEllerVerre()) return
        nesteTilstand()?.also { tilstand(aktivitetslogg, it) }
    }

    private fun håndterOverlappendeSøknad(
        søknad: Søknad,
        aktivitetslogg: IAktivitetslogg,
        nesteTilstand: Vedtaksperiodetilstand? = null
    ) {
        if (søknad.delvisOverlappende) return aktivitetslogg.funksjonellFeil(`Mottatt søknad som delvis overlapper`)
        aktivitetslogg.info("Håndterer overlappende søknad")
        håndterSøknad(søknad, aktivitetslogg) { nesteTilstand }
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
        if (dager.erKorrigeringForGammel(aktivitetslogg, opprinneligAgp.arbeidsgiverperioder)) {
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
        val inntektForArbeidsgiver = arbeidsgiver
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
            subsummerBrukAvSkatteopplysninger(arbeidsgiver.organisasjonsnummer, inntektsdata, skatteopplysning?.treMånederFørSkjæringstidspunkt ?: emptyList())

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
    ): ArbeidsgiverInntektsopplysning? {
        val alleForSammeArbeidsgiver = vedtaksperioderMedSammeSkjæringstidspunkt
            .filter { it.arbeidsgiver === this.arbeidsgiver }

        return when (this.arbeidsgiver.yrkesaktivitetssporing) {
            is Arbeidstaker -> ArbeidsgiverInntektsopplysning(
                orgnummer = arbeidsgiver.organisasjonsnummer,
                faktaavklartInntekt = inntektForArbeidsgiver(hendelse, aktivitetsloggTilDenSomVilkårsprøver, skatteopplysning, alleForSammeArbeidsgiver),
                korrigertInntekt = null,
                skjønnsmessigFastsatt = null
            )

            Behandlingsporing.Yrkesaktivitet.Selvstendig,

            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans -> null
        }
    }

    private fun avklarSykepengegrunnlagForSelvstendig(): SelvstendigInntektsopplysning? {
        return when (this.arbeidsgiver.yrkesaktivitetssporing) {
            Behandlingsporing.Yrkesaktivitet.Selvstendig -> SelvstendigInntektsopplysning(
                faktaavklartInntekt = inntektForSelvstendig(),
                skjønnsmessigFastsatt = null
            )

            is Arbeidstaker,
            Behandlingsporing.Yrkesaktivitet.Arbeidsledig,
            Behandlingsporing.Yrkesaktivitet.Frilans -> null
        }
    }

    private fun inntektForSelvstendig(): SelvstendigFaktaavklartInntekt {
        val faktaavklartInntekt = checkNotNull(behandlinger.faktaavklartInntekt) { "Forventer å ha en inntekt for selvstendig" }
        val inntektsgrunnlag = faktaavklartInntekt.beregnInntektsgrunnlag(`1G`.beløp(skjæringstidspunkt))

        return faktaavklartInntekt.copy(inntektsdata = faktaavklartInntekt.inntektsdata.copy(beløp = inntektsgrunnlag))
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

        // en inntekt per arbeidsgiver med søknad
        return perioderMedSammeSkjæringstidspunkt
            .distinctBy { it.arbeidsgiver }.mapNotNull { vedtaksperiode ->
                val skatteopplysningForArbeidsgiver = skatteopplysninger.firstOrNull { it.arbeidsgiver == vedtaksperiode.arbeidsgiver.organisasjonsnummer }
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
        if (ghosts.isNotEmpty()) aktivitetslogg.varsel(Varselkode.RV_VV_2)
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
        vilkårsgrunnlag.valider(aktivitetslogg, sykepengegrunnlag, subsumsjonslogg)
        val grunnlagsdata = vilkårsgrunnlag.grunnlagsdata()
        grunnlagsdata.validerFørstegangsvurdering(aktivitetslogg)
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
        inntekterForBeregning(aktivitetslogg, beregningsperiode)
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
            arbeidsgiver.organisasjonsnummer,
            beregningSlutt.minusMonths(2),
            beregningSlutt
        )
    }

    private fun opplysningerViTrenger(): Set<PersonObserver.ForespurtOpplysning> {
        if (!skalBehandlesISpeil()) return emptySet() // perioden er AUU ✋
        if (arbeidsgiver.finnVedtaksperiodeRettFør(this)?.skalBehandlesISpeil() == true) return emptySet() // Da har perioden foran oss spurt for oss/ vi har det vi trenger ✋

        val opplysninger = mutableSetOf<PersonObserver.ForespurtOpplysning>().apply {
            if (!harEksisterendeInntekt()) addAll(setOf(PersonObserver.Inntekt, PersonObserver.Refusjon)) // HAG støtter ikke skjema uten refusjon, så når vi først spør om inntekt _må_ vi også spørre om refusjon
            if (refusjonstidslinje.isEmpty()) add(PersonObserver.Refusjon) // For de tilfellene vi faktiske trenger refusjon
        }
        if (opplysninger.isEmpty()) return emptySet() // Om vi har inntekt og refusjon så er saken biff 🥩

        if (behandlinger.dagerNavOvertarAnsvar.isNotEmpty()) return opplysninger // Trenger hvert fall ikke opplysninger om arbeidsgiverperiode dersom Nav har overtatt ansvar for den ✋

        return opplysninger.apply {
            val sisteDelAvAgp = behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.lastOrNull()
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
        arbeidsgiver.finnVedtaksperiodeRettEtter(this)?.trengerIkkeArbeidsgiveropplysninger()
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
            yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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
            .filterNot { it.arbeidsgiver === this.arbeidsgiver }
            .groupBy { it.arbeidsgiver }
            .mapNotNull { (arbeidsgiver, perioder) ->
                val førsteFraværsdagForArbeidsgiver = perioder
                    .asReversed()
                    .firstNotNullOfOrNull { it.førsteFraværsdag }
                førsteFraværsdagForArbeidsgiver?.let {
                    PersonObserver.FørsteFraværsdag(arbeidsgiver.yrkesaktivitetssporing, it)
                }
            }
        val minEgen = førsteFraværsdag?.let {
            PersonObserver.FørsteFraværsdag(arbeidsgiver.yrkesaktivitetssporing, it)
        } ?: return deAndre
        return deAndre.plusElement(minEgen)
    }

    private fun vedtaksperioderIArbeidsgiverperiodeTilOgMedDenne(): List<Vedtaksperiode> {
        val arbeidsgiverperiode = behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.periode() ?: return listOf(this)
        return arbeidsgiver.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).filter { it <= this }
    }

    private fun trengerIkkeArbeidsgiveropplysninger() {
        person.trengerIkkeArbeidsgiveropplysninger(
            PersonObserver.TrengerIkkeArbeidsgiveropplysningerEvent(
                yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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
            yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
            vedtaksperiodeId = id,
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
                yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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
        person.analytiskDatapakke(behandlinger.analytiskDatapakke(this.arbeidsgiver.yrkesaktivitetssporing, this.id))
        person.gjenopptaBehandling(aktivitetslogg)
    }

    override fun vedtakAnnullert(aktivitetslogg: IAktivitetslogg, behandlingId: UUID) {
        person.vedtaksperiodeAnnullert(
            PersonObserver.VedtaksperiodeAnnullertEvent(
                fom = periode.start,
                tom = periode.endInclusive,
                vedtaksperiodeId = id,
                yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
                behandlingId = behandlingId
            )
        )
    }

    override fun behandlingLukket(behandlingId: UUID) {
        person.behandlingLukket(
            PersonObserver.BehandlingLukketEvent(
                yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
                vedtaksperiodeId = id,
                behandlingId = behandlingId
            )
        )
    }

    override fun behandlingForkastet(behandlingId: UUID, automatiskBehandling: Boolean) {
        person.behandlingForkastet(
            PersonObserver.BehandlingForkastetEvent(
                yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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
            yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
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
            vedtaksperiode.person.vedtaksperiodeIkkePåminnet(id, arbeidsgiver.organisasjonsnummer, type)
            return null
        }
        vedtaksperiode.person.vedtaksperiodePåminnet(id, arbeidsgiver.organisasjonsnummer, påminnelse)
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
        håndter(vedtaksperiode, påminnelse, aktivitetslogg)
        return null
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    internal fun trengerGodkjenning(aktivitetslogg: IAktivitetslogg) {
        behandlinger.godkjenning(aktivitetslogg, utkastTilVedtakBuilder())
    }

    private fun utkastTilVedtakBuilder(): UtkastTilVedtakBuilder {
        val builder = UtkastTilVedtakBuilder(
            yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
            vedtaksperiodeId = id,
            kanForkastes = arbeidsgiver.kanForkastes(this, Aktivitetslogg()),
            erForlengelse = erForlengelse(),
            harPeriodeRettFør = arbeidsgiver.finnVedtaksperiodeRettFør(this) != null,
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
                yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
                vedtaksperiodeId = id,
                skjæringstidspunkt = skjæringstidspunkt,
                periode = periode,
                typeEndring = typeEndring
            )
        )
    }

    internal fun vedtaksperiodeVenter(venterPå: Vedtaksperiode): VedtaksperiodeVenter? {
        val venteårsak = venterPå.venteårsak() ?: return null
        val builder = VedtaksperiodeVenter.Builder()
        builder.venterPå(
            venterPå.id,
            venterPå.skjæringstidspunkt,
            venterPå.arbeidsgiver.yrkesaktivitetssporing,
            venteårsak
        )
        builder.venter(
            vedtaksperiodeId = id,
            skjæringstidspunkt = skjæringstidspunkt,
            yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
            ventetSiden = oppdatert,
            venterTil = venterTil(venterPå)
        )
        behandlinger.behandlingVenter(builder)
        builder.hendelseIder(eksterneIder)
        return builder.build()
    }

    private fun venterTil(venterPå: Vedtaksperiode) =
        if (id == venterPå.id) makstid()
        else minOf(makstid(), venterPå.makstid())

    private fun venteårsak() = tilstand.venteårsak(this)
    private fun makstid(tilstandsendringstidspunkt: LocalDateTime = oppdatert) =
        tilstand.makstid(this, tilstandsendringstidspunkt)

    fun slutterEtter(dato: LocalDate) = periode.slutterEtter(dato)

    private fun lagNyUtbetaling(
        arbeidsgiverSomBeregner: Arbeidsgiver,
        aktivitetslogg: IAktivitetslogg,
        beregning: BeregnetPeriode
    ) {
        behandlinger.nyUtbetaling(
            vedtaksperiodeSomLagerUtbetaling = this.id,
            arbeidsgiver = this.arbeidsgiver,
            aktivitetslogg = aktivitetslogg,
            beregning = beregning
        )
        val subsumsjonen = Utbetalingstidslinjesubsumsjon(this.subsumsjonslogg, this.sykdomstidslinje, beregning.utbetalingstidslinje)
        subsumsjonen.subsummer(periode, this.arbeidsgiver.yrkesaktivitetssporing)
        beregning.maksdatovurdering.subsummer(subsumsjonslogg, periode)
        loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(arbeidsgiverSomBeregner, aktivitetslogg)
    }

    private fun loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(
        arbeidsgiverSomBeregner: Arbeidsgiver,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (!behandlinger.trekkerTilbakePenger()) return
        if (this.arbeidsgiver === arbeidsgiverSomBeregner && !person.blitt6GBegrensetSidenSist(skjæringstidspunkt)) return
        aktivitetslogg.info("En endring hos en arbeidsgiver har medført at det trekkes tilbake penger hos andre arbeidsgivere")
    }

    private fun perioderDetSkalBeregnesUtbetalingFor(): List<Vedtaksperiode> {
        // lag utbetaling for seg selv + andre overlappende perioder hos andre arbeidsgivere (som ikke er utbetalt/avsluttet allerede)
        return person.nåværendeVedtaksperioder { it.erKandidatForUtbetaling(this, this.skjæringstidspunkt) }
            .filter { it.behandlinger.klarForUtbetaling() }
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

    private fun erKandidatForUtbetaling(periodeSomBeregner: Vedtaksperiode, skjæringstidspunktet: LocalDate): Boolean {
        if (this === periodeSomBeregner) return true
        if (!skalBehandlesISpeil()) return false
        return this.periode.overlapperMed(periodeSomBeregner.periode) && skjæringstidspunktet == this.skjæringstidspunkt && !this.tilstand.erFerdigBehandlet
    }

    internal fun skalBehandlesISpeil(): Boolean {
        return forventerInntekt() || behandlinger.navOvertarAnsvar()
    }

    internal fun skalOmgjøres(): Boolean {
        return forventerInntekt() || behandlinger.navOvertarAnsvar()
    }

    private fun forventerInntekt(): Boolean {
        return arbeidsgiver.arbeidsgiverperiode(periode)?.forventerInntekt(periode) == true
    }

    internal fun måInnhenteInntektEllerRefusjon(): Boolean {
        when (arbeidsgiver.yrkesaktivitetssporing) {
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
        return harEksisterendeInntekt() || behandlinger.harGjenbrukbarInntekt(arbeidsgiver.organisasjonsnummer)
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
            .filter { it.arbeidsgiver === this.arbeidsgiver }

        return arbeidsgiver.kanBeregneSykepengegrunnlag(skjæringstidspunkt, perioderMedSammeSkjæringstidspunkt)
    }

    internal fun førstePeriodeSomTrengerInntektsmelding(): Vedtaksperiode? {
        val førsteMursteinsperiodeSomTrengerInntektEllerRefusjon = perioderSomMåHensyntasVedBeregning()
            .firstOrNull { it.måInnhenteInntektEllerRefusjon() }

        if (vilkårsgrunnlag != null) return førsteMursteinsperiodeSomTrengerInntektEllerRefusjon

        val førstePeriodePåSkjæringstidspunktetAnnenArbeidsgiverSomTrengerInntektEllerRefusjon = person.nåværendeVedtaksperioder { other ->
            this.arbeidsgiver !== other.arbeidsgiver && other.skjæringstidspunkt == skjæringstidspunkt && other.måInnhenteInntektEllerRefusjon()
        }.minOrNull()

        return førstePeriodePåSkjæringstidspunktetAnnenArbeidsgiverSomTrengerInntektEllerRefusjon ?: førsteMursteinsperiodeSomTrengerInntektEllerRefusjon
    }

    private val beregningsperiode get() = checkNotNull(perioderSomMåHensyntasVedBeregning().map { it.periode }.periode()) { "Hvordan kan det ha seg at vi ikke har noen beregningsperiode?" }
    private fun beregnUtbetalinger(aktivitetslogg: IAktivitetslogg, inntekterForBeregningBuilder: InntekterForBeregning.Builder): Maksdatoresultat {
        val perioderDetSkalBeregnesUtbetalingFor = perioderDetSkalBeregnesUtbetalingFor()

        check(perioderDetSkalBeregnesUtbetalingFor.all { it.skjæringstidspunkt == this.skjæringstidspunkt }) {
            "ugyldig situasjon: skal beregne utbetaling for vedtaksperioder med ulike skjæringstidspunkter"
        }
        val grunnlagsdata = checkNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for ${skjæringstidspunkt}, men har ikke. Lages det utbetaling for en periode som ikke skal lage utbetaling?"
        }

        val inntekterForBeregning = inntekterForBeregningBuilder.apply {
            grunnlagsdata.inntektsgrunnlag.beverte(this)
        }.build()

        val beregnetTidslinjePerVedtaksperiode = beregnUtbetalingstidslinjeForOverlappendeVedtaksperioder(
            aktivitetslogg,
            grunnlagsdata,
            inntekterForBeregning
        )
        perioderDetSkalBeregnesUtbetalingFor.forEach { other ->
            val beregning = beregnetTidslinjePerVedtaksperiode.single { it.vedtaksperiodeId == other.id }
            val utbetalingstidslinje = beregning.utbetalingstidslinje
            val maksdatoresultat = beregning.maksdatovurdering
            other.lagNyUtbetaling(
                arbeidsgiverSomBeregner = this.arbeidsgiver,
                aktivitetslogg = other.registrerKontekst(aktivitetslogg),
                beregning = BeregnetPeriode(
                    maksdatovurdering = maksdatoresultat,
                    grunnlagsdata = grunnlagsdata,
                    utbetalingstidslinje = utbetalingstidslinje,
                    inntekterForBeregning = inntekterForBeregning
                )
            )
        }
        return behandlinger.maksdato
    }

    private fun beregnUtbetalingstidslinjeForOverlappendeVedtaksperioder(
        aktivitetslogg: IAktivitetslogg,
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        inntekterForBeregning: InntekterForBeregning
    ): List<Vedtaksperiodeberegningsresultat> {
        val uberegnetTidslinjePerArbeidsgiver = utbetalingstidslinjePerArbeidsgiver(inntekterForBeregning)
        return filtrerUtbetalingstidslinjer(aktivitetslogg, uberegnetTidslinjePerArbeidsgiver, grunnlagsdata)
    }

    private data class Vedtaksperiodeberegningsresultat(
        val vedtaksperiodeId: UUID,
        val utbetalingstidslinje: Utbetalingstidslinje,
        val maksdatovurdering: Maksdatovurdering
    )

    private fun utbetalingstidslinjePerArbeidsgiver(inntekterForBeregning: InntekterForBeregning): List<Arbeidsgiverberegning> {
        val utbetalingstidslinjer = perioderSomMåHensyntasVedBeregning()
            .groupBy({ it.arbeidsgiver.organisasjonsnummer }) { vedtaksperiode ->
                val (fastsattÅrsinntekt, inntektjusteringer) = inntekterForBeregning.tilBeregning(vedtaksperiode.arbeidsgiver.organisasjonsnummer)
                Vedtaksperiodeberegning(
                    vedtaksperiodeId = vedtaksperiode.id,
                    utbetalingstidslinje = vedtaksperiode.behandlinger.lagUtbetalingstidslinje(
                        fastsattÅrsinntekt = fastsattÅrsinntekt,
                        inntektjusteringer = inntektjusteringer,
                        yrkesaktivitet = vedtaksperiode.arbeidsgiver.yrkesaktivitetssporing
                    )
                )
            }
            .map { (orgnr, vedtaksperioder) ->
                Arbeidsgiverberegning(
                    orgnummer = orgnr,
                    vedtaksperioder = vedtaksperioder,
                    ghostOgAndreInntektskilder = emptyList()
                )
            }
        // nå vi må lage en ghost-tidslinje per arbeidsgiver for de som eksisterer i sykepengegrunnlaget.
        // i tillegg må vi lage én tidslinje per inntektskilde som ikke er en del av sykepengegrunnlaget
        // resultatet er én utbetalingstidslinje per arbeidsgiver som garantert dekker perioden ${vedtaksperiode.periode}, dog kan
        // andre arbeidsgivere dekke litt før/litt etter, avhengig av perioden til vedtaksperiodene som overlapper
        return inntekterForBeregning.hensyntattAlleInntektskilder(utbetalingstidslinjer)
    }

    private fun filtrerUtbetalingstidslinjer(
        aktivitetslogg: IAktivitetslogg,
        uberegnetTidslinjePerArbeidsgiver: List<Arbeidsgiverberegning>,
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
    ): List<Vedtaksperiodeberegningsresultat> {
        // grunnlaget for maksdatoberegning er alt som har skjedd før,
        // frem til og med vedtaksperioden som beregnes
        val historisktidslinje = person.vedtaksperioder { it.periode.endInclusive < periode.start }
            .map { it.behandlinger.utbetalingstidslinje() }
            .fold(person.infotrygdhistorikk.utbetalingstidslinje(), Utbetalingstidslinje::plus)

        val maksdatofilter = MaksimumSykepengedagerfilter(person.alder, subsumsjonslogg, aktivitetslogg, person.regler, historisktidslinje)
        val filtere = listOf(
            Sykdomsgradfilter(person.minimumSykdomsgradsvurdering, subsumsjonslogg, aktivitetslogg),
            AvvisDagerEtterDødsdatofilter(person.alder, aktivitetslogg),
            AvvisInngangsvilkårfilter(
                skjæringstidspunkt = skjæringstidspunkt,
                alder = person.alder,
                subsumsjonslogg = subsumsjonslogg,
                aktivitetslogg = aktivitetslogg,
                inntektsgrunnlag = grunnlagsdata.inntektsgrunnlag,
                medlemskapstatus = (grunnlagsdata as? VilkårsgrunnlagHistorikk.Grunnlagsdata)?.medlemskapstatus,
                opptjening = grunnlagsdata.opptjening.takeUnless { arbeidsgiver.yrkesaktivitetssporing is Behandlingsporing.Yrkesaktivitet.Selvstendig }
            ),
            maksdatofilter,
            MaksimumUtbetalingFilter(
                sykepengegrunnlagBegrenset6G = grunnlagsdata.inntektsgrunnlag.sykepengegrunnlag,
                er6GBegrenset = grunnlagsdata.inntektsgrunnlag.er6GBegrenset(),
                aktivitetslogg = aktivitetslogg
            )
        )

        val beregnetTidslinjePerArbeidsgiver = filtere.fold(uberegnetTidslinjePerArbeidsgiver) { tidslinjer, filter ->
            filter.filter(tidslinjer, periode)
        }

        return beregnetTidslinjePerArbeidsgiver.flatMap {
            it.vedtaksperioder.map { vedtaksperiodeberegning ->
                Vedtaksperiodeberegningsresultat(
                    vedtaksperiodeId = vedtaksperiodeberegning.vedtaksperiodeId,
                    utbetalingstidslinje = vedtaksperiodeberegning.utbetalingstidslinje,
                    maksdatovurdering = maksdatofilter.maksdatoresultatForVedtaksperiode(vedtaksperiodeberegning.periode)
                )
            }
        }
    }

    internal fun håndterOverstyringIgangsattRevurdering(
        revurdering: Revurderingseventyr,
        aktivitetslogg: IAktivitetslogg
    ) {
        revurdering.inngåSomRevurdering(this, aktivitetslogg)
        behandlinger.sikreNyBehandling(
            arbeidsgiver,
            revurdering.hendelse.metadata.behandlingkilde,
            person.beregnSkjæringstidspunkt(),
            arbeidsgiver.beregnArbeidsgiverperiode()
        )
        tilstand(aktivitetslogg, AvventerRevurdering)
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

    internal fun sikreRefusjonsopplysningerHvisTomt(påminnelse: Påminnelse, aktivitetslogg: IAktivitetslogg) {
        if (!påminnelse.når(Flagg("fullRefusjon"))) return
        if (!behandlinger.refusjonstidslinje().isEmpty()) return
        val grunnlag = vilkårsgrunnlag ?: return
        val inntekt = grunnlag.inntektsgrunnlag.arbeidsgiverInntektsopplysninger.firstOrNull { it.orgnummer == arbeidsgiver.organisasjonsnummer } ?: return
        behandlinger.håndterRefusjonstidslinje(
            arbeidsgiver = arbeidsgiver,
            behandlingkilde = påminnelse.metadata.behandlingkilde,
            dokumentsporing = null,
            aktivitetslogg = aktivitetslogg,
            beregnSkjæringstidspunkt = person.beregnSkjæringstidspunkt(),
            beregnArbeidsgiverperiode = arbeidsgiver.beregnArbeidsgiverperiode(),
            refusjonstidslinje = Beløpstidslinje.fra(periode, inntekt.fastsattÅrsinntekt, Kilde(inntekt.faktaavklartInntekt.inntektsdata.hendelseId, Avsender.ARBEIDSGIVER, inntekt.faktaavklartInntekt.inntektsdata.tidsstempel))
        )
    }

    private fun prioritertNabolag(): List<Vedtaksperiode> {
        val (nabolagFør, nabolagEtter) = this.arbeidsgiver.finnSammenhengendeVedtaksperioder(this)
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
            arbeidsgiver.refusjonstidslinje(this).takeUnless { it.isEmpty() }?.also { ubrukte ->
                val unikeKilder = ubrukte.filterIsInstance<Beløpsdag>().map { it.kilde.meldingsreferanseId }.toSet()
                aktivitetslogg.info("Fant ubrukte refusjonsopplysninger for $periode fra kildene ${unikeKilder.joinToString()}")
            } ?: Beløpstidslinje()
        val benyttetRefusjonstidslinje =
            (refusjonstidslinjeFraArbeidsgiver + refusjonstidslinjeFraNabolaget).fyll(periode)
        if (benyttetRefusjonstidslinje.isEmpty()) return
        this.behandlinger.håndterRefusjonstidslinje(
            arbeidsgiver,
            behandlingkilde,
            dokumentsporing,
            aktivitetslogg,
            person.beregnSkjæringstidspunkt(),
            arbeidsgiver.beregnArbeidsgiverperiode(),
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
            organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
            arbeidsgiver = arbeidsgiver,
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
                    vedtaksperiode.arbeidsgiver.finnSammenhengendeVedtaksperioder(vedtaksperiode)
                val startdatoPåSammenhengendeVedtaksperioder = sammenhendeVedtaksperioder.first().periode.start
                startdatoer.putAll(sammenhendeVedtaksperioder.associate { it.id to startdatoPåSammenhengendeVedtaksperioder })
            }

            return startdatoer.values.toSet()
        }

        internal fun List<Vedtaksperiode>.aktiv(vedtaksperiodeId: UUID) = any { it.id == vedtaksperiodeId }

        // Fredet funksjonsnavn
        internal val OVERLAPPENDE_OG_ETTERGØLGENDE = fun(segSelv: Vedtaksperiode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode): Boolean {
                return segSelv.periode.overlapperEllerStarterFør(other.periode)
            }
        }

        internal val SAMME_ARBEIDSGIVERPERIODE = fun(arbeidsgiver: Arbeidsgiver, arbeidsgiverperiode: Periode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode): Boolean {
                return other.arbeidsgiver.organisasjonsnummer == arbeidsgiver.organisasjonsnummer && (other.behandlinger.arbeidsgiverperiode().arbeidsgiverperioder.periode()?.overlapperMed(arbeidsgiverperiode) == true)
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
            it.tilstand == AvsluttetUtenUtbetaling && it.skalOmgjøres()
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
            beregnArbeidsgiverperiode: (Periode) -> List<Periode>
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

        internal fun List<Vedtaksperiode>.venter(nestemann: Vedtaksperiode) =
            mapNotNull { vedtaksperiode -> vedtaksperiode.tilstand.venter(vedtaksperiode, nestemann) }

        internal fun List<Vedtaksperiode>.validerTilstand(hendelse: Hendelse, aktivitetslogg: IAktivitetslogg) =
            forEach { it.validerTilstand(hendelse, aktivitetslogg) }

        internal fun gjenopprett(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            dto: VedtaksperiodeInnDto,
            regelverkslogg: Regelverkslogg,
            grunnlagsdata: Map<UUID, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement>,
            utbetalinger: Map<UUID, Utbetaling>
        ): Vedtaksperiode {
            return Vedtaksperiode(
                person = person,
                arbeidsgiver = arbeidsgiver,
                id = dto.id,
                tilstand = when (dto.tilstand) {
                    VedtaksperiodetilstandDto.AVSLUTTET -> Avsluttet
                    VedtaksperiodetilstandDto.AVSLUTTET_UTEN_UTBETALING -> AvsluttetUtenUtbetaling
                    VedtaksperiodetilstandDto.AVVENTER_BLOKKERENDE_PERIODE -> AvventerBlokkerendePeriode
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

                    VedtaksperiodetilstandDto.SELVSTENDIG_START -> SelvstendigStart
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK -> SelvstendigAvventerInfotrygdHistorikk
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE -> SelvstendigAvventerBlokkerendePeriode
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING -> SelvstendigAvventerVilkårsprøving
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK -> SelvstendigAvventerHistorikk
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING -> SelvstendigAvventerSimulering
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING -> SelvstendigAvventerGodkjenning

                    VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING -> SelvstendigTilUtbetaling
                    VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET -> SelvstendigAvsluttet
                    VedtaksperiodetilstandDto.SELVSTENDIG_TIL_INFOTRYGD -> SelvstendigTilInfotrygd
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
                    yrkesaktivitetssporing = arbeidsgiver.yrkesaktivitetssporing,
                    vedtaksperiodeId = this.id,
                    kanForkastes = arbeidsgiver.kanForkastes(this, Aktivitetslogg()),
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

            SelvstendigAvsluttet -> VedtaksperiodetilstandDto.SELVSTENDIG_AVSLUTTET
            SelvstendigAvventerBlokkerendePeriode -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_BLOKKERENDE_PERIODE
            SelvstendigAvventerGodkjenning -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_GODKJENNING
            SelvstendigAvventerHistorikk -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_HISTORIKK
            SelvstendigAvventerInfotrygdHistorikk -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_INFOTRYGDHISTORIKK
            SelvstendigAvventerSimulering -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_SIMULERING
            SelvstendigAvventerVilkårsprøving -> VedtaksperiodetilstandDto.SELVSTENDIG_AVVENTER_VILKÅRSPRØVING
            SelvstendigStart -> VedtaksperiodetilstandDto.SELVSTENDIG_START
            SelvstendigTilInfotrygd -> VedtaksperiodetilstandDto.SELVSTENDIG_TIL_INFOTRYGD
            SelvstendigTilUtbetaling -> VedtaksperiodetilstandDto.SELVSTENDIG_TIL_UTBETALING
        },
        skjæringstidspunkt = this.skjæringstidspunkt,
        fom = this.periode.start,
        tom = this.periode.endInclusive,
        sykmeldingFom = this.sykmeldingsperiode.start,
        sykmeldingTom = this.sykmeldingsperiode.endInclusive,
        behandlinger = behandlinger.dto(),
        venteårsak = LazyVedtaksperiodeVenterDto { nestemann?.let { tilstand.venter(this, it)?.dto() } },
        opprettet = opprettet,
        oppdatert = oppdatert
    )

    private fun IAktivitetslogg.medFeilSomVarslerHvisNødvendig() =
        when (!arbeidsgiver.kanForkastes(this@Vedtaksperiode, this)) {
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
    val skalBehandlesISpeil: Boolean
) {
    val sykdomstidslinje = behandlinger.behandlinger.last().endringer.last().sykdomstidslinje
    val refusjonstidslinje = behandlinger.behandlinger.last().endringer.last().refusjonstidslinje
    val dagerNavOvertarAnsvar = behandlinger.behandlinger.last().endringer.last().dagerNavOvertarAnsvar
}

internal val HendelseMetadata.behandlingkilde
    get() =
        Behandlingkilde(meldingsreferanseId, innsendt, registrert, avsender)
