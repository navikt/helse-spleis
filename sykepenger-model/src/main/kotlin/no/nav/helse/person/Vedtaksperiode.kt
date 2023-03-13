package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Alder
import no.nav.helse.Toggle
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplayUtført
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.PersonHendelse
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.UtbetalingshistorikkEtterInfotrygdendring
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.hendelser.inntektsmelding.InntektOgRefusjonFraInntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.memoized
import no.nav.helse.person.Arbeidsgiver.Companion.avventerSøknad
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigInntektForVilkårsprøving
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigRefusjonsopplysninger
import no.nav.helse.person.Arbeidsgiver.Companion.harTilstrekkeligInformasjonTilUtbetaling
import no.nav.helse.person.Arbeidsgiver.Companion.trengerInntektsmelding
import no.nav.helse.person.Arbeidsgiver.Companion.vedtaksperioder
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.Dokumentsporing.Companion.toMap
import no.nav.helse.person.ForlengelseFraInfotrygd.IKKE_ETTERSPURT
import no.nav.helse.person.InntektsmeldingInfo.Companion.ider
import no.nav.helse.person.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.person.Sykefraværstilfelleeventyr.Companion.bliMed
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
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
import no.nav.helse.person.Venteårsak.Hvorfor.HAR_SYKMELDING_SOM_OVERLAPPER_PÅ_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_INNTEKT_FOR_VILKÅRSPRØVING_PÅ_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_REFUSJONSOPPLYSNINGER_PÅ_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_SAMME_ARBEIDSGIVER
import no.nav.helse.person.Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsforhold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.dødsinformasjon
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSammenligningsgrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlag
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
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad out of order`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad out of order innenfor 18 dager`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad som delvis overlapper`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.`Mottatt søknad som overlapper`
import no.nav.helse.person.aktivitetslogg.Varselkode.Companion.varsel
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OO_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RE_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_15
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_19
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_20
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_28
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_29
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_30
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_16
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_6
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_7
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.serde.AktivitetsloggMap
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingInntektskilde
import no.nav.helse.utbetalingslinjer.UtbetalingPeriodetype
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.sammenlign
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjeBuilderException
import no.nav.helse.økonomi.Inntekt
import org.slf4j.LoggerFactory

internal class Vedtaksperiode private constructor(
    private val person: Person,
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private var tilstand: Vedtaksperiodetilstand,
    private var skjæringstidspunktFraInfotrygd: LocalDate?,
    private var sykdomstidslinje: Sykdomstidslinje,
    private val hendelseIder: MutableSet<Dokumentsporing>,
    private var inntektsmeldingInfo: InntektsmeldingInfo?,
    private var periode: Periode,
    private val sykmeldingsperiode: Periode,
    private val utbetalinger: VedtaksperiodeUtbetalinger,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
    private var forlengelseFraInfotrygd: ForlengelseFraInfotrygd = IKKE_ETTERSPURT,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet,
    jurist: MaskinellJurist
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val jurist = jurist.medVedtaksperiode(id, hendelseIder.toMap(), sykmeldingsperiode)
    private val skjæringstidspunkt get() = person.skjæringstidspunkt(sykdomstidslinje.sykdomsperiode() ?: periode)
    private val vilkårsgrunnlag get() = person.vilkårsgrunnlagFor(skjæringstidspunkt)
    private val inntektskilde get() = vilkårsgrunnlag?.inntektskilde() ?: Inntektskilde.EN_ARBEIDSGIVER

    internal constructor(
        søknad: Søknad,
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        sykdomstidslinje: Sykdomstidslinje,
        dokumentsporing: Dokumentsporing,
        periode: Periode,
        jurist: MaskinellJurist
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = UUID.randomUUID(),
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        tilstand = Start,
        skjæringstidspunktFraInfotrygd = null,
        sykdomstidslinje = sykdomstidslinje,
        hendelseIder = mutableSetOf(dokumentsporing),
        inntektsmeldingInfo = null,
        periode = periode,
        sykmeldingsperiode = periode,
        utbetalinger = VedtaksperiodeUtbetalinger(),
        utbetalingstidslinje = Utbetalingstidslinje(),
        opprettet = LocalDateTime.now(),
        jurist = jurist
    ) {
        kontekst(søknad)
    }

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        val skjæringstidspunktMemoized = this::skjæringstidspunkt.memoized()
        val inntektskildeMemoized = this::inntektskilde.memoized()
        visitor.preVisitVedtaksperiode(
            this,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            sykmeldingsperiode,
            skjæringstidspunktMemoized,
            skjæringstidspunktFraInfotrygd,
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskildeMemoized
        )
        inntektsmeldingInfo?.accept(visitor)
        sykdomstidslinje.accept(visitor)
        utbetalingstidslinje.accept(visitor)
        utbetalinger.accept(visitor)
        visitor.postVisitVedtaksperiode(
            this,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            sykmeldingsperiode,
            skjæringstidspunktMemoized,
            skjæringstidspunktFraInfotrygd,
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskildeMemoized
        )
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun jurist() = jurist.medVedtaksperiode(id, hendelseIder.toSet().toMap(), sykmeldingsperiode)

    internal fun harId(vedtaksperiodeId: UUID) = id == vedtaksperiodeId

    internal fun hendelseIder() = hendelseIder.ider()

    internal fun håndter(sykmelding: Sykmelding) {
        sykmelding.trimLeft(periode.endInclusive)
    }

    internal fun håndter(søknad: Søknad) {
        if (!søknad.erRelevant(this.periode)) return
        kontekst(søknad)
        søknadHåndtert(søknad)
        tilstand.håndter(this, søknad)
        søknad.trimLeft(periode.endInclusive)
    }

    private fun inntektsmeldingHåndtert(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
        if (!inntektOgRefusjon.leggTil(hendelseIder)) return
        person.emitInntektsmeldingHåndtert(inntektOgRefusjon.meldingsreferanseId(), id, organisasjonsnummer)
    }
    private fun inntektsmeldingHåndtert(hendelse: SykdomstidslinjeHendelse) {
        if (!hendelse.leggTil(hendelseIder)) return
        person.emitInntektsmeldingHåndtert(hendelse.meldingsreferanseId(), id, organisasjonsnummer)
    }
    private fun inntektsmeldingHåndtert(dager: DagerFraInntektsmelding) {
        if (!dager.leggTil(hendelseIder)) return
        person.emitInntektsmeldingHåndtert(dager.meldingsreferanseId(), id, organisasjonsnummer)
    }

    private fun søknadHåndtert(søknad: Søknad) {
        søknad.leggTil(hendelseIder)
        person.emitSøknadHåndtert(søknad.meldingsreferanseId(), id, organisasjonsnummer)
    }

    internal fun håndter(inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {
        if (!inntektsmeldingReplayUtført.erRelevant(this.id)) return
        kontekst(inntektsmeldingReplayUtført)
        tilstand.håndter(this, inntektsmeldingReplayUtført)
    }

    internal fun håndter(dager: DagerFraInntektsmelding): Boolean {
        val skalHåndtereDager = dager.skalHåndteresAv(periode)
        if (erAlleredeHensyntatt(dager.meldingsreferanseId()) || !skalHåndtereDager) {
            dager.vurdertTilOgMed(periode.endInclusive)
            return skalHåndtereDager
        }
        kontekst(dager)
        return tilstand.håndter(this, dager).also {
            dager.vurdertTilOgMed(periode.endInclusive)
        }
    }

    internal fun postHåndter(dager: DagerFraInntektsmelding) {
        if (dager.harBlittHåndtertAv(periode) || dager.skalHåndteresAv(periode)) {
            inntektsmeldingHåndtert(dager)
            if (tilstand == AvsluttetUtenUtbetaling) {
                // Kun for å beholde dagens funksjonelle oppførsel
                // Hvis en periode i AUU overlapper med noen arbeidgsiveperiodedager, men har ikke håndtert inntekt
                // Så mistenker vi at det mulig vil komme en forlengelse som skal håndtere den inntekten
                // Derfor utsetter vi oppretelse av gosys-oppgave på inntektsmeldingen
                dager.utsettOppgave(person)
            }
        }
    }

    private fun forventerInntektOgRefusjonFraInntektsmelding() = tilstand != AvsluttetUtenUtbetaling || forventerInntekt()

    internal fun håndter(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
        val alleredeHensyntatt = erAlleredeHensyntatt(inntektOgRefusjon.meldingsreferanseId())
        kontekst(inntektOgRefusjon)
        inntektsmeldingHåndtert(inntektOgRefusjon)
        inntektsmeldingInfo = inntektOgRefusjon.addInntektsmelding(skjæringstidspunkt, arbeidsgiver, jurist)
        if (alleredeHensyntatt) return
        inntektOgRefusjon.nyeArbeidsgiverInntektsopplysninger(skjæringstidspunkt, person, jurist())
        tilstand.håndter(this, inntektOgRefusjon)
    }

    private fun håndterInntektOgRefusjon(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding, nesteTilstand: Vedtaksperiodetilstand? = null) {
        inntektOgRefusjon.valider(periode, skjæringstidspunkt)
        inntektOgRefusjon.info("Fullført behandling av inntektsmelding")
        if (inntektOgRefusjon.harFunksjonelleFeilEllerVerre()) return forkast(inntektOgRefusjon)
        nesteTilstand?.also { tilstand(inntektOgRefusjon, it) }
    }

    private fun håndterDager(dager: DagerFraInntektsmelding) {
        tilstand.håndterDagerFør(this, dager)
        dager.håndter(periode, arbeidsgiver)?.let { oppdatertSykdomstidslinje ->
            sykdomstidslinje = oppdatertSykdomstidslinje
        }
    }

    private fun erAlleredeHensyntatt(meldingsreferanseId: UUID) =
        hendelseIder.ider().contains(meldingsreferanseId)

    internal fun håndterHistorikkFraInfotrygd(hendelse: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk) {
        tilstand.håndter(person, arbeidsgiver, this, hendelse, infotrygdhistorikk)
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk, infotrygdhistorikk: Infotrygdhistorikk) {
        if (!utbetalingshistorikk.erRelevant(id)) return
        kontekst(utbetalingshistorikk)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingshistorikk, infotrygdhistorikk)
    }

    internal fun håndter(
        hendelse: UtbetalingshistorikkEtterInfotrygdendring,
        infotrygdhistorikk: Infotrygdhistorikk
    ) {
        kontekst(hendelse)
        tilstand.håndter(this, hendelse, infotrygdhistorikk)
    }

    internal fun håndter(
        ytelser: Ytelser,
        infotrygdhistorikk: Infotrygdhistorikk,
        arbeidsgiverUtbetalinger: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
    ) {
        if (!ytelser.erRelevant(id)) return
        kontekst(ytelser)
        tilstand.håndter(person, arbeidsgiver, this, ytelser, infotrygdhistorikk, arbeidsgiverUtbetalinger)
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        if (!utbetalingsgodkjenning.erRelevant(id.toString())) return
        if (utbetalinger.gjelderIkkeFor(utbetalingsgodkjenning)) return utbetalingsgodkjenning.info("Ignorerer løsning på godkjenningsbehov, utbetalingid på løsningen matcher ikke vedtaksperiodens nåværende utbetaling")
        kontekst(utbetalingsgodkjenning)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingsgodkjenning)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (!vilkårsgrunnlag.erRelevant(id, skjæringstidspunkt)) return
        kontekst(vilkårsgrunnlag)
        tilstand.håndter(this, vilkårsgrunnlag)
    }

    internal fun håndter(simulering: Simulering) {
        if (!simulering.erRelevant(id)) return
        kontekst(simulering)
        tilstand.håndter(this, simulering)
    }

    internal fun håndter(hendelse: UtbetalingHendelse) {
        if (utbetalinger.gjelderIkkeFor(hendelse)) return
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
    }

    internal fun håndter(hendelse: AnnullerUtbetaling, annullering: Utbetaling) {
        if (utbetalinger.hørerIkkeSammenMed(annullering)) return
        kontekst(hendelse)
        hendelse.info("Forkaster denne, og senere perioder, som følge av annullering.")
        forkast(hendelse)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (!påminnelse.erRelevant(id)) return false
        kontekst(påminnelse)
        tilstand.påminnelse(this, påminnelse)
        return true
    }

    internal fun håndter(hendelse: OverstyrTidslinje) {
        if (!hendelse.erRelevant(periode)) return
        kontekst(hendelse)
        hendelse.leggTil(hendelseIder)
        tilstand.håndter(this, hendelse)
        hendelse.trimLeft(periode.endInclusive)
    }

    internal fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold, vedtaksperioder: Iterable<Vedtaksperiode>): Boolean {
        if (!overstyrArbeidsforhold.erRelevant(skjæringstidspunkt)) return false
        kontekst(overstyrArbeidsforhold)
        vedtaksperioder.filter(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt)).forEach {
            overstyrArbeidsforhold.leggTil(it.hendelseIder)
        }
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(overstyrArbeidsforhold, this.skjæringstidspunkt, jurist)
        return true
    }

    internal fun håndter(overstyrArbeidsgiveropplysninger: OverstyrArbeidsgiveropplysninger, vedtaksperioder: Iterable<Vedtaksperiode>): Boolean {
        if (!overstyrArbeidsgiveropplysninger.erRelevant(skjæringstidspunkt)) return false
        kontekst(overstyrArbeidsgiveropplysninger)
        vedtaksperioder.filter(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt)).forEach {
            overstyrArbeidsgiveropplysninger.leggTil(it.hendelseIder)
        }
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
            overstyrArbeidsgiveropplysninger,
            this.skjæringstidspunkt,
            jurist
        )
        return true
    }

    internal fun nekterOpprettelseAvNyPeriode(ny: Vedtaksperiode, hendelse: Søknad) {
        if (ny.periode.starterEtter(this.periode)) return
        if (this.arbeidsgiver !== ny.arbeidsgiver) return
        kontekst(hendelse)
        if (this.periode.overlapperMed(ny.periode)) return hendelse.funksjonellFeil(`Mottatt søknad som overlapper`)
        // Vi er litt runde i kantene før perioden er utbetalt
        if (!this.utbetalinger.harAvsluttede() && !this.utbetalinger.utbetales()) return
        // Vi er litt strengere etter perioden er utbetalt

        if (this.påvirkerArbeidsgiverperioden(ny)) return hendelse.funksjonellFeil(`Mottatt søknad out of order innenfor 18 dager`)
        if (Toggle.OutOfOrderPåvirkerSkjæringstidspunkt.disabled && ny.periode.erRettFør(this.periode)) return hendelse.funksjonellFeil(`Mottatt søknad out of order`)
    }

    private fun påvirkerArbeidsgiverperioden(ny: Vedtaksperiode): Boolean {
        if (Toggle.OutOfOrderInnenfor18Dager.enabled) return false
        val dagerMellom = ny.periode.periodeMellom(this.periode.start)?.count() ?: return false
        // dersom "ny" slutter på en fredag, så starter ikke oppholdstelling før påfølgende mandag.
        // det kan derfor være mer enn 16 dager avstand mellom periodene, og arbeidsgiverperioden kan være den samme
        // Derfor bruker vi tallet 18 fremfor kanskje det forventende 16…
        return dagerMellom < 18L
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

    internal fun erSykeperiodeAvsluttetUtenUtbetalingRettFør(other: Vedtaksperiode) =
        this.sykdomstidslinje.erRettFør(other.sykdomstidslinje) && this.tilstand == AvsluttetUtenUtbetaling

    private fun manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() =
        person.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt)

    private fun harTilstrekkeligInformasjonTilUtbetaling(hendelse: IAktivitetslogg) =
        arbeidsgiver.harTilstrekkeligInformasjonTilUtbetaling(skjæringstidspunkt, periode, hendelse)

    private fun låsOpp() = arbeidsgiver.låsOpp(periode)
    private fun lås() = arbeidsgiver.lås(periode)

    internal fun forkast(hendelse: IAktivitetslogg, utbetalinger: List<Utbetaling>): Boolean {
        if (!this.utbetalinger.kanForkastes(utbetalinger) || this.tilstand == AvsluttetUtenUtbetaling) return false
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        this.utbetalinger.forkast(hendelse)
        person.vedtaksperiodeForkastet(
            PersonObserver.VedtaksperiodeForkastetEvent(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = id,
                gjeldendeTilstand = tilstand.type,
                hendelser = hendelseIder(),
                fom = periode.start,
                tom = periode.endInclusive
            )
        )
        if (this.tilstand !in AVSLUTTET_OG_SENERE) tilstand(hendelse, TilInfotrygd)
        return true
    }

    private fun forkast(hendelse: IAktivitetslogg) {
        person.søppelbøtte(hendelse, TIDLIGERE_OG_ETTERGØLGENDE(this))
    }

    private fun revurderTidslinje(hendelse: OverstyrTidslinje) {
        oppdaterHistorikk(hendelse)
        igangsettOverstyringAvTidslinje(hendelse)
    }
    private fun revurderLåstTidslinje(hendelse: OverstyrTidslinje) {
        låsOpp()
        oppdaterHistorikk(hendelse)
        lås()
        igangsettOverstyringAvTidslinje(hendelse)
    }

    private fun igangsettOverstyringAvTidslinje(hendelse: OverstyrTidslinje) {
        val vedtaksperiodeTilRevurdering = arbeidsgiver.finnVedtaksperiodeFør(this)
            ?.takeIf { nyArbeidsgiverperiodeEtterEndring(it, hendelse) } ?: this
        person.igangsettOverstyring(hendelse, Revurderingseventyr.sykdomstidslinje(vedtaksperiodeTilRevurdering.skjæringstidspunkt, vedtaksperiodeTilRevurdering.periode))
    }

    private fun nyArbeidsgiverperiodeEtterEndring(other: Vedtaksperiode, hendelse: OverstyrTidslinje): Boolean {
        // hvorvidt man delte samme utbetaling før
        if (other.utbetalinger.hørerIkkeSammenMed(this.utbetalinger)) return false
        val arbeidsgiverperiodeOther = other.finnArbeidsgiverperiode()
        val arbeidsgiverperiodeThis = this.finnArbeidsgiverperiode()
        if (arbeidsgiverperiodeOther == null || arbeidsgiverperiodeThis == null) return false
        val periode = arbeidsgiverperiodeThis.periode(this.periode.endInclusive)
        // ingen overlapp i arbeidsgiverperiodene => ny arbeidsgiverperiode
        return periode !in arbeidsgiverperiodeOther
    }

    internal fun periode() = periode

    private fun kontekst(hendelse: IAktivitetslogg) {
        hendelse.kontekst(arbeidsgiver)
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun sykefraværstilfelle() = person.sykefraværstilfelle(skjæringstidspunkt)

    private fun tilstand(
        event: IAktivitetslogg,
        nyTilstand: Vedtaksperiodetilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) return  // Already in this state => ignore
        tilstand.leaving(this, event)

        val previousState = tilstand

        tilstand = nyTilstand
        oppdatert = LocalDateTime.now()
        block()

        event.kontekst(tilstand)
        emitVedtaksperiodeEndret(event, previousState)
        tilstand.entering(this, event)
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        periode = hendelse.oppdaterFom(this.periode)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
        lagreTidsnæreopplysninger()
        arbeidsgiver.finnVedtaksperiodeRettEtter(this)?.lagreTidsnæreopplysninger()
    }

    private fun lagreTidsnæreopplysninger() {
        utbetalinger.lagreTidsnæreInntekter(arbeidsgiver, skjæringstidspunkt)
    }

    private fun håndterSøknad(søknad: Søknad, nesteTilstand: () -> Vedtaksperiodetilstand? = { null }) {
        oppdaterHistorikk(søknad)
        søknad.valider(periode, jurist())
        søknad.validerInntektskilder(vilkårsgrunnlag == null)
        if (søknad.harFunksjonelleFeilEllerVerre()) {
            return forkast(søknad)
        }
        nesteTilstand()?.also { tilstand(søknad, it) }
    }

    private fun håndterOverlappendeSøknad(søknad: Søknad, nesteTilstand: Vedtaksperiodetilstand? = null) {
        if (søknad.delvisOverlappende(periode)) {
            søknad.funksjonellFeil(`Mottatt søknad som delvis overlapper`)
            return forkast(søknad)
        }
        håndterSøknad(søknad) { nesteTilstand }
    }

    private fun håndterLåstOverlappendeSøknadRevurdering(søknad: Søknad) {
        håndterOverlappendeSøknadRevurdering(søknad) {
            låsOpp()
            oppdaterHistorikk(søknad)
            lås()
        }
    }
    private fun håndterOverlappendeSøknadRevurdering(søknad: Søknad, oppdaterHistorikkBlock: (SykdomstidslinjeHendelse) -> Unit = ::oppdaterHistorikk) {
        if (søknad.delvisOverlappende(periode)) return søknad.funksjonellFeil(`Mottatt søknad som delvis overlapper`)
        if (søknad.sendtTilGosys()) return søknad.funksjonellFeil(RV_SØ_30)
        if (søknad.utenlandskSykmelding()) return søknad.funksjonellFeil(RV_SØ_29)
        if (søknad.harArbeidsdager()) søknad.varsel(RV_SØ_15)
        else {
            søknad.valider(periode, jurist())
            søknad.validerInntektskilder(vilkårsgrunnlag == null)
            søknad.info("Søknad har trigget en revurdering")
            oppdaterHistorikkBlock(søknad)
        }

        person.igangsettOverstyring(søknad, Revurderingseventyr.korrigertSøknad(skjæringstidspunkt, periode))
    }

    private fun håndterVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand, block: (VilkårsgrunnlagHistorikk.Grunnlagsdata) -> Unit = {}) {
        val grunnlagForSykepengegrunnlag = vilkårsgrunnlag.avklarSykepengegrunnlag(person, jurist())
        vilkårsgrunnlag.valider(grunnlagForSykepengegrunnlag, jurist())
        val grunnlagsdata = vilkårsgrunnlag.grunnlagsdata()
        block(grunnlagsdata)
        person.lagreVilkårsgrunnlag(grunnlagsdata)
        if (vilkårsgrunnlag.harFunksjonelleFeilEllerVerre()) {
            return forkast(vilkårsgrunnlag)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag vurdert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun håndterUtbetalingHendelse(hendelse: UtbetalingHendelse, onUtbetalt: () -> Unit) {
        if (hendelse.harFunksjonelleFeilEllerVerre()) return hendelse.funksjonellFeil(RV_UT_5)
        if (!utbetalinger.erUtbetalt()) return
        onUtbetalt()
    }

    private fun ferdigstillVedtak(hendelse: IAktivitetslogg) {
        sendVedtakFattet()
        person.gjenopptaBehandling(hendelse)
    }

    private fun trengerYtelser(hendelse: IAktivitetslogg, periode: Periode = periode()) {
        val søkevinduFamilieytelser = periode.oppdaterFom(periode.start.minusWeeks(4))
        foreldrepenger(hendelse, søkevinduFamilieytelser)
        pleiepenger(hendelse, søkevinduFamilieytelser)
        omsorgspenger(hendelse, søkevinduFamilieytelser)
        opplæringspenger(hendelse, søkevinduFamilieytelser)
        institusjonsopphold(hendelse, periode)
        arbeidsavklaringspenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
        dagpenger(hendelse, periode.start.minusMonths(2), periode.endInclusive)
        dødsinformasjon(hendelse)
    }

    private fun trengerVilkårsgrunnlag(hendelse: IAktivitetslogg) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntekterForSykepengegrunnlag(hendelse, skjæringstidspunkt, beregningSlutt.minusMonths(2), beregningSlutt)
        arbeidsforhold(hendelse, skjæringstidspunkt)
        inntekterForSammenligningsgrunnlag(hendelse, skjæringstidspunkt, beregningSlutt.minusMonths(11), beregningSlutt)
        medlemskap(hendelse, skjæringstidspunkt, periode.start, periode.endInclusive)
    }

    private fun trengerArbeidsgiveropplysninger() {
        val fastsattInntekt = person.vilkårsgrunnlagFor(skjæringstidspunkt)?.inntekt(arbeidsgiver.organisasjonsnummer())
        val arbeidsgiverperiode = finnArbeidsgiverperiode()
        val relevanteSykmeldingsperioder =
            arbeidsgiver.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).map { it.periode() }

        val forespurteOpplysninger = listOfNotNull(
            forespurtInntekt(fastsattInntekt),
            forespurtFastsattInntekt(fastsattInntekt),
            forespurtRefusjon(fastsattInntekt),
            forespurtArbeidsgiverperiode(arbeidsgiverperiode)
        )

        person.trengerArbeidsgiveropplysninger(
            PersonObserver.TrengerArbeidsgiveropplysningerEvent(
                organisasjonsnummer = organisasjonsnummer,
                sykmeldingsperioder = relevanteSykmeldingsperioder,
                vedtaksperiodeId = id,
                forespurteOpplysninger = forespurteOpplysninger
            )
        )
    }

    private fun forespurtInntekt(fastsattInntekt: Inntekt?): PersonObserver.Inntekt? {
        val beregningsmåneder = 3.downTo(1).map {
            YearMonth.from(skjæringstidspunkt).minusMonths(it.toLong())
        }
        if (fastsattInntekt == null) return PersonObserver.Inntekt(forslag = PersonObserver.Inntektsforslag(beregningsmåneder))
        return null
    }

    private fun forespurtFastsattInntekt(fastsattInntekt: Inntekt?): PersonObserver.FastsattInntekt? =
        fastsattInntekt?.let(PersonObserver::FastsattInntekt)

    private fun forespurtRefusjon(fastsattInntekt: Inntekt?): PersonObserver.Refusjon {
        if (fastsattInntekt != null) {
            val refusjonsopplysninger = person
                .vilkårsgrunnlagFor(skjæringstidspunkt)
                ?.overlappendeEllerSenereRefusjonsopplysninger(arbeidsgiver.organisasjonsnummer(), periode())
                .orEmpty()
            return PersonObserver.Refusjon(forslag = refusjonsopplysninger)

        }
        return PersonObserver.Refusjon(forslag = emptyList())
    }

    private fun forespurtArbeidsgiverperiode(arbeidsgiverperiode: Arbeidsgiverperiode?): PersonObserver.Arbeidsgiverperiode? {
        val arbeidsgiverperiodeperioder = arbeidsgiverperiode?.toList()?.grupperSammenhengendePerioder().orEmpty()
        val trengerArbeidsgiverperiode = arbeidsgiver.erFørsteSykedagEtter(periode().start, arbeidsgiverperiode)
                || arbeidsgiverperiodeperioder.maxByOrNull { it.endInclusive }?.overlapperMed(periode())
                ?: false

        if (trengerArbeidsgiverperiode) return PersonObserver.Arbeidsgiverperiode(arbeidsgiverperiodeperioder)
        return null
    }

    private fun trengerInntektsmelding() {
        if (!forventerInntekt()) return
        if (arbeidsgiver.finnVedtaksperiodeRettFør(this) != null) return
        this.person.trengerInntektsmelding(
            PersonObserver.ManglendeInntektsmeldingEvent(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = id,
                fom = this.periode.start,
                tom = this.periode.endInclusive,
                søknadIder = hendelseIder.søknadIder()
            )
        )
    }

    private fun trengerIkkeInntektsmelding() {
        this.person.trengerIkkeInntektsmelding(
            PersonObserver.TrengerIkkeInntektsmeldingEvent(
                fødselsnummer = fødselsnummer,
                aktørId = aktørId,
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = id,
                fom = this.periode.start,
                tom = this.periode.endInclusive,
                søknadIder = hendelseIder.søknadIder()
            )
        )
    }

    internal fun trengerInntektsmeldingReplay() {
        person.inntektsmeldingReplay(id, skjæringstidspunkt, organisasjonsnummer, finnArbeidsgiverperiode()?.firstOrNull())
    }

    private fun emitVedtaksperiodeVenter(
        vedtaksperiodeVenter: VedtaksperiodeVenter
    ) {
        person.vedtaksperiodeVenter(vedtaksperiodeVenter.event(aktørId, fødselsnummer))
    }

    private fun emitVedtaksperiodeEndret(
        aktivitetslogg: IAktivitetslogg,
        previousState: Vedtaksperiodetilstand = tilstand
    ) {
        val event = PersonObserver.
        VedtaksperiodeEndretEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = id,
            gjeldendeTilstand = tilstand.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = when (aktivitetslogg) {
                is Aktivitetslogg -> AktivitetsloggMap().map(aktivitetslogg)
                else -> emptyMap()
            },
            harVedtaksperiodeWarnings = person.aktivitetslogg.logg(this)
                .let { it.harVarslerEllerVerre() && !it.harFunksjonelleFeilEllerVerre() },
            hendelser = hendelseIder(),
            makstid = tilstand.makstid(LocalDateTime.now()),
            fom = periode.start,
            tom = periode.endInclusive
        )

        person.vedtaksperiodeEndret(event)
    }

    private fun sendVedtakFattet() {
        val builder = VedtakFattetBuilder(
            fødselsnummer,
            aktørId,
            organisasjonsnummer,
            id,
            periode,
            hendelseIder(),
            skjæringstidspunkt
        )
        utbetalinger.build(builder)
        person.build(skjæringstidspunkt, builder)
        person.vedtakFattet(builder.result())
    }

    private fun lagUtbetaling(hendelse: IAktivitetslogg, vedtaksperiodeSomBeregner: Vedtaksperiode, grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, maksimumSykepenger: Alder.MaksimumSykepenger, utbetalingstidslinje: Utbetalingstidslinje) {
        val utbetaling = arbeidsgiver.lagUtbetaling(
            aktivitetslogg = hendelse,
            fødselsnummer = fødselsnummer,
            orgnummerTilDenSomBeregner = vedtaksperiodeSomBeregner.organisasjonsnummer,
            utbetalingstidslinje = utbetalingstidslinje,
            maksdato = maksimumSykepenger.sisteDag(),
            forbrukteSykedager = maksimumSykepenger.forbrukteDager(),
            gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(),
            periode = periode
        )
        nyUtbetaling(grunnlagsdata, utbetaling)
    }

    private fun nyUtbetaling(grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, utbetaling: Utbetaling) {
        utbetalingstidslinje = utbetalinger.nyUtbetaling(id, grunnlagsdata, periode, utbetaling)
    }

    private fun høstingsresultater(hendelse: ArbeidstakerHendelse, simuleringtilstand: Vedtaksperiodetilstand, godkjenningtilstand: Vedtaksperiodetilstand) = when {
        utbetalinger.harUtbetalinger() -> tilstand(hendelse, simuleringtilstand) {
            hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
        }
        else -> tilstand(hendelse, godkjenningtilstand) {
            hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
        }
    }

    internal fun lagRevurdering(aktivitetslogg: IAktivitetslogg, vedtaksperiodeSomBeregner: Vedtaksperiode, grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, maksimumSykepenger: Alder.MaksimumSykepenger, utbetalingstidslinje: Utbetalingstidslinje) {
        val utbetaling = arbeidsgiver.lagRevurdering(
            aktivitetslogg = aktivitetslogg,
            fødselsnummer = fødselsnummer,
            orgnummerTilDenSomBeregner = vedtaksperiodeSomBeregner.organisasjonsnummer,
            utbetalingstidslinje = utbetalingstidslinje,
            maksdato = maksimumSykepenger.sisteDag(),
            forbrukteSykedager = maksimumSykepenger.forbrukteDager(),
            gjenståendeSykedager = maksimumSykepenger.gjenståendeDager(),
            periode = periode
        )

        // finner vedtaksperiodene for samme ag som oss selv, som også skal få revurderingen
        val filter: VedtaksperiodeFilter = if (vedtaksperiodeSomBeregner.arbeidsgiver === this.arbeidsgiver)
            { it -> it.tilstand == AvventerGjennomførtRevurdering }
        else
            { it -> it !== this &&
                    it.tilstand == AvventerRevurdering && it.skjæringstidspunkt == this.skjæringstidspunkt
                    // deler skjæringstidspunkt, men ikke utbetaling (Infotrygdperiode mellom)
                    && !it.utbetalinger.hørerIkkeSammenMed(utbetaling) }

        // fordel revurderingen til alle som venter, inkl. oss selv
        listOf(arbeidsgiver)
            .vedtaksperioder(filter)
            .plus(this)
            .forEach {
                it.aktivitetsloggkopi(aktivitetslogg).info("Mottar revurdert utbetaling fra $this som følge av at $vedtaksperiodeSomBeregner for ${vedtaksperiodeSomBeregner.organisasjonsnummer} beregner")
                it.nyUtbetaling(grunnlagsdata, utbetaling)
            }
        loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(vedtaksperiodeSomBeregner.organisasjonsnummer, aktivitetslogg)
    }

    private fun loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(orgnummer: String, aktivitetslogg: IAktivitetslogg) {
        if (!utbetalinger.trekkerTilbakePenger()) return

        if (orgnummer != organisasjonsnummer || person.blitt6GBegrensetSidenSist(skjæringstidspunkt)) {
            aktivitetslogg.info("En endring hos en arbeidsgiver har medført at det trekkes tilbake penger hos andre arbeidsgivere")
        }
    }

    private fun Vedtaksperiodetilstand.påminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
        if (!påminnelse.gjelderTilstand(type)) return vedtaksperiode.person.vedtaksperiodeIkkePåminnet(id, organisasjonsnummer, type)
        vedtaksperiode.person.vedtaksperiodePåminnet(id, organisasjonsnummer, påminnelse)
        if (påminnelse.nåddMakstid(::makstid)) return håndterMakstid(vedtaksperiode, påminnelse)
        håndter(vedtaksperiode, påminnelse)
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    private fun finnArbeidsgiverperiode() = arbeidsgiver.arbeidsgiverperiode(periode)

    private fun forventerInntekt(): Boolean {
        return Arbeidsgiverperiode.forventerInntekt(finnArbeidsgiverperiode(), periode, sykdomstidslinje, jurist())
    }

    private fun sendOppgaveEvent() {
        if (!skalOppretteOppgave()) return
        val inntektsmeldingIds =
            arbeidsgiver.finnSammenhengendePeriode(skjæringstidspunkt)
                .mapNotNull { it.inntektsmeldingInfo }.ider()
        person.sendOppgaveEvent(
            periode = periode(),
            hendelseIder = hendelseIder() + inntektsmeldingIds
        )
    }

    private fun skalOppretteOppgave() =
        inntektsmeldingInfo != null ||
                arbeidsgiver.finnSammenhengendePeriode(skjæringstidspunkt)
                    .any { it.inntektsmeldingInfo != null } ||
                sykdomstidslinje.any { it.kommerFra(Søknad::class) }

    private fun loggInnenforArbeidsgiverperiode() {
        if (forventerInntekt()) return
        sikkerlogg.info(
            "Vedtaksperioden {} for {} er egentlig innenfor arbeidsgiverperioden ved {}",
            keyValue("vedtaksperiodeId", id), keyValue("fnr", fødselsnummer), keyValue("tilstand", tilstand.type)
        )
    }

    private fun kopierManglende(other: InntektsmeldingInfo) {
        this.inntektsmeldingInfo = other
        other.leggTil(this.hendelseIder)
    }

    private fun trengerGodkjenning(hendelse: IAktivitetslogg) {
        val periodetype = arbeidsgiver.periodetype(periode)
        utbetalinger.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            førstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING,
            inntektskilde = requireNotNull(person.vilkårsgrunnlagFor(skjæringstidspunkt)?.inntektskilde()),
            orgnummereMedRelevanteArbeidsforhold = person.relevanteArbeidsgivere(skjæringstidspunkt)
        )
    }

    internal fun gjenopptaBehandling(hendelse: IAktivitetslogg, arbeidsgivere: Iterable<Arbeidsgiver>) {
        hendelse.kontekst(arbeidsgiver)
        kontekst(hendelse)
        hendelse.info("Forsøker å gjenoppta $this")
        tilstand.gjenopptaBehandling(this, arbeidsgivere, hendelse)
    }


    fun igangsettRevurdering(hendelse: IAktivitetslogg) {
        check(tilstand == AvventerGjennomførtRevurdering){ "Må være i AvventerGjennomførtRevurdering for å igangsette" }
        hendelse.kontekst(arbeidsgiver)
        kontekst(hendelse)
        tilstand(hendelse, AvventerHistorikkRevurdering)
    }

    internal fun gjenopptaRevurdering(hendelse: IAktivitetslogg, første: Vedtaksperiode) {
        hendelse.kontekst(arbeidsgiver)
        kontekst(hendelse)
        if (this.skjæringstidspunkt != første.skjæringstidspunkt) return
        if (this.utbetalinger.hørerIkkeSammenMed(første.utbetalinger)) return
        this.tilstand.gjenopptaRevurdering(this, hendelse, første)
    }

    internal fun ferdigstillRevurdering(hendelse: IAktivitetslogg, ferdigstiller: Vedtaksperiode) {
        if (ferdigstiller.skjæringstidspunkt != this.skjæringstidspunkt || ferdigstiller === this) return
        kontekst(hendelse)
        tilstand.ferdigstillRevurdering(this, hendelse)
    }

    internal fun igangsettOverstyring(hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
        if (revurdering.ikkeRelevant(periode, skjæringstidspunkt)) return
        kontekst(hendelse)
        tilstand.igangsettOverstyring(this, hendelse, revurdering)
    }

    private fun varsleForrigePeriodeRevurderingOmgjort(hendelse: IAktivitetslogg) {
        arbeidsgiver.finnVedtaksperiodeFør(this)?.revurderingOmgjort(hendelse.barn(), skjæringstidspunkt)
    }

    private fun revurderingOmgjort(hendelse: IAktivitetslogg, skjæringstidspunkt: LocalDate) {
        if (this.skjæringstidspunkt != skjæringstidspunkt) return
        varsleForrigePeriodeRevurderingOmgjort(hendelse)
        kontekst(hendelse)
        tilstand.revurderingOmgjort(this, hendelse)
    }

    internal fun inngåIRevurderingseventyret(
        vedtaksperioder: MutableList<PersonObserver.OverstyringIgangsatt.VedtaksperiodeData>,
        typeEndring: String
    ) {
        vedtaksperioder.add(
            PersonObserver.OverstyringIgangsatt.VedtaksperiodeData(
                orgnummer = organisasjonsnummer,
                vedtaksperiodeId = id,
                skjæringstidspunkt = skjæringstidspunkt,
                periode = periode,
                typeEndring = typeEndring
            )
        )
    }

    internal fun håndtertInntektPåSkjæringstidspunktet(hendelse: SykdomstidslinjeHendelse) {
        inntektsmeldingHåndtert(hendelse)
        tilstand.håndtertInntektPåSkjæringstidspunktet(this, hendelse)
    }

    private fun vedtaksperiodeVenter(påminnelse: Påminnelse, venterPå: Vedtaksperiode? = person.nestemann()) {
        if (venterPå == null) return sikkerlogg.warn("Hvordan kan det ha seg at {} for {} ikke er nestemann?", keyValue("vedtaksperiodeId", id), keyValue("aktørId", aktørId))
        val builder = VedtaksperiodeVenter.Builder()
        val venteårsak = person.venteårsak(venterPå, påminnelse) ?: return
        builder.venterPå(venterPå.id, venterPå.organisasjonsnummer, venteårsak)
        påminnelse.venter(builder, tilstand::makstid)
        builder.hendelseIder(hendelseIder())
        val vedtaksperiodeVenter = builder.build()
        sikkerlogg.info("$vedtaksperiodeVenter", keyValue("aktørId", aktørId))
        emitVedtaksperiodeVenter(vedtaksperiodeVenter)
    }

    internal fun venteårsak(hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
        tilstand.venteårsak(this, hendelse, arbeidsgivere)

    fun slutterEtter(dato: LocalDate) = periode.slutterEtter(dato)

    private fun aktivitetsloggkopi(hendelse: IAktivitetslogg) =
        hendelse.barn().also { kopi ->
            this.kontekst(kopi)
        }

    private fun beregnUtbetalinger(
        hendelse: PersonHendelse,
        arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
        beregningsperiode: Periode,
        beregningsperioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>,
        utbetalingsperioder: List<Vedtaksperiode>,
        utbetalingStrategy: (Vedtaksperiode, IAktivitetslogg, Vedtaksperiode, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, Alder.MaksimumSykepenger, Utbetalingstidslinje) -> Unit
    ): Boolean {
        check(utbetalingsperioder.all { it.skjæringstidspunkt == this.skjæringstidspunkt }) {
            "ugyldig situasjon: skal beregne utbetaling for vedtaksperioder med ulike skjæringstidspunkter"
        }
        val grunnlagsdata = checkNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for ${skjæringstidspunkt}, men har ikke. Lages det utbetaling for en periode som ikke skal lage utbetaling?"
        }
        try {
            val (maksimumSykepenger, tidslinjerPerArbeidsgiver) = arbeidsgiverUtbetalinger.beregn(skjæringstidspunkt, beregningsperiode, beregningsperioder)
            utbetalingsperioder.forEach {
                val utbetalingstidslinje = tidslinjerPerArbeidsgiver.getValue(it.arbeidsgiver)
                utbetalingStrategy(it, it.aktivitetsloggkopi(hendelse), this, grunnlagsdata, maksimumSykepenger, utbetalingstidslinje)
            }
        } catch (err: UtbetalingstidslinjeBuilderException) {
            err.logg(hendelse)
        }
        return !hendelse.harFunksjonelleFeilEllerVerre()
    }

    internal fun sykefraværsfortelling(list: List<Sykefraværstilfelleeventyr>) =
        list.bliMed(this.id, this.organisasjonsnummer, this.periode)

    // Gang of four State pattern
    internal sealed interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType
        val erFerdigBehandlet: Boolean get() = false

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        fun håndterMakstid(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            påminnelse.funksjonellFeil(RV_VT_1)
            vedtaksperiode.forkast(påminnelse)
        }

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand", mapOf(
                    "tilstand" to type.name
                )
            )
        }

        // Gitt at du er nestemann som skal behandles - hva venter du på?
        fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>): Venteårsak?

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad)

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {}
        fun håndterDagerFør(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {}
        fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            val arbeidsgiverperiodeFør = vedtaksperiode.finnArbeidsgiverperiode()
            vedtaksperiode.håndterDager(dager)
            val arbeidsgiverperiodeEtter = vedtaksperiode.finnArbeidsgiverperiode()
            if (!arbeidsgiverperiodeFør.sammenlign(arbeidsgiverperiodeEtter)) {
                // Hvis AGP er uendret så legger vi ikke til varsel om at det er mottatt flere inntektsmeldinger
                // Det kan derimot være at inntekt- og refusjon legger på varselet
                dager.varsel(RV_IM_4, "Endrer arbeidsgiverperiode etter håndtering av dager fra inntektsmelding i ${type.name} ")
            }
            return true
        }
        fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            inntektOgRefusjon.varsel(RV_IM_4, "Håndterer inntekt og refusjon fra inntektsmelding i ${type.name}")
        }

        fun håndtertInntektPåSkjæringstidspunktet(vedtaksperiode: Vedtaksperiode, hendelse: SykdomstidslinjeHendelse) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.info("Forventet ikke vilkårsgrunnlag i %s".format(type.name))
            vilkårsgrunnlag.funksjonellFeil(RV_VT_2)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {}

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: UtbetalingshistorikkEtterInfotrygdendring,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.utbetalingshistorikkEtterInfotrygdendring(
                vedtaksperiode.id,
                vedtaksperiode.periode,
                vedtaksperiode.tilstand.type.toString(),
                vedtaksperiode.organisasjonsnummer,
                vedtaksperiode.person
            )
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            arbeidsgiverUtbetalingerFun: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
        ) {
            ytelser.info("Forventet ikke ytelsehistorikk i %s".format(type.name))
            ytelser.funksjonellFeil(RV_VT_7)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            utbetalingsgodkjenning.info("Forventet ikke utbetalingsgodkjenning i %s".format(type.name))
            utbetalingsgodkjenning.funksjonellFeil(RV_VT_3)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            simulering.info("Forventet ikke simulering i %s".format(type.name))
            simulering.funksjonellFeil(RV_VT_4)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            hendelse.info("Forventet ikke utbetaling i %s".format(type.name))
            hendelse.funksjonellFeil(RV_VT_5)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Forventet ikke overstyring fra saksbehandler i %s".format(type.name))
            hendelse.funksjonellFeil(RV_VT_6)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrArbeidsgiveropplysninger) {}

        fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            hendelse.info("Tidligere periode ferdigbehandlet, men gjør ingen tilstandsendring.")
        }

        fun gjenopptaRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, første: Vedtaksperiode) {}

        fun ferdigstillRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
        }

        fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!revurdering.inngåSomRevurdering(hendelse, vedtaksperiode)) return
            vedtaksperiode.varsleForrigePeriodeRevurderingOmgjort(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        fun revurderingOmgjort(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            HJELP.utenBegrunnelse

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            val harSenereUtbetalinger = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING(vedtaksperiode)).isNotEmpty()
            val harSenereAUU = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_UTEN_UTBETALING(vedtaksperiode)).isNotEmpty()
            if (harSenereUtbetalinger || harSenereAUU) {
                søknad.varsel(RV_OO_1)
            }
            vedtaksperiode.håndterSøknad(søknad) {
                val rettFør = vedtaksperiode.arbeidsgiver.finnVedtaksperiodeRettFør(vedtaksperiode)
                when {
                    rettFør != null && rettFør.tilstand !in setOf(AvsluttetUtenUtbetaling, AvventerInfotrygdHistorikk, AvventerInntektsmelding) -> AvventerBlokkerendePeriode
                    else -> AvventerInfotrygdHistorikk
                }
            }
            søknad.info("Fullført behandling av søknad")
            if (!søknad.harFunksjonelleFeilEllerVerre()) {
                vedtaksperiode.person.igangsettOverstyring(
                    søknad,
                    Revurderingseventyr.nyPeriode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode, vedtaksperiode.forventerInntekt())
                )
            }
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            throw IllegalStateException("Har startet revurdering før den nyopprettede perioden har håndtert søknaden")
        }
    }

    internal object AvventerInfotrygdHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_INFOTRYGDHISTORIKK
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.trengerHistorikkFraInfotrygd(hendelse, vedtaksperiode)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = null
        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            vedtaksperiode.person.trengerHistorikkFraInfotrygd(hendelse, vedtaksperiode)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            validation(hendelse) {
                onValidationFailed { vedtaksperiode.forkast(hendelse) }
                valider {
                    infotrygdhistorikk.valider(
                        this,
                        vedtaksperiode.periode,
                        vedtaksperiode.skjæringstidspunkt,
                        arbeidsgiver.organisasjonsnummer()
                    )
                }
                onSuccess {
                    if (vedtaksperiode.vilkårsgrunnlag is InfotrygdVilkårsgrunnlag) {
                        info("Oppdaget at perioden startet i infotrygd")
                        vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
                    } else {
                        vedtaksperiode.tilstand(hendelse, AvventerInntektsmelding)
                    }
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.person.trengerHistorikkFraInfotrygd(påminnelse, vedtaksperiode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun igangsettOverstyring(
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            revurdering: Revurderingseventyr
        ) {
        }
    }

    internal object AvventerRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(hendelse)) {
                hendelse.info("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding")
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>): Venteårsak? {
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(hendelse))
                return INNTEKTSMELDING fordi MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_SAMME_ARBEIDSGIVER
            if (!harTilstrekkeligInformasjonTilUtbetaling(vedtaksperiode, arbeidsgivere, hendelse))
                return INNTEKTSMELDING fordi MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_ANDRE_ARBEIDSGIVERE
            return null
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(hendelse))
                return hendelse.info("Mangler nødvendig inntekt for vilkårsprøving eller refusjonsopplysninger og kan derfor ikke gjenoppta revurdering.")
            if (!harTilstrekkeligInformasjonTilUtbetaling(vedtaksperiode, arbeidsgivere, hendelse))
                return hendelse.info("En annen arbeidsgiver mangler nødvendig inntekt for vilkårsprøving eller refusjonsopplysninger og kan derfor ikke gjenoppta revurdering.")
            vedtaksperiode.tilstand(hendelse, AvventerGjennomførtRevurdering)
            vedtaksperiode.arbeidsgiver.gjenopptaRevurdering(vedtaksperiode, hendelse)
        }

        private fun harTilstrekkeligInformasjonTilUtbetaling(vedtaksperiode: Vedtaksperiode, arbeidsgivere: Iterable<Arbeidsgiver>, hendelse: IAktivitetslogg): Boolean {
            val skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
            val periode = arbeidsgivere.vedtaksperioder { other -> other.skjæringstidspunkt == skjæringstidspunkt && other.tilstand == AvventerRevurdering }.periode()
            return arbeidsgivere.harTilstrekkeligInformasjonTilUtbetaling(skjæringstidspunkt, periode, hendelse)
        }

        override fun gjenopptaRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, første: Vedtaksperiode) {
            hendelse.info("$vedtaksperiode blir med i revurderingen igangsatt av $første")
            vedtaksperiode.tilstand(hendelse, AvventerGjennomførtRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            vedtaksperiode.håndterUtbetalingHendelse(hendelse) {
                vedtaksperiode.ferdigstillVedtak(hendelse)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            inntektOgRefusjon.wrap { vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon) }
            vedtaksperiode.trengerIkkeInntektsmelding()
            vedtaksperiode.person.gjenopptaBehandling(inntektOgRefusjon)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.vedtaksperiodeVenter(påminnelse)
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(påminnelse)) {
                påminnelse.info("Varsler arbeidsgiver at vi har behov for inntektsmelding.")
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }
    }

    internal object AvventerGjennomførtRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GJENNOMFØRT_REVURDERING

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            HJELP.utenBegrunnelse

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            hendelse.info("Gjenopptar ikke behandling fordi perioden avventer på at revurderingen ferdigstilles.")
        }

        override fun revurderingOmgjort(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        override fun ferdigstillRevurdering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (feiletRevurdering(vedtaksperiode)) {
                hendelse.info("Går til revurdering feilet fordi revurdering er avvist")
                return vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
            }

            hendelse.info("Går til avsluttet fordi revurdering er fullført via en annen vedtaksperiode")
            vedtaksperiode.tilstand(hendelse, Avsluttet)
        }

        private fun feiletRevurdering(vedtaksperiode: Vedtaksperiode) =
            vedtaksperiode.utbetalinger.erAvvist() || vedtaksperiode.arbeidsgiver.feiletRevurdering(vedtaksperiode)

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            hendelse.info("Kvittering håndteres av vedtaksperioden som har håndtert utbetaling av revurderingen.")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }
    }

    internal object AvventerHistorikkRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK_REVURDERING

        override fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
            val periode = vedtaksperiode.sykefraværstilfelle()
            vedtaksperiode.trengerYtelser(hendelse, periode)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            BEREGNING fordi OVERSTYRING_IGANGSATT

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes())
                return vedtaksperiode.person.igangsettOverstyring(påminnelse, Revurderingseventyr.reberegning(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
            vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            val periode = vedtaksperiode.sykefraværstilfelle()
            vedtaksperiode.trengerYtelser(påminnelse, periode)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            arbeidsgiverUtbetalingerFun: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
        ) {
            val vilkårsgrunnlag = vedtaksperiode.vilkårsgrunnlag ?: return vedtaksperiode.tilstand(ytelser, AvventerVilkårsprøvingRevurdering) {
                ytelser.info("Trenger å utføre vilkårsprøving før vi kan beregne utbetaling for revurderingen.")
            }

            FunksjonelleFeilTilVarsler.wrap(ytelser) {
                // beregningsperiode brukes for å avgjøre hvilke perioder vi skal -kreve- inntekt for
                // beregningsperioder brukes for å lage varsel
                val utvalg = beregningsperioder(person, vedtaksperiode)

                utvalg.forEach {
                    infotrygdhistorikk.valider(it.aktivitetsloggkopi(ytelser), it.periode, it.skjæringstidspunkt, it.organisasjonsnummer)
                    it.kontekst(ytelser) // overskriver kontekst for ytelser-hendelsen
                    ytelser.valider(it.periode, it.skjæringstidspunkt)
                    vedtaksperiode.kontekst(ytelser) // endre kontekst tilbake for ytelser-hendelsen
                }
                person.valider(ytelser, vilkårsgrunnlag, vedtaksperiode.organisasjonsnummer, vedtaksperiode.skjæringstidspunkt)

                val beregningsperiode = utvalg.periode()
                val beregningsperioder = utvalg.map { Triple(it.periode, it.aktivitetsloggkopi(ytelser), it.jurist) }

                // utbetalingsperioder brukes for å lage revurderinger
                val utbetalingsperioder = listOf(vedtaksperiode) + person.nåværendeVedtaksperioder {
                    // perioder som er sist på skjæringstidspunktet per arbeidsgiver (hensyntatt pingpong)
                    it.arbeidsgiver !== vedtaksperiode.arbeidsgiver
                            && it.tilstand == AvventerRevurdering
                            && it.arbeidsgiver.finnVedtaksperiodeRettEtter(it) == null
                            && it.skjæringstidspunkt == vedtaksperiode.skjæringstidspunkt
                }

                val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                vedtaksperiode.beregnUtbetalinger(
                    ytelser,
                    arbeidsgiverUtbetalinger,
                    beregningsperiode,
                    beregningsperioder,
                    utbetalingsperioder,
                    Vedtaksperiode::lagRevurdering
                )

                vedtaksperiode.høstingsresultater(ytelser, AvventerSimuleringRevurdering, AvventerGodkjenningRevurdering)
            }
        }

        private fun beregningsperioder(person: Person, vedtaksperiode: Vedtaksperiode) =
            (listOf(vedtaksperiode) + person
                .vedtaksperioder {
                    it.tilstand in listOf(AvventerGjennomførtRevurdering, AvventerRevurdering)
                            && it.skjæringstidspunkt == vedtaksperiode.skjæringstidspunkt
                })

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }
    }

    internal object AvventerVilkårsprøvingRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = null
        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            FunksjonelleFeilTilVarsler.wrap(vilkårsgrunnlag) { vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikkRevurdering) }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

    }

    internal object AvventerInntektsmelding : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_INNTEKTSMELDING
        override fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(180)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if(Toggle.Splarbeidsbros.enabled) {
                vedtaksperiode.trengerArbeidsgiveropplysninger()
            }
            vedtaksperiode.trengerInntektsmeldingReplay()
            vedtaksperiode.trengerInntektsmelding()
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            INNTEKTSMELDING.utenBegrunnelse

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
            vedtaksperiode.trengerIkkeInntektsmelding()
            vedtaksperiode.person.trengerIkkeInntektsmeldingReplay(vedtaksperiode.id)
        }

        override fun håndterDagerFør(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            dager.leggTilArbeidsdagerFør(vedtaksperiode.periode.start)
            vedtaksperiode.periode = dager.oppdatertFom(vedtaksperiode.periode)
            dager.håndterPeriodeRettFør(vedtaksperiode.periode, vedtaksperiode.arbeidsgiver)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            vedtaksperiode.håndterDager(dager)
            if (vedtaksperiode.forventerInntekt()) return true
            vedtaksperiode.tilstand(dager, AvsluttetUtenUtbetaling)
            return true
        }

        private fun tilstandEtterInntektPåSkjæringstidspunkt(vedtaksperiode: Vedtaksperiode) = when {
            !vedtaksperiode.forventerInntekt() -> AvsluttetUtenUtbetaling
            !vedtaksperiode.arbeidsgiver.kanBeregneSykepengegrunnlag(vedtaksperiode.skjæringstidspunkt) -> AvventerInntektsmelding.also {
                sikkerlogg.info("Har lagret inntekt, men kan ikke beregne sykepengegrunnlag på skjæringstidspunkt ${vedtaksperiode.skjæringstidspunkt} for arbeidsgiver ${vedtaksperiode.arbeidsgiver.organisasjonsnummer()}")
            }
            else -> AvventerBlokkerendePeriode
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon, tilstandEtterInntektPåSkjæringstidspunkt(vedtaksperiode))
        }

        override fun håndtertInntektPåSkjæringstidspunktet(vedtaksperiode: Vedtaksperiode, hendelse: SykdomstidslinjeHendelse) {
            vedtaksperiode.tilstand(hendelse, tilstandEtterInntektPåSkjæringstidspunkt(vedtaksperiode))
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: UtbetalingshistorikkEtterInfotrygdendring,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            super.håndter(vedtaksperiode, hendelse, infotrygdhistorikk)
            validation(hendelse) {
                onValidationFailed { vedtaksperiode.forkast(hendelse) }
                valider {
                    infotrygdhistorikk.valider(this, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.organisasjonsnummer)
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vurderOmKanGåVidere(vedtaksperiode, påminnelse)
            if (vedtaksperiode.tilstand == AvventerInntektsmelding) {
                vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            }
        }

        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, arbeidsgivere: Iterable<Arbeidsgiver>, hendelse: IAktivitetslogg) {
            vurderOmKanGåVidere(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {
            vurderOmKanGåVidere(vedtaksperiode, inntektsmeldingReplayUtført)
        }

        private fun vurderOmKanGåVidere(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (!vedtaksperiode.forventerInntekt()) return vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(hendelse)) return
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }
    }

    internal object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_BLOKKERENDE_PERIODE
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>): Venteårsak? {
            return tilstand(vedtaksperiode, arbeidsgivere, hendelse).venteårsak()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) = tilstand(vedtaksperiode, arbeidsgivere, hendelse).gjenopptaBehandling(vedtaksperiode, hendelse)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            super.håndter(vedtaksperiode, inntektOgRefusjon)
            vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon)
            vedtaksperiode.person.gjenopptaBehandling(inntektOgRefusjon)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.vedtaksperiodeVenter(påminnelse)
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }
        private fun tilstand(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) = when {
            arbeidsgivere.avventerSøknad(vedtaksperiode.periode) -> AvventerTidligereEllerOverlappendeSøknad
            !vedtaksperiode.forventerInntekt() -> ForventerIkkeInntekt
            vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() -> ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag
            !vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) -> ManglerNødvendigInntektForVilkårsprøving
            !arbeidsgivere.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) -> ManglerNødvendigInntektForVilkårsprøvingForAndreArbeidsgivere
            arbeidsgivere.trengerInntektsmelding(vedtaksperiode.periode) -> TrengerInntektsmelding
            !arbeidsgivere.harNødvendigRefusjonsopplysninger(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode, hendelse) -> ManglerNødvendigRefusjonsopplysninger
            vedtaksperiode.vilkårsgrunnlag == null -> KlarForVilkårsprøving
            else -> KlarForBeregning
        }

        private sealed interface Tilstand {
            fun venteårsak(): Venteårsak? = null
            fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg)
        }
        private object AvventerTidligereEllerOverlappendeSøknad: Tilstand {
            override fun venteårsak() = SØKNAD fordi HAR_SYKMELDING_SOM_OVERLAPPER_PÅ_ANDRE_ARBEIDSGIVERE
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                hendelse.info("Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding som er før eller overlapper med vedtaksperioden")
            }
        }
        private object ForventerIkkeInntekt: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
            }
        }
        private object ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                hendelse.funksjonellFeil(RV_SV_2)
                vedtaksperiode.forkast(hendelse)
            }
        }
        private object ManglerNødvendigInntektForVilkårsprøving: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                hendelse.info("Mangler inntekt for sykepengegrunnlag som følge av at skjæringstidspunktet har endret seg")
                vedtaksperiode.tilstand(hendelse, AvventerInntektsmelding)
            }
        }
        private object ManglerNødvendigInntektForVilkårsprøvingForAndreArbeidsgivere: Tilstand {
            override fun venteårsak() = INNTEKTSMELDING fordi MANGLER_INNTEKT_FOR_VILKÅRSPRØVING_PÅ_ANDRE_ARBEIDSGIVERE
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                hendelse.info("Gjenopptar ikke behandling fordi minst én arbeidsgiver ikke har tilstrekkelig inntekt for skjæringstidspunktet")
            }
        }
        private object TrengerInntektsmelding: Tilstand {
            override fun venteårsak() = INNTEKTSMELDING fordi MANGLER_REFUSJONSOPPLYSNINGER_PÅ_ANDRE_ARBEIDSGIVERE
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                hendelse.info("Gjenopptar ikke behandling fordi minst én overlappende periode venter på nødvendig opplysninger fra arbeidsgiver")
            }
        }
        private object ManglerNødvendigRefusjonsopplysninger: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                hendelse.funksjonellFeil(RV_RE_2)
                vedtaksperiode.forkast(hendelse)
            }
        }
        private object KlarForVilkårsprøving: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøving)
            }
        }
        private object KlarForBeregning: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
            }
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING
        override fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(5)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = null

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikk) { grunnlagsdata ->
                grunnlagsdata.validerFørstegangsvurdering(vilkårsgrunnlag)
            }
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            super.håndter(vedtaksperiode, inntektOgRefusjon)
            vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon, AvventerBlokkerendePeriode)
        }
    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK

        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Forventer vilkårsgrunnlag for å beregne utbetaling" }
            vedtaksperiode.loggInnenforArbeidsgiverperiode()
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            BEREGNING.utenBegrunnelse

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            vedtaksperiode.trengerYtelser(påminnelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            arbeidsgiverUtbetalingerFun: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
        ) {
            validation(ytelser) {
                onValidationFailed {
                    if (!ytelser.harFunksjonelleFeilEllerVerre()) funksjonellFeil(RV_AY_10)
                    vedtaksperiode.forkast(ytelser)
                }
                onSuccess {
                    vedtaksperiode.skjæringstidspunktFraInfotrygd = person.skjæringstidspunkt(vedtaksperiode.sykdomstidslinje.sykdomsperiode() ?: vedtaksperiode.periode)
                }
                valider {
                    infotrygdhistorikk.valider(
                        this,
                        vedtaksperiode.periode,
                        vedtaksperiode.skjæringstidspunkt,
                        vedtaksperiode.organisasjonsnummer
                    )
                }
                valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }

                // skal ikke mangle vilkårsgrunnlag her med mindre skjæringstidspunktet har endret seg som følge
                // av historikk fra IT
                valider(Varselkode.RV_IT_33) {
                    (vedtaksperiode.vilkårsgrunnlag != null).also {
                        if (!it) info("Mangler vilkårsgrunnlag for ${vedtaksperiode.skjæringstidspunkt}")
                    }
                }

                lateinit var vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
                onSuccess {
                    vilkårsgrunnlag = requireNotNull(vedtaksperiode.vilkårsgrunnlag)
                    ytelser.kontekst(vilkårsgrunnlag)
                }
                valider {
                    person.valider(this, vilkårsgrunnlag, vedtaksperiode.organisasjonsnummer, vedtaksperiode.skjæringstidspunkt)
                }
                onSuccess {
                    if (vedtaksperiode.inntektsmeldingInfo == null) {
                        arbeidsgiver.finnTidligereInntektsmeldinginfo(vedtaksperiode.skjæringstidspunkt)?.also { vedtaksperiode.kopierManglende(it) }
                    }
                }
                lateinit var arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
                valider(RV_UT_16) {
                    arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                    val beregningsperiode = vedtaksperiode.finnArbeidsgiverperiode()?.periode(vedtaksperiode.periode.endInclusive) ?: vedtaksperiode.periode
                    val beregningsperioder = listOf(Triple(vedtaksperiode.periode, this, vedtaksperiode.jurist()))
                    val utbetalingsperioder = person
                        .nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
                        .filter { vedtaksperiode.periode.overlapperMed(it.periode)
                                && vedtaksperiode.skjæringstidspunkt == it.skjæringstidspunkt }

                    vedtaksperiode.beregnUtbetalinger(
                        ytelser,
                        arbeidsgiverUtbetalinger,
                        beregningsperiode,
                        beregningsperioder,
                        utbetalingsperioder,
                        Vedtaksperiode::lagUtbetaling
                    )
                }
                onSuccess {
                    vedtaksperiode.høstingsresultater(ytelser, AvventerSimulering, AvventerGodkjenning)
                }
            }
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING

        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            UTBETALING.utenBegrunnelse

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            if (vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return
            aktivitetslogg.info("Forkaster utbetalingen fordi utbetalingen er ikke simulert, " +
                    "og perioden endrer tilstand")
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
            vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            vedtaksperiode.utbetalinger.valider(simulering)
            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return simulering.info("Kan ikke gå videre da begge oppdragene ikke er simulert.")
            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.simuler(hendelse)
        }
    }

    internal object AvventerSimuleringRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.simuler(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            UTBETALING fordi OVERSTYRING_IGANGSATT

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            if (vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return
            aktivitetslogg.info("Forkaster utbetalingen fordi utbetalingen er ikke simulert, " +
                    "og perioden endrer tilstand")
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerHistorikkRevurdering) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
            vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            vedtaksperiode.utbetalinger.simuler(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            FunksjonelleFeilTilVarsler.wrap(simulering) {
                vedtaksperiode.utbetalinger.valider(simulering)
            }
            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return simulering.info("Kan ikke gå videre da begge oppdragene ikke er simulert.")
            vedtaksperiode.tilstand(simulering, AvventerGodkjenningRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = GODKJENNING.utenBegrunnelse


        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
            if (søknad.harFunksjonelleFeilEllerVerre()) return
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            if (vedtaksperiode.utbetalinger.erAvvist()) return vedtaksperiode.forkast(utbetalingsgodkjenning)
            vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                when {
                    vedtaksperiode.utbetalinger.harUtbetalinger() -> TilUtbetaling
                    else -> Avsluttet
                }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
            vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            vedtaksperiode.trengerGodkjenning(påminnelse)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: UtbetalingshistorikkEtterInfotrygdendring,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            super.håndter(vedtaksperiode, hendelse, infotrygdhistorikk)
            if (vedtaksperiode.utbetalinger.erHistorikkEndretSidenBeregning(infotrygdhistorikk)) return vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
            }
        }
        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            revurdering.inngåSomEndring(hendelse, vedtaksperiode)
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

    }

    internal object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) =
            GODKJENNING fordi OVERSTYRING_IGANGSATT

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerHistorikkRevurdering) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
            vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            vedtaksperiode.trengerGodkjenning(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            if (vedtaksperiode.utbetalinger.erAvvist()) {
                utbetalingsgodkjenning.varsel(RV_UT_1)
                if (utbetalingsgodkjenning.automatiskBehandling()) {
                    utbetalingsgodkjenning.info("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
                    return sikkerlogg.error("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
                }
            }
            vedtaksperiode.tilstand(utbetalingsgodkjenning, when {
                vedtaksperiode.utbetalinger.erAvvist() -> RevurderingFeilet
                vedtaksperiode.utbetalinger.harUtbetalinger() -> TilUtbetaling
                else -> Avsluttet
            })
        }
        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: UtbetalingshistorikkEtterInfotrygdendring,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            super.håndter(vedtaksperiode, hendelse, infotrygdhistorikk)
            if (vedtaksperiode.utbetalinger.erHistorikkEndretSidenBeregning(infotrygdhistorikk)) return vedtaksperiode.tilstand(
                hendelse,
                AvventerHistorikkRevurdering
            ) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = UTBETALING.utenBegrunnelse

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            vedtaksperiode.håndterUtbetalingHendelse(hendelse) {
                vedtaksperiode.tilstand(hendelse, Avsluttet) {
                    hendelse.info("OK fra Oppdragssystemet")
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            when {
                vedtaksperiode.utbetalinger.erUbetalt() -> vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode)
                vedtaksperiode.utbetalinger.erUtbetalt() -> vedtaksperiode.tilstand(påminnelse, Avsluttet)
                else -> vedtaksperiode.vedtaksperiodeVenter(påminnelse, vedtaksperiode)
            }
        }
    }

    internal object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING

        override val erFerdigBehandlet = true

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.lås()
            check(!vedtaksperiode.utbetalinger.harUtbetaling()) { "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. " }
            vedtaksperiode.ferdigstillVedtak(hendelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.låsOpp()
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = HJELP.utenBegrunnelse

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!vedtaksperiode.forventerInntekt()) return
            revurdering.loggDersomKorrigerendeSøknad(hendelse, "Startet omgjøring grunnet korrigerende søknad")
            hendelse.info(RV_RV_1.varseltekst)
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(hendelse)) {
                hendelse.info("mangler nødvendige opplysninger fra arbeidsgiver")
                return vedtaksperiode.tilstand(hendelse, AvventerInntektsmelding)
            }
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            søknad.info("Prøver å igangsette revurdering grunnet korrigerende søknad")
            vedtaksperiode.håndterLåstOverlappendeSøknadRevurdering(søknad)
            if (!søknad.harFunksjonelleFeilEllerVerre() && !vedtaksperiode.forventerInntekt()) {
                vedtaksperiode.emitVedtaksperiodeEndret(søknad) // TODO: for å unngå at flex oppretter oppgaver
            }
        }
        override fun håndterDagerFør(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            dager.leggTilArbeidsdagerFør(vedtaksperiode.periode.start)
            vedtaksperiode.periode = dager.oppdatertFom(vedtaksperiode.periode)
            dager.håndterPeriodeRettFør(vedtaksperiode.periode, vedtaksperiode.arbeidsgiver)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            if (!skalHensyntaInntektsmelding(vedtaksperiode, dager)) {
                vedtaksperiode.emitVedtaksperiodeEndret(dager)
                return false
            }
            vedtaksperiode.låsOpp()
            vedtaksperiode.håndterDager(dager)
            vedtaksperiode.lås()
            if (!vedtaksperiode.forventerInntekt() || Toggle.AuuHåndtererIkkeInntekt.enabled) {
                vedtaksperiode.emitVedtaksperiodeEndret(dager)
                vedtaksperiode.person.igangsettOverstyring(
                    dager,
                    Revurderingseventyr.arbeidsgiverperiode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
                )
            }
            return true
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            if (!vedtaksperiode.forventerInntekt()) return
            if (!skalHensyntaInntektsmelding(vedtaksperiode, inntektOgRefusjon)) return
            vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon)
            inntektOgRefusjon.info("Varsler revurdering i tilfelle inntektsmelding påvirker andre perioder.")
            vedtaksperiode.person.igangsettOverstyring(
                inntektOgRefusjon,
                Revurderingseventyr.arbeidsgiverperiode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            )
        }

        private fun skalHensyntaInntektsmelding(
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg
        ): Boolean {
            val skalHensyntaInntektsmelding = vedtaksperiode.person.vedtaksperioder(NYERE_ELLER_SAMME_SKJÆRINGSTIDSPUNKT_ER_UTBETALT(vedtaksperiode)).isEmpty()
            if (skalHensyntaInntektsmelding) return true
            sikkerlogg.info(
                "Inntektsmelding i AUU hensyntas ikke i {} for {} {} fordi dette eller nyere skjæringstidspunkt har vært utbetalt",
                keyValue("vedtaksperiodeId", vedtaksperiode.id),
                keyValue("aktørId", vedtaksperiode.aktørId),
                keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer)
            )
            hendelse.info("Hensyntar ikke inntektsmelding fordi dette eller nyere skjæringstidspunkt har vært utbetalt")
            return false
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!vedtaksperiode.forventerInntekt()) return
            if (påminnelse.skalReberegnes())
                return vedtaksperiode.person.igangsettOverstyring(påminnelse, Revurderingseventyr.arbeidsgiverperiode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
            if (vedtaksperiode.vilkårsgrunnlag == null) return påminnelse.info("AUU-periode som potensielt burde omgjøres og mangler vilkårsgrunnlag")
            påminnelse.info("AUU-periode som potensielt burde omgjøres og har vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderLåstTidslinje(hendelse)
        }

    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override val erFerdigBehandlet = true

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.lås()
            check(vedtaksperiode.utbetalinger.erAvsluttet()) {
                "forventer at utbetaling skal være avsluttet"
            }
            vedtaksperiode.arbeidsgiver.ferdigstillRevurdering(hendelse, vedtaksperiode)
            vedtaksperiode.kontekst(hendelse) // obs: 'ferdigstillRevurdering' påvirker kontekst på hendelsen
            vedtaksperiode.ferdigstillVedtak(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = HJELP.utenBegrunnelse

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.skjæringstidspunktFraInfotrygd = vedtaksperiode.person.skjæringstidspunkt(vedtaksperiode.sykdomstidslinje.sykdomsperiode() ?: vedtaksperiode.periode)
            vedtaksperiode.låsOpp()
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!revurdering.inngåSomRevurdering(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderLåstTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterLåstOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            vedtaksperiode.håndterDager(dager)
            return true
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) { }
    }

    internal object RevurderingFeilet : Vedtaksperiodetilstand {
        override val type: TilstandType = REVURDERING_FEILET

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.arbeidsgiver.ferdigstillRevurdering(hendelse, vedtaksperiode)
            vedtaksperiode.kontekst(hendelse) // 'ferdigstillRevurdering'  påvirker hendelsekontekst
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
            vedtaksperiode.sendOppgaveEvent()
        }
        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>): Venteårsak? {
            if (vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode.utbetalinger)) return null
            return HJELP.utenBegrunnelse
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            throw IllegalStateException("Kan ikke håndtere søknad mens perioden er i RevurderingFeilet")
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (!vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode.utbetalinger)) return hendelse.info("Gjenopptar ikke revurdering feilet fordi perioden har tidligere avsluttede utbetalinger. Må behandles manuelt vha annullering.")
            hendelse.funksjonellFeil(RV_RV_2)
            vedtaksperiode.forkast(hendelse)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val erFerdigBehandlet = true

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            vedtaksperiode.sendOppgaveEvent()
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, arbeidsgivere: List<Arbeidsgiver>) = HJELP.utenBegrunnelse

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            throw IllegalStateException("Kan ikke håndtere søknad mens perioden er i TilInfotrygd")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er sendt til Infotrygd")
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            throw IllegalStateException("Revurdering håndteres av en periode i til_infotrygd")
        }
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        private val AVSLUTTET_OG_SENERE = listOf(
            Avsluttet,
            AvventerRevurdering,
            AvventerGjennomførtRevurdering,
            AvventerHistorikkRevurdering,
            AvventerVilkårsprøvingRevurdering,
            AvventerSimuleringRevurdering,
            AvventerGodkjenningRevurdering,
            RevurderingFeilet
        )

        // Fredet funksjonsnavn
        internal val TIDLIGERE_OG_ETTERGØLGENDE = fun(segSelv: Vedtaksperiode): VedtaksperiodeFilter {
            // forkaster perioder som er før/overlapper med oppgitt periode, eller som er sammenhengende med
            // perioden som overlapper (per skjæringstidpunkt!).
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            return fun(other: Vedtaksperiode) = other.periode.start >= segSelv.periode.start || other.skjæringstidspunkt == skjæringstidspunkt
        }

        internal val IKKE_FERDIG_REVURDERT: VedtaksperiodeFilter = { it.tilstand == AvventerGjennomførtRevurdering }
        internal val PÅGÅENDE_REVURDERING: VedtaksperiodeFilter = {
            it.tilstand in setOf(
                AvventerVilkårsprøvingRevurdering,
                AvventerHistorikkRevurdering,
                AvventerSimuleringRevurdering,
                AvventerGodkjenningRevurdering
            )
        }

        internal val HAR_PÅGÅENDE_UTBETALINGER: VedtaksperiodeFilter = { it.utbetalinger.utbetales() }

        internal val KLAR_TIL_BEHANDLING: VedtaksperiodeFilter = {
            it.tilstand == AvventerBlokkerendePeriode || it.tilstand == AvventerGodkjenning
        }

        internal val IKKE_FERDIG_BEHANDLET: VedtaksperiodeFilter = { !it.tilstand.erFerdigBehandlet }

        internal val ER_ELLER_HAR_VÆRT_AVSLUTTET: VedtaksperiodeFilter =
            { it.tilstand is AvsluttetUtenUtbetaling || it.utbetalinger.harAvsluttede() }

        internal val MED_SKJÆRINGSTIDSPUNKT = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.skjæringstidspunkt == skjæringstidspunkt }
        }

        internal fun SAMMENHENGENDE_MED_SAMME_SKJÆRINGSTIDSPUNKT_SOM(vedtaksperiode: Vedtaksperiode): VedtaksperiodeFilter {
            val sammenhengendePerioder = vedtaksperiode.arbeidsgiver.finnSammenhengendeVedtaksperioder(vedtaksperiode)
            return { other: Vedtaksperiode -> other.skjæringstidspunkt == vedtaksperiode.skjæringstidspunkt && other in sammenhengendePerioder}
        }

        internal val SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode ->
                MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt)(vedtaksperiode) && vedtaksperiode.forventerInntekt()
            }
        }

        internal val TRENGER_REFUSJONSOPPLYSNINGER = { skjæringstidspunkt: LocalDate, periode: Periode ->
            { vedtaksperiode: Vedtaksperiode ->
                SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG(skjæringstidspunkt)(vedtaksperiode) && vedtaksperiode.periode.overlapperMed(periode)
            }
        }

        internal val NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING = { segSelv: Vedtaksperiode ->
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            { vedtaksperiode: Vedtaksperiode ->
                vedtaksperiode.utbetalinger.erAvsluttet() && vedtaksperiode.skjæringstidspunkt > skjæringstidspunkt && vedtaksperiode.skjæringstidspunkt > segSelv.periode.endInclusive
            }
        }

        internal val NYERE_ELLER_SAMME_SKJÆRINGSTIDSPUNKT_ER_UTBETALT = { segSelv: Vedtaksperiode ->
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            { vedtaksperiode: Vedtaksperiode ->
                vedtaksperiode.utbetalinger.harAvsluttede() && vedtaksperiode.skjæringstidspunkt >= skjæringstidspunkt
            }
        }

        internal val NYERE_SKJÆRINGSTIDSPUNKT_UTEN_UTBETALING = { segSelv: Vedtaksperiode ->
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            { vedtaksperiode: Vedtaksperiode ->
                vedtaksperiode.tilstand == AvsluttetUtenUtbetaling && vedtaksperiode.skjæringstidspunkt > skjæringstidspunkt && vedtaksperiode.skjæringstidspunkt > segSelv.periode.endInclusive
            }
        }

        internal fun aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode: Vedtaksperiode): Aktivitetslogg {
            val tidligereUbetalt =
                vedtaksperiode.arbeidsgiver.finnSykeperioderAvsluttetUtenUtbetalingRettFør(vedtaksperiode)
            val aktivitetskontekster = listOf(vedtaksperiode) + tidligereUbetalt
            return vedtaksperiode.person.aktivitetslogg.logg(*aktivitetskontekster.toTypedArray())
        }

        internal fun Iterable<Vedtaksperiode>.nåværendeVedtaksperiode(filter: VedtaksperiodeFilter) =
            firstOrNull(filter)

        internal fun List<Vedtaksperiode>.trengerInntektsmelding() = filter {
            it.tilstand == AvventerInntektsmelding
        }.filter {
            it.forventerInntekt()
        }

        internal fun List<Vedtaksperiode>.medSkjæringstidspunkt(skjæringstidspunkt: LocalDate) =
            this.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        internal fun List<Vedtaksperiode>.skalHåndtere(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding): Vedtaksperiode? {
            inntektOgRefusjon.strategier.forEach { strategy ->
                forEach { vedtaksperiode ->
                    val match = inntektOgRefusjon.skalHåndteresAv(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode, strategy) { vedtaksperiode.forventerInntektOgRefusjonFraInntektsmelding() }
                    if (match) return vedtaksperiode.also {
                        inntektOgRefusjon.info("Vedtaksperiode ${vedtaksperiode.periode} ble plukket ut til å håndtere inntekt og refusjon av ${strategy::class.simpleName}")
                    }
                }
            }
            return null
        }

        internal fun List<Vedtaksperiode>.håndterHale(dager: DagerFraInntektsmelding) {
            val sisteVedtaksperiodeSomOverlapper = lastOrNull { dager.skalHåndteresAv(it.periode) } ?: return
            dager.håndterHaleEtter(sisteVedtaksperiodeSomOverlapper.periode, sisteVedtaksperiodeSomOverlapper.arbeidsgiver)
        }

        internal fun harNyereForkastetPeriode(forkastede: Iterable<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            forkastede
                .filter { it.periode().endInclusive >= hendelse.periode().start }
                .onEach {
                    hendelse.funksjonellFeil(RV_SØ_20)
                    hendelse.info("Søknad overlapper med, eller er før, en forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                }
                .isNotEmpty()

        internal fun harKortGapTilForkastet(forkastede: Iterable<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            forkastede
                .filter { it.sykdomstidslinje.dagerMellom(hendelse.sykdomstidslinje()) in 2..20 }
                .onEach {
                    hendelse.funksjonellFeil(RV_SØ_28)
                    hendelse.info("Søknad har et gap som er kortere enn 20 dager til en forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                }
                .isNotEmpty()

        internal fun forlengerForkastet(forkastede: List<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse) =
            forkastede
                .filter { it.sykdomstidslinje.erRettFør(hendelse.sykdomstidslinje()) }
                .onEach {
                    hendelse.funksjonellFeil(RV_SØ_19)
                    hendelse.info("Søknad forlenger forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                }
                .isNotEmpty()

        internal fun sjekkOmOverlapperMedForkastet(
            forkastede: Iterable<Vedtaksperiode>,
            inntektsmelding: Inntektsmelding
        ) =
            forkastede.any { it.periode.overlapperMed(inntektsmelding.periode()) }

        internal fun List<Vedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            filter { it.utbetalinger.harId(utbetalingId) }.map { it.id }

        internal fun List<Vedtaksperiode>.periode(): Periode {
            val fom = minOf { it.periode.start }
            val tom = maxOf { it.periode.endInclusive }
            return Periode(fom, tom)
        }

        internal fun arbeidsgiverperiodeFor(
            person: Person,
            perioder: List<Vedtaksperiode>,
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            subsumsjonObserver: SubsumsjonObserver?
        ): List<Arbeidsgiverperiode> {
            val samletSykdomstidslinje =
                Sykdomstidslinje.gammelTidslinje(perioder.map { it.sykdomstidslinje }).merge(sykdomstidslinje, replace)
            return person.arbeidsgiverperiodeFor(
                organisasjonsnummer,
                samletSykdomstidslinje,
                subsumsjonObserver
            )
        }

        internal fun List<Vedtaksperiode>.feiletRevurdering(other: Vedtaksperiode) =
            any { it.tilstand == RevurderingFeilet && it.skjæringstidspunkt == other.skjæringstidspunkt }

        internal fun ferdigVedtaksperiode(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            id: UUID,
            aktørId: String,
            fødselsnummer: String,
            organisasjonsnummer: String,
            tilstand: Vedtaksperiodetilstand,
            skjæringstidspunktFraInfotrygd: LocalDate?,
            sykdomstidslinje: Sykdomstidslinje,
            dokumentsporing: Set<Dokumentsporing>,
            inntektsmeldingInfo: InntektsmeldingInfo?,
            periode: Periode,
            sykmeldingsperiode: Periode,
            utbetalinger: VedtaksperiodeUtbetalinger,
            utbetalingstidslinje: Utbetalingstidslinje,
            forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
            opprettet: LocalDateTime,
            oppdatert: LocalDateTime,
            medVedtaksperiode: MaskinellJurist
        ): Vedtaksperiode = Vedtaksperiode(
            person = person,
            arbeidsgiver = arbeidsgiver,
            id = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            tilstand = tilstand,
            skjæringstidspunktFraInfotrygd = skjæringstidspunktFraInfotrygd,
            sykdomstidslinje = sykdomstidslinje,
            hendelseIder = dokumentsporing.map { it }.toMutableSet(),
            inntektsmeldingInfo = inntektsmeldingInfo,
            periode = periode,
            sykmeldingsperiode = sykmeldingsperiode,
            utbetalinger = utbetalinger,
            utbetalingstidslinje = utbetalingstidslinje,
            forlengelseFraInfotrygd = forlengelseFraInfotrygd,
            opprettet = opprettet,
            oppdatert = oppdatert,
            jurist = medVedtaksperiode
        )

        internal fun List<Vedtaksperiode>.sykefraværstilfelle(skjæringstidspunkt: LocalDate): Periode {
            val sisteDato = filter { it.skjæringstidspunkt == skjæringstidspunkt }.maxOf { it.periode.endInclusive }
            return skjæringstidspunkt til sisteDato
        }

        private fun List<Vedtaksperiode>.manglendeUtbetalingsopplysninger(dag: LocalDate, melding: String) {
            val vedtaksperiode = firstOrNull { dag in it.periode } ?: return
            val potensieltNyttSkjæringstidspunkt =
                vedtaksperiode.skjæringstidspunktFraInfotrygd != null && vedtaksperiode.skjæringstidspunkt != vedtaksperiode.skjæringstidspunktFraInfotrygd

            sikkerlogg.warn("Manglende utbetalingsopplysninger: $melding for $dag med skjæringstidspunkt ${vedtaksperiode.skjæringstidspunkt}. {}, {}, {}, {}, {}",
                keyValue("aktørId", vedtaksperiode.aktørId),
                keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer),
                keyValue("tilstand", vedtaksperiode.tilstand.type.name),
                keyValue("vedtaksperiodeId", "${vedtaksperiode.id}"),
                keyValue("potensieltNyttSkjæringstidspunkt", "$potensieltNyttSkjæringstidspunkt")
            )
        }

        internal fun List<Vedtaksperiode>.ugyldigUtbetalingstidslinje(dager: Set<LocalDate>) {
            val vedtaksperiode = firstOrNull() ?: return
            sikkerlogg.warn("Ugyldig utbetalingstidslinje: utbetalingsdager med kilde Sykmelding: ${dager.grupperSammenhengendePerioder()}. {}, {}, {}",
                keyValue("aktørId", vedtaksperiode.aktørId),
                keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer),
                keyValue("antallDager", dager.size)
            )
        }

        internal fun List<Vedtaksperiode>.manglerVilkårsgrunnlag(dag: LocalDate) =
            manglendeUtbetalingsopplysninger(dag, "mangler vilkårsgrunnlag")
        internal fun List<Vedtaksperiode>.inngårIkkeISykepengegrunnlaget(dag: LocalDate) =
            manglendeUtbetalingsopplysninger(dag, "inngår ikke i sykepengegrunnlaget")
        internal fun List<Vedtaksperiode>.manglerRefusjonsopplysninger(dag: LocalDate) =
            manglendeUtbetalingsopplysninger(dag, "mangler refusjonsopplysninger")

    }
}


enum class ForlengelseFraInfotrygd {
    IKKE_ETTERSPURT,
    JA,
    NEI
}

enum class Periodetype {
    /** Perioden er første periode i et sykdomstilfelle */
    FØRSTEGANGSBEHANDLING,

    /** Perioden en en forlengelse av en Spleis-periode */
    FORLENGELSE,

    /** Perioden en en umiddelbar forlengelse av en periode som er utbetalt i Infotrygd */
    OVERGANG_FRA_IT,

    /** Perioden er en direkte eller indirekte forlengelse av en OVERGANG_FRA_IT-periode */
    INFOTRYGDFORLENGELSE;

    fun tilUtbetalingPeriodetype(): UtbetalingPeriodetype = when (this) {
        FØRSTEGANGSBEHANDLING -> UtbetalingPeriodetype.FØRSTEGANGSBEHANDLING
        FORLENGELSE -> UtbetalingPeriodetype.FORLENGELSE
        OVERGANG_FRA_IT -> UtbetalingPeriodetype.OVERGANG_FRA_IT
        INFOTRYGDFORLENGELSE -> UtbetalingPeriodetype.INFOTRYGDFORLENGELSE
    }
}

enum class Inntektskilde {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE;
    fun tilUtbetalingInntektskilde(): UtbetalingInntektskilde = when(this) {
        EN_ARBEIDSGIVER -> UtbetalingInntektskilde.EN_ARBEIDSGIVER
        FLERE_ARBEIDSGIVERE -> UtbetalingInntektskilde.FLERE_ARBEIDSGIVERE
    }
}

internal typealias VedtaksperiodeFilter = (Vedtaksperiode) -> Boolean
