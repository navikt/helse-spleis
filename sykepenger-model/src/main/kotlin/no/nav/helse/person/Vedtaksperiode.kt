package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Hendelseskontekst
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrInntekt
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Subsumsjon
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingsgrunnlag
import no.nav.helse.hendelser.Utbetalingshistorikk
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.hendelser.Vilkårsgrunnlag
import no.nav.helse.hendelser.Ytelser
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.memoized
import no.nav.helse.oktober
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsforhold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dødsinformasjon
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSammenligningsgrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigInntekt
import no.nav.helse.person.Arbeidsgiver.Companion.senerePerioderPågående
import no.nav.helse.person.Arbeidsgiver.Companion.trengerSøknadFør
import no.nav.helse.person.Arbeidsgiver.Companion.trengerSøknadISammeMåned
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.ForlengelseFraInfotrygd.IKKE_ETTERSPURT
import no.nav.helse.person.InntektsmeldingInfo.Companion.ider
import no.nav.helse.person.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.person.Periodetype.OVERGANG_FRA_IT
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
import no.nav.helse.person.TilstandType.AVVENTER_ARBEIDSGIVERE_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_BLOKKERENDE_PERIODE
import no.nav.helse.person.TilstandType.AVVENTER_GJENNOMFØRT_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING
import no.nav.helse.person.TilstandType.AVVENTER_GODKJENNING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_HISTORIKK_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK
import no.nav.helse.person.TilstandType.AVVENTER_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING
import no.nav.helse.person.TilstandType.AVVENTER_SIMULERING_REVURDERING
import no.nav.helse.person.TilstandType.AVVENTER_UFERDIG
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.filter.Utbetalingsfilter
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Refusjonsgjødsler
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.utbetalingstidslinje.UtbetalingstidslinjerFilter
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
    private var inntektskilde: Inntektskilde,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet,
    jurist: MaskinellJurist
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val jurist = jurist.medVedtaksperiode(id, hendelseIder, sykmeldingsperiode)
    private val skjæringstidspunkt get() = skjæringstidspunktFraInfotrygd ?: person.skjæringstidspunkt(periode)
    private val periodetype get() = arbeidsgiver.periodetype(periode)

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
        utbetalinger = VedtaksperiodeUtbetalinger(arbeidsgiver),
        utbetalingstidslinje = Utbetalingstidslinje(),
        inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        opprettet = LocalDateTime.now(),
        jurist = jurist
    ) {
        kontekst(søknad)
    }

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        val periodetypeMemoized = this::periodetype.memoized()
        val skjæringstidspunktMemoized = this::skjæringstidspunkt.memoized()
        visitor.preVisitVedtaksperiode(
            this,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            sykmeldingsperiode,
            periodetypeMemoized,
            skjæringstidspunktMemoized,
            skjæringstidspunktFraInfotrygd,
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskilde
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
            periodetypeMemoized,
            skjæringstidspunktMemoized,
            skjæringstidspunktFraInfotrygd,
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskilde
        )
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun jurist() = jurist.medVedtaksperiode(id, hendelseIder.toSet(), sykmeldingsperiode)

    internal fun harId(vedtaksperiodeId: UUID) = id == vedtaksperiodeId

    internal fun hendelseIder() = hendelseIder.ider()

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        val periodeFør = sykmelding.periode()
        sykmelding.trimLeft(periode.endInclusive)
        kontekst(sykmelding)
        sykmelding.info("Trimmer sykmelding som overlapper med vedtaksperiode. Før trimming $periodeFør, etter trimming ${sykmelding.periode()}")
    }

    internal fun håndter(søknad: Søknad) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        søknad.leggTil(hendelseIder)
        tilstand.håndter(this, søknad)
    }

    internal fun håndter(
        inntektsmelding: Inntektsmelding,
        other: UUID?,
        vedtaksperioder: List<Vedtaksperiode>
    ): Boolean {
        val sammenhengendePerioder = arbeidsgiver.finnSammehengendeVedtaksperioder(this)
        val overlapper = overlapperMedSammenhengende(inntektsmelding, sammenhengendePerioder, other, vedtaksperioder)
        return overlapper.also {
            if (erAlleredeHensyntatt(inntektsmelding)) {
                sikkerlogg.info("Vedtaksperiode med id=$id har allerede hensyntatt inntektsmeldingen med id=${inntektsmelding.meldingsreferanseId()}, replayes det en inntektsmelding unødvendig?")
                return@also
            }
            if (it) inntektsmelding.leggTil(hendelseIder)
            if (!it) return@also inntektsmelding.trimLeft(periode.endInclusive)
            kontekst(inntektsmelding)
            if (!inntektsmelding.erRelevant(periode, sammenhengendePerioder.map { it.periode })) return@also
            tilstand.håndter(this, inntektsmelding)
        }
    }

    private fun erAlleredeHensyntatt(inntektsmelding: Inntektsmelding) =
        hendelseIder.ider().contains(inntektsmelding.meldingsreferanseId())

    internal fun håndterHistorikkFraInfotrygd(hendelse: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk) {
        tilstand.håndter(person, arbeidsgiver, this, hendelse, infotrygdhistorikk)
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk, infotrygdhistorikk: Infotrygdhistorikk) {
        if (!utbetalingshistorikk.erRelevant(id)) return
        kontekst(utbetalingshistorikk)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingshistorikk, infotrygdhistorikk)
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
        if (!vilkårsgrunnlag.erRelevant(id)) return
        kontekst(vilkårsgrunnlag)
        tilstand.håndter(this, vilkårsgrunnlag)
    }

    internal fun håndter(utbetalingsgrunnlag: Utbetalingsgrunnlag) {
        if (!utbetalingsgrunnlag.erRelevant(id)) return
        kontekst(utbetalingsgrunnlag)
        tilstand.håndter(this, utbetalingsgrunnlag)
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
        forkast(hendelse, SENERE_INCLUSIVE(this))
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (!påminnelse.erRelevant(id)) return false
        kontekst(påminnelse)
        tilstand.påminnelse(this, påminnelse)
        return true
    }

    internal fun håndter(hendelse: OverstyrTidslinje) = hendelse.erRelevant(periode).also { erRelevant ->
        if (!erRelevant) return IKKE_HÅNDTERT
        kontekst(hendelse)
        hendelse.leggTil(hendelseIder)
        if (!hendelse.alleredeHåndtert()) {
            hendelse.markerHåndtert()
            tilstand.håndter(this, hendelse)
        }
    }

    internal fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold, subsumsjon: Subsumsjon?): Boolean {
        if (!overstyrArbeidsforhold.erRelevant(skjæringstidspunkt)) return false
        kontekst(overstyrArbeidsforhold)
        overstyrArbeidsforhold.leggTil(hendelseIder)
        return tilstand.håndter(this, overstyrArbeidsforhold, subsumsjon)
    }

    internal fun håndterOverstyringAvGhostInntekt(overstyrInntekt: OverstyrInntekt): Boolean {
        if (!kanHåndtereOverstyring(overstyrInntekt)) return false
        kontekst(overstyrInntekt)
        overstyrInntekt.leggTil(hendelseIder)
        return tilstand.håndterOverstyringAvGhostInntekt(this, overstyrInntekt)
    }

    internal fun håndter(hendelse: OverstyrInntekt, vedtaksperioder: Iterable<Vedtaksperiode>): Boolean {
        if (!kanHåndtereOverstyring(hendelse)) return false
        kontekst(hendelse)
        vedtaksperioder.filter(MED_SKJÆRINGSTIDSPUNKT(hendelse.skjæringstidspunkt)).forEach {
            hendelse.leggTil(it.hendelseIder)
        }
        tilstand.håndter(this, hendelse)
        return true
    }

    internal fun nyPeriodeMedNyFlyt(ny: Vedtaksperiode, hendelse: Søknad) {
        if (ny etter this || ny == this) return
        kontekst(hendelse)
        tilstand.nyPeriodeFørMedNyFlyt(this, ny, hendelse)
    }

    internal fun blokkererOverstyring(skjæringstidspunkt: LocalDate) =
        skjæringstidspunkt == this.skjæringstidspunkt && !tilstand.kanReberegnes

    internal fun håndterRevurdertUtbetaling(
        other: Vedtaksperiode,
        revurdertUtbetaling: Utbetaling,
        aktivitetslogg: IAktivitetslogg
    ) {
        if (this == other || utbetalinger.hørerIkkeSammenMed(revurdertUtbetaling) || this.skjæringstidspunkt != other.skjæringstidspunkt) return
        tilstand.håndterRevurdertUtbetaling(this, revurdertUtbetaling, aktivitetslogg)
    }

    override fun compareTo(other: Vedtaksperiode) = this.periode.endInclusive.compareTo(other.periode.endInclusive)
    internal infix fun før(other: Vedtaksperiode) = this < other
    internal infix fun etter(other: Vedtaksperiode) = this > other

    internal fun erVedtaksperiodeRettFør(other: Vedtaksperiode) =
        this.sykdomstidslinje.erRettFør(other.sykdomstidslinje)

    internal fun erSykeperiodeAvsluttetUtenUtbetalingRettFør(other: Vedtaksperiode) =
        this.sykdomstidslinje.erRettFør(other.sykdomstidslinje) && this.tilstand == AvsluttetUtenUtbetaling

    private fun kanHåndtereOverstyring(hendelse: OverstyrInntekt): Boolean {
        return utbetalingstidslinje.isNotEmpty() && hendelse.skjæringstidspunkt == this.skjæringstidspunkt
    }

    private fun kanOverstyreInntektForFlereArbeidsgivere(hendelse: OverstyrInntekt): Boolean {
        if (Toggle.OverstyrInntektMedFlereArbeidsgivere.disabled && inntektskilde == Inntektskilde.FLERE_ARBEIDSGIVERE) {
            hendelse.error("Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
            return false
        }
        return true
    }

    private fun kanRevurdereInntektForFlereArbeidsgivere(hendelse: OverstyrInntekt): Boolean {
        if (Toggle.RevurdereInntektMedFlereArbeidsgivere.disabled && inntektskilde == Inntektskilde.FLERE_ARBEIDSGIVERE) {
            hendelse.error("Forespurt revurdering av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
            return false
        }
        return true
    }

    private fun harNødvendigInntektForVilkårsprøving() =
        arbeidsgiver.harNødvendigInntektForVilkårsprøving(skjæringstidspunkt, periode.start, ingenUtbetaling())

    private fun låsOpp() = arbeidsgiver.låsOpp(periode)
    private fun lås() = arbeidsgiver.lås(periode)

    private fun skalHaWarningForFlereArbeidsforholdUtenSykdomEllerUlikStartdato(
        vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
    ) =
        arbeidsgiver.erFørstegangsbehandling(periode)
                && (flereArbeidsforholdUtenSykdom(vilkårsgrunnlag) || flereArbeidsforholdUlikStartdato())

    private fun flereArbeidsforholdUtenSykdom(vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement) =
        vilkårsgrunnlag is VilkårsgrunnlagHistorikk.Grunnlagsdata && vilkårsgrunnlag.harInntektFraAOrdningen()

    private fun flereArbeidsforholdUlikStartdato() = person.harFlereArbeidsforholdMedUlikStartdato(skjæringstidspunkt)

    internal fun forkast(hendelse: IAktivitetslogg, utbetalinger: List<Utbetaling>): Boolean {
        if (!this.utbetalinger.kanForkastes(utbetalinger) || this.tilstand == AvsluttetUtenUtbetaling) return false
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        this.utbetalinger.forkast(hendelse)
        person.vedtaksperiodeForkastet(
            hendelse,
            PersonObserver.VedtaksperiodeForkastetEvent(
                gjeldendeTilstand = tilstand.type,
                hendelser = hendelseIder(),
                fom = periode.start,
                tom = periode.endInclusive
            )
        )
        if (this.tilstand !in AVSLUTTET_OG_SENERE) tilstand(hendelse, TilInfotrygd)
        return true
    }

    private fun forkast(hendelse: IAktivitetslogg, filter: VedtaksperiodeFilter = TIDLIGERE_OG_ETTERGØLGENDE(periode)) {
        person.søppelbøtte(hendelse, filter)
    }

    private fun erUtbetalt() = tilstand == Avsluttet && utbetalinger.erAvsluttet()

    private fun revurderInntekt(hendelse: OverstyrInntekt) {
        person.nyInntekt(hendelse)
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(hendelse, skjæringstidspunkt, jurist(), null, null)
        person.startRevurdering(this, hendelse)
    }

    private fun revurderTidslinje(hendelse: OverstyrTidslinje) {
        oppdaterHistorikk(hendelse)
        person.startRevurdering(this, hendelse)
    }

    internal fun periode() = periode

    private fun kontekst(hendelse: IAktivitetslogg) {
        hendelse.kontekst(arbeidsgiver)
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) = hendelse.erRelevant(this.periode)

    private fun overlapperMedSammenhengende(
        inntektsmelding: Inntektsmelding,
        perioder: List<Vedtaksperiode>,
        other: UUID?,
        vedtaksperioder: List<Vedtaksperiode>
    ): Boolean {
        return inntektsmelding.erRelevant(perioder.periode()) && relevantForReplay(other, perioder, vedtaksperioder)
    }

    private fun skjæringstidspunktperiode() = person.skjæringstidspunktperiode(skjæringstidspunkt)

    private fun validerYtelserForSkjæringstidspunkt(ytelser: Ytelser) {
        person.validerYtelserForSkjæringstidspunkt(ytelser, skjæringstidspunkt)
        kontekst(ytelser) // resett kontekst til oss selv
    }

    // IM som replayes skal kunne påvirke alle perioder som er sammenhengende med replay-perioden, men også alle evt. påfølgende perioder.
    // Dvs. IM skal -ikke- påvirke perioder _før_ replay-perioden som ikke er i sammenheng med vedtaksperioden som ba om replay
    private fun relevantForReplay(
        other: UUID?,
        sammenhengendePerioder: List<Vedtaksperiode>,
        vedtaksperioder: List<Vedtaksperiode>
    ): Boolean {
        if (other == null) return true // ingen replay
        if (vedtaksperioder.none { it.id == other }) return false // perioden som ba om replay ikke finnes mer
        if (sammenhengendePerioder.any { it.id == other }) return true // perioden som ba om replay er en del av de sammenhengende periodene som overlapper med IM (som vedtaksperioden er en del av)
        return this etter vedtaksperioder.first { it.id == other } // vedtaksperioden er _etter_ den perioden som ba om replay
    }

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

    private fun håndterInntektsmelding(hendelse: Inntektsmelding, nesteTilstand: Vedtaksperiodetilstand
    ) {
        periode = hendelse.oppdaterFom(periode)
        oppdaterHistorikk(hendelse)
        inntektsmeldingInfo = arbeidsgiver.addInntektsmelding(skjæringstidspunkt, hendelse, jurist())
        hendelse.valider(periode, skjæringstidspunkt, finnArbeidsgiverperiode(), jurist())
        if (hendelse.hasErrorsOrWorse()) return forkast(hendelse, SENERE_INCLUSIVE(this))
        hendelse.info("Fullført behandling av inntektsmelding")
        if (hendelse.hasErrorsOrWorse()) return
        tilstand(hendelse, nesteTilstand)
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        hendelse.trimLeft(periode.endInclusive)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
    }

    private fun håndterSøknad(hendelse: Søknad, nesteTilstand: () -> Vedtaksperiodetilstand? = { null }) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (!person.harFlereArbeidsgivereMedSykdom()) hendelse.validerIkkeOppgittFlereArbeidsforholdMedSykmelding()
        hendelse.valider(periode, jurist())
        if (hendelse.hasErrorsOrWorse()) {
            if (person.harFlereArbeidsgivereMedSykdom()) return person.invaliderAllePerioder(
                hendelse,
                "Invaliderer alle perioder pga flere arbeidsgivere og feil i søknad"
            )
            hendelse.error("Invaliderer alle perioder for arbeidsgiver pga feil i søknad")
            return forkast(hendelse)
        }
        nesteTilstand()?.also { tilstand(hendelse, it) }
    }

    private fun overlappendeSøknadIkkeStøttet(søknad: Søknad, egendefinertFeiltekst: String? = null) {
        søknad.trimLeft(periode.endInclusive)
        søknad.error(
            egendefinertFeiltekst
                ?: "Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass"
        )
        if (!tilstand.kanForkastes) return
        forkast(søknad)
    }

    private fun håndterOverlappendeSøknad(søknad: Søknad, nesteTilstand: Vedtaksperiodetilstand? = null) {
        if (søknad.periode().utenfor(periode)) return overlappendeSøknadIkkeStøttet(
            søknad,
            "Overlappende søknad starter før, eller slutter etter, opprinnelig periode"
        )
        if (søknad.harArbeidsdager()) return overlappendeSøknadIkkeStøttet(
            søknad,
            "Mottatt flere søknader for perioden - siste søknad inneholder arbeidsdag"
        )
        håndterSøknad(søknad) { nesteTilstand }
    }

    private fun håndterVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand) {
        vilkårsgrunnlag.lagreArbeidsforhold(person, skjæringstidspunkt)
        vilkårsgrunnlag.lagreSkatteinntekter(person, skjæringstidspunkt)
        vilkårsgrunnlag.loggUkjenteArbeidsforhold(person, skjæringstidspunkt)

        if (person.harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(skjæringstidspunkt)) {
            vilkårsgrunnlag.warn("Arbeidsgiver er ikke registrert i Aa-registeret.")
        }

        vilkårsgrunnlag.lagreRapporterteInntekter(person, skjæringstidspunkt)

        val grunnlagForSykepengegrunnlag = person.beregnSykepengegrunnlag(skjæringstidspunkt, jurist(), null, null)
        val sammenligningsgrunnlag = person.beregnSammenligningsgrunnlag(skjæringstidspunkt, jurist())
        val opptjening = person.beregnOpptjening(skjæringstidspunkt, jurist())

        vilkårsgrunnlag.valider(
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            skjæringstidspunkt,
            opptjening,
            person.antallArbeidsgivereMedRelevantArbeidsforhold(skjæringstidspunkt),
            jurist()
        )
        person.lagreVilkårsgrunnlag(vilkårsgrunnlag.grunnlagsdata())
        if (vilkårsgrunnlag.hasErrorsOrWorse()) {
            return person.invaliderAllePerioder(vilkårsgrunnlag, null)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag vurdert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun trengerYtelser(hendelse: IAktivitetslogg, periode: Periode = periode()) {
        person.trengerHistorikkFraInfotrygd(hendelse)
        foreldrepenger(hendelse)
        pleiepenger(hendelse, periode)
        omsorgspenger(hendelse, periode)
        opplæringspenger(hendelse, periode)
        institusjonsopphold(hendelse, periode)
        arbeidsavklaringspenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
        dagpenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
        dødsinformasjon(hendelse)
    }

    private fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, maksAlder: LocalDateTime? = null) {
        person.trengerHistorikkFraInfotrygd(hendelse, this, maksAlder)
    }

    private fun trengerVilkårsgrunnlag(hendelse: IAktivitetslogg) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntekterForSykepengegrunnlag(hendelse, beregningSlutt.minusMonths(2), beregningSlutt)
        arbeidsforhold(hendelse)
        inntekterForSammenligningsgrunnlag(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
        medlemskap(hendelse, periode.start, periode.endInclusive)
    }

    private fun trengerInntektsmelding(hendelseskontekst: Hendelseskontekst) {
        this.person.trengerInntektsmelding(
            hendelseskontekst,
            this.organisasjonsnummer,
            PersonObserver.ManglendeInntektsmeldingEvent(
                fom = this.periode.start,
                tom = this.periode.endInclusive,
                søknadIder = hendelseIder.søknadIder()
            )
        )
    }

    private fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst) {
        this.person.trengerIkkeInntektsmelding(
            hendelseskontekst,
            PersonObserver.TrengerIkkeInntektsmeldingEvent(
                fom = this.periode.start,
                tom = this.periode.endInclusive,
                søknadIder = hendelseIder.søknadIder()
            )
        )
    }

    internal fun trengerInntektsmeldingReplay() {
        person.inntektsmeldingReplay(id)
    }

    private fun emitVedtaksperiodeEndret(
        aktivitetslogg: IAktivitetslogg,
        previousState: Vedtaksperiodetilstand = tilstand
    ) {
        val event = PersonObserver.VedtaksperiodeEndretEvent(
            gjeldendeTilstand = tilstand.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = aktivitetslogg.toMap(),
            harVedtaksperiodeWarnings = person.aktivitetslogg.logg(this)
                .let { it.hasWarningsOrWorse() && !it.hasErrorsOrWorse() },
            hendelser = hendelseIder(),
            makstid = tilstand.makstid(this, LocalDateTime.now()),
            fom = periode.start,
            tom = periode.endInclusive
        )

        person.vedtaksperiodeEndret(aktivitetslogg, event)
    }

    private fun sendVedtakFattet(hendelse: IAktivitetslogg) {
        val builder = VedtakFattetBuilder(
            periode,
            hendelseIder(),
            skjæringstidspunkt
        )
        utbetalinger.build(builder)
        person.build(skjæringstidspunkt, builder)
        person.vedtakFattet(hendelse.hendelseskontekst(), builder.result())
    }

    /**
     * Skedulering av utbetaling opp mot andre arbeidsgivere
     */
    private fun forsøkUtbetaling(
        maksimumSykepenger: Alder.MaksimumSykepenger,
        hendelse: ArbeidstakerHendelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
        vedtaksperioder
            .filter { this.periode.overlapperMed(it.periode) }
            .forEach { it.lagUtbetaling(maksimumSykepenger, hendelse) }
        høstingsresultater(hendelse, vedtaksperioder.drop(1))
    }

    private fun lagUtbetaling(maksimumSykepenger: Alder.MaksimumSykepenger, hendelse: ArbeidstakerHendelse) {
        utbetalingstidslinje = utbetalinger.lagUtbetaling(fødselsnummer, periode, maksimumSykepenger, hendelse)
    }

    private fun høstingsresultater(hendelse: ArbeidstakerHendelse, andreVedtaksperioder: List<Vedtaksperiode>) {
        val ingenUtbetaling = !utbetalinger.harUtbetalinger()
        val kunArbeidsgiverdager = utbetalingstidslinje.kunArbeidsgiverdager()
        val harBrukerutbetaling = harBrukerutbetaling(andreVedtaksperioder)

        val utbetalingsfilter = Utbetalingsfilter.Builder()
            .inntektkilde(inntektskilde)
            .also { utbetalinger.build(it) }
            .also { inntektsmeldingInfo?.build(it, arbeidsgiver) }
            .utbetalingstidslinjerHarBrukerutbetaling(harBrukerutbetaling)
            .build()

        when {
            utbetalingsfilter.kanIkkeUtbetales(hendelse) -> {
                person.invaliderAllePerioder(
                    hendelse,
                    "Kan ikke fortsette på grunn av manglende funksjonalitet for utbetaling til bruker"
                )
            }
            ingenUtbetaling -> {
                tilstand(hendelse, AvventerGodkjenning) {
                    if (kunArbeidsgiverdager)
                        hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav, men inneholder andre warnings""")
                    else
                        hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
                }
            }
            else -> {
                tilstand(hendelse, AvventerSimulering) {
                    hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
                }
            }
        }
    }

    private fun harBrukerutbetaling(andreVedtaksperioder: List<Vedtaksperiode>) =
        utbetalingstidslinje.harBrukerutbetalinger() || andreVedtaksperioder.any { it.utbetalingstidslinje.harBrukerutbetalinger() }

    private fun forsøkRevurdering(
        arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
        hendelse: ArbeidstakerHendelse
    ) {
        person.lagRevurdering(this, arbeidsgiverUtbetalinger, hendelse)
        høstingsresultaterRevurdering(hendelse)
    }

    private fun lagRevurdering(maksimumSykepenger: Alder.MaksimumSykepenger, aktivitetslogg: IAktivitetslogg) {
        utbetalinger.forkast(aktivitetslogg)
        utbetalingstidslinje = utbetalinger.lagRevurdering(fødselsnummer, this, periode, maksimumSykepenger, aktivitetslogg)
    }

    private fun høstingsresultaterRevurdering(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Videresender utbetaling til alle vedtaksperioder innenfor samme fagsystemid som er til revurdering")
        when {
            !utbetalinger.harUtbetalinger() -> {
                tilstand(hendelse, AvventerGodkjenningRevurdering) {
                    hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
                }
            }
            else -> {
                tilstand(hendelse, AvventerSimuleringRevurdering) {
                    hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
                }
            }
        }
    }

    private fun mottaUtbetalingTilRevurdering(aktivitetslogg: IAktivitetslogg, utbetaling: Utbetaling) {
        utbetalingstidslinje = utbetalinger.mottaRevurdering(aktivitetslogg, utbetaling, periode)
    }

    private fun harArbeidsgivereMedOverlappendeUtbetaltePerioder(periode: Periode) =
        person.harArbeidsgivereMedOverlappendeUtbetaltePerioder(organisasjonsnummer, periode)

    private fun Vedtaksperiodetilstand.påminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
        if (!påminnelse.gjelderTilstand(type)) return vedtaksperiode.person.vedtaksperiodeIkkePåminnet(påminnelse, type)
        vedtaksperiode.person.vedtaksperiodePåminnet(påminnelse)
        if (LocalDateTime.now() >= makstid(
                vedtaksperiode,
                påminnelse.tilstandsendringstidspunkt()
            )
        ) return håndterMakstid(vedtaksperiode, påminnelse)
        håndter(vedtaksperiode, påminnelse)
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    private fun finnArbeidsgiverperiode() =
        arbeidsgiver.arbeidsgiverperiode(
            periode,
            SubsumsjonObserver.NullObserver
        ) // TODO: skal vi logge ved beregning av agp?

    private fun ingenUtbetaling() = Arbeidsgiverperiode.ingenUtbetaling(finnArbeidsgiverperiode(), periode, jurist())

    // Gang of four State pattern
    internal sealed interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType
        val erFerdigBehandlet: Boolean get() = false
        val kanForkastes: Boolean get() = true

        /**
         *  en periode som kan reberegnes er en periode hvor vi ikke har kommet med forslag til en utbetaling, dvs. alle tilstander før AvventerGodkjenning
         */
        val kanReberegnes: Boolean get() = true

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(110)

        fun håndterMakstid(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            påminnelse.error("Gir opp fordi tilstanden er nådd makstid")
            vedtaksperiode.forkast(påminnelse)
        }

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand", mapOf(
                    "tilstand" to type.name
                )
            )
        }

        fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {
            if (Toggle.RevurdereOutOfOrder.enabled) return
            hendelse.error("Mottatt søknad out of order")
            vedtaksperiode.forkast(hendelse)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.overlappendeSøknadIkkeStøttet(søknad)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.trimLeft(vedtaksperiode.periode.endInclusive)
            inntektsmelding.warn("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.error("Forventet ikke vilkårsgrunnlag i %s", type.name)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            validation(hendelse) {
                onValidationFailed { vedtaksperiode.forkast(hendelse) }
                valider {
                    infotrygdhistorikk.validerOverlappende(
                        this,
                        arbeidsgiver.avgrensetPeriode(vedtaksperiode.periode),
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                onSuccess { info("Utbetalingshistorikk sjekket; fant ingen feil.") }
            }
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            arbeidsgiverUtbetalingerFun: (SubsumsjonObserver) -> ArbeidsgiverUtbetalinger
        ) {
            ytelser.error("Forventet ikke ytelsehistorikk i %s", type.name)
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            utbetalingsgodkjenning.error("Forventet ikke utbetalingsgodkjenning i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerHistorikkFraInfotrygd(påminnelse, LocalDateTime.now().minusHours(24))
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            simulering.error("Forventet ikke simulering i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            hendelse.error("Forventet ikke utbetaling i %s", type.name)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: OverstyrTidslinje
        ) {
            hendelse.error("Forventet ikke overstyring fra saksbehandler i %s", type.name)
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            overstyrArbeidsforhold: OverstyrArbeidsforhold,
            subsumsjon: Subsumsjon?
        ) = false

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Utbetalingsgrunnlag) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {}

        fun håndterOverstyringAvGhostInntekt(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) = false

        fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg
        ) {}

        fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            hendelse.info("Tidligere periode ferdigbehandlet, men gjør ingen tilstandsendring.")
        }

        fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {}

        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (vedtaksperiode.harArbeidsgivereMedOverlappendeUtbetaltePerioder(vedtaksperiode.periode)) {
                søknad.warn("Denne personen har en utbetaling for samme periode for en annen arbeidsgiver. Kontroller at beregningene for begge arbeidsgiverne er korrekte.")
            }

            vedtaksperiode.håndterSøknad(søknad) {
                when {
                    !vedtaksperiode.harNødvendigInntektForVilkårsprøving() -> AvventerInntektsmeldingEllerHistorikk
                    else -> AvventerBlokkerendePeriode
                }
            }

            søknad.info("Fullført behandling av søknad")
        }
    }

    internal object AvventerRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (!vedtaksperiode.person.kanStarteRevurdering(vedtaksperiode)) return
            vedtaksperiode.tilstand(hendelse, AvventerGjennomførtRevurdering)
            vedtaksperiode.person.gjenopptaRevurdering(vedtaksperiode.arbeidsgiver, vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.person.startRevurdering(vedtaksperiode, hendelse)
        }
        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, this)
            vedtaksperiode.person.gjenopptaBehandling(inntektsmelding)
        }

        override fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Mottatt revurdert utbetaling fra en annen vedtaksperiode")
            vedtaksperiode.mottaUtbetalingTilRevurdering(aktivitetslogg, utbetaling)
        }

        override fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.valider(ytelser, arbeidsgiver, periode, skjæringstidspunkt)
            ytelser.valider(periode, skjæringstidspunkt)
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }
    }

    internal object AvventerGjennomførtRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GJENNOMFØRT_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            sikkerlogg.info("Vedtaksperiode {} i tilstand {} overlappet med inntektsmelding {}",
                keyValue("vedtaksperiodeId", vedtaksperiode.id), keyValue("tilstand", type), keyValue("meldingsreferanseId", inntektsmelding.meldingsreferanseId()))
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, this)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg
        ) {
            aktivitetslogg.info("Mottatt revurdert utbetaling fra en annen vedtaksperiode")
            vedtaksperiode.mottaUtbetalingTilRevurdering(aktivitetslogg, utbetaling)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (vedtaksperiode.arbeidsgiver.avventerRevurdering()) return
            if (vedtaksperiode.utbetalinger.erAvvist()) {
                hendelse.info("Går til revurdering feilet fordi revurdering er avvist")
                return vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
            }
            hendelse.info("Går til avsluttet fordi revurdering er fullført via en annen vedtaksperiode")
            vedtaksperiode.tilstand(hendelse, Avsluttet)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            hendelse.info("Kvittering håndteres av vedtaksperioden som har håndtert utbetaling av revurderingen.")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.emitVedtaksperiodeEndret(hendelse)
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            vedtaksperiode.emitVedtaksperiodeEndret(hendelse)
            vedtaksperiode.revurderInntekt(hendelse)
        }

        override fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.valider(ytelser, arbeidsgiver, periode, skjæringstidspunkt)
            ytelser.valider(periode, skjæringstidspunkt)
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }
    }

    internal object AvventerHistorikkRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
            val periode = vedtaksperiode.skjæringstidspunktperiode()
            vedtaksperiode.trengerYtelser(hendelse, periode)
            vedtaksperiode.finnArbeidsgiverperiode()?.firstOrNull()?.also {
                if (it < 1.oktober(2021)) {
                    hendelse.info(
                        "Arbeidsgiverperioden er beregnet til å starte tidligere enn 1. oktober 2021." +
                                "Denne perioden ville ikke kunne bli utbetalt dersom vi fjerner konverteringen av Utbetaling til Sykdomstidslinje"
                    )
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, this)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            vedtaksperiode.revurderInntekt(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
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
            if (vedtaksperiode.person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt) == null) return vedtaksperiode.tilstand(ytelser, AvventerVilkårsprøvingRevurdering) {
                ytelser.info("Trenger å utføre vilkårsprøving før vi kan beregne utbetaling for revurderingen.")
            }

            ErrorsTilWarnings.wrap(ytelser) {
                vedtaksperiode.validerYtelserForSkjæringstidspunkt(ytelser)
                val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                vedtaksperiode.forsøkRevurdering(arbeidsgiverUtbetalinger, ytelser)
            }
        }

        override fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.valider(ytelser, arbeidsgiver, periode, skjæringstidspunkt)
            ytelser.valider(periode, skjæringstidspunkt)
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }
    }

    internal object AvventerVilkårsprøvingRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            ErrorsTilWarnings.wrap(vilkårsgrunnlag) { vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikkRevurdering) }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            ErrorsTilWarnings.wrap(hendelse) {
                vedtaksperiode.person.nyInntekt(hendelse)
                vedtaksperiode.person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
                    hendelse,
                    vedtaksperiode.skjæringstidspunkt,
                    vedtaksperiode.jurist(),
                null,
                null
            )
            }
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }
    }

    internal object AvventerInntektsmeldingEllerHistorikk : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerInntektsmeldingReplay()
            if (vedtaksperiode.arbeidsgiver.finnForkastetSykeperiodeRettFør(vedtaksperiode) == null) {
                vedtaksperiode.trengerInntektsmelding(hendelse.hendelseskontekst())
            }
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding(aktivitetslogg.hendelseskontekst())
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {}

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {}

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            validation(hendelse) {
                onValidationFailed { person.invaliderAllePerioder(hendelse, null) }
                valider {
                    infotrygdhistorikk.validerOverlappende(
                        this,
                        arbeidsgiver.avgrensetPeriode(vedtaksperiode.periode),
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                onSuccess {
                    infotrygdhistorikk.addInntekter(person, this)
                    if (vedtaksperiode.ingenUtbetaling()) {
                        vedtaksperiode.utbetalinger.forkast(hendelse)
                        vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
                    } else if (vedtaksperiode.harNødvendigInntektForVilkårsprøving()) {
                        info("Oppdaget at perioden startet i infotrygd")
                        vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
                    }
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }

        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, arbeidsgivere: Iterable<Arbeidsgiver>, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerHistorikkFraInfotrygd(hendelse)
        }
    }

    internal object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_BLOKKERENDE_PERIODE

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {}

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            when {
                vedtaksperiode.arbeidsgiver.validerBrukerutbetaling(hendelse, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode) ->
                    vedtaksperiode.person.invaliderAllePerioder(hendelse, "Forkaster perioden pga. brukerutbetaling")
                arbeidsgivere.trengerSøknadISammeMåned(vedtaksperiode.skjæringstidspunkt) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding i samme måned som skjæringstidspunktet"
                )
                arbeidsgivere.trengerSøknadFør(vedtaksperiode.periode) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding i samme måned som skjæringstidspunktet"
                )
                arbeidsgivere.senerePerioderPågående(vedtaksperiode) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi det finnes pågående revurderinger."
                )
                vedtaksperiode.ingenUtbetaling() -> vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
                !arbeidsgivere.harNødvendigInntekt(vedtaksperiode.skjæringstidspunkt) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én arbeidsgiver ikke har tilstrekkelig inntekt for skjæringstidspunktet"
                )
                else -> vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
            if (vedtaksperiode.arbeidsgiver.harSykmeldingsperiodeFør(vedtaksperiode.periode.endInclusive.plusDays(1))) {
                sikkerlogg.warn("Har sykmeldingsperiode før eller lik tom. VedtaksperiodeId=${vedtaksperiode.id}, aktørId=${påminnelse.aktørId()}")
            }
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusDays(5)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikk)
        }
    }

    // TODO: slett tilstand
    internal object AvventerUferdig : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (!vedtaksperiode.harNødvendigInntektForVilkårsprøving())
                return vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingEllerHistorikk)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode) {
                påminnelse.info("Bytter til Avventer blokkerende periode for å migrere oss bort fra AvventerUferdig")
            }
        }
    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.loggInnenforArbeidsgiverperiode()
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.utbetalinger.forkast(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
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
            val periodetype = vedtaksperiode.periodetype
            validation(ytelser) {
                onValidationFailed {
                    if (!ytelser.hasErrorsOrWorse()) error("Behandling av Ytelser feilet, årsak ukjent")
                    vedtaksperiode.forkast(ytelser)
                }
                onSuccess {
                    vedtaksperiode.skjæringstidspunktFraInfotrygd = person.skjæringstidspunkt(vedtaksperiode.periode)
                }
                valider {
                    infotrygdhistorikk.valider(
                        this,
                        arbeidsgiver,
                        vedtaksperiode.periode,
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                validerHvis("Har ikke overgang for alle arbeidsgivere i Infotrygd", periodetype == OVERGANG_FRA_IT) {
                    person.kunOvergangFraInfotrygd(vedtaksperiode)
                }
                validerHvis(
                    "Har utbetalinger fra andre arbeidsgivere etter skjæringstidspunktet",
                    periodetype == OVERGANG_FRA_IT
                ) {
                    person.ingenUkjenteArbeidsgivere(vedtaksperiode, vedtaksperiode.skjæringstidspunkt)
                }
                onSuccess { infotrygdhistorikk.addInntekter(person, this) }
                onSuccess {
                    person.lagreVilkårsgrunnlagFraInfotrygd(
                        vedtaksperiode.skjæringstidspunkt,
                        vedtaksperiode.periode(),
                        vedtaksperiode.jurist()
                    )
                }
                onSuccess {
                    if (person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt) == null) {
                        return@håndter vedtaksperiode.tilstand(ytelser, AvventerVilkårsprøving) {
                            // TODO: Mangler ofte vilkårsgrunnlag for perioder (https://logs.adeo.no/goto/844ac8a834ecd9c7ee5022ba0f89e569).
                            info("Mangler vilkårsgrunnlag for ${vedtaksperiode.skjæringstidspunkt}")
                        }
                    }
                }
                lateinit var vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
                onSuccess {
                    vilkårsgrunnlag = requireNotNull(person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt))
                    ytelser.kontekst(vilkårsgrunnlag)
                }
                valider {
                    person.valider(this, vilkårsgrunnlag, vedtaksperiode.skjæringstidspunkt, arbeidsgiver.finnVedtaksperiodeRettFør(vedtaksperiode) != null)
                }
                onSuccess {
                    vedtaksperiode.inntektskilde = vilkårsgrunnlag.inntektskilde()
                    if (vedtaksperiode.inntektsmeldingInfo == null) {
                        arbeidsgiver.finnTidligereInntektsmeldinginfo(vedtaksperiode.skjæringstidspunkt)?.also { vedtaksperiode.kopierManglende(it) }
                    }
                }
                lateinit var arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    person.fyllUtPeriodeMedForventedeDager(
                        ytelser,
                        vedtaksperiode.periode,
                        vedtaksperiode.skjæringstidspunkt
                    )
                    arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                    arbeidsgiver.beregn(this, arbeidsgiverUtbetalinger, vedtaksperiode.periode, mapOf(vedtaksperiode.periode to (this to vedtaksperiode.jurist())))
                }
                onSuccess {
                    if (vedtaksperiode.person.harKunEttAnnetRelevantArbeidsforholdEnn(
                            vedtaksperiode.skjæringstidspunkt,
                            vedtaksperiode.organisasjonsnummer
                        )
                    ) {
                        ytelser.warn("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig.")
                    } else if (vedtaksperiode.skalHaWarningForFlereArbeidsforholdUtenSykdomEllerUlikStartdato(vilkårsgrunnlag)) {
                        ytelser.warn("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold")
                    }
                    vedtaksperiode.forsøkUtbetaling(arbeidsgiverUtbetalinger.maksimumSykepenger, ytelser)
                }
            }
        }

    }

    private fun loggInnenforArbeidsgiverperiode() {
        if (!ingenUtbetaling()) return
        sikkerlogg.info(
            "Vedtaksperioden {} for {} er egentlig innenfor arbeidsgiverperioden ved {}",
            keyValue("vedtaksperiodeId", id), keyValue("fnr", fødselsnummer), keyValue("tilstand", tilstand.type)
        )
    }

    private fun kopierManglende(other: InntektsmeldingInfo) {
        this.inntektsmeldingInfo = other
        other.leggTil(this.hendelseIder)
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING
        override val kanReberegnes = false

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetalinger.valider(simulering).hasErrorsOrWorse())
                return vedtaksperiode.forkast(simulering)

            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return

            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.simuler(hendelse)
        }
    }

    internal object AvventerSimuleringRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING_REVURDERING
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.simuler(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.utbetalinger.simuler(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            ErrorsTilWarnings.wrap(simulering) {
                if (vedtaksperiode.utbetalinger.valider(simulering).hasWarningsOrWorse()) {
                    simulering.warn("Simulering av revurdert utbetaling feilet. Utbetalingen må annulleres")
                }
            }
            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return simulering.info("Kan ikke gå videre da begge oppdragene ikke er simulert.")
            vedtaksperiode.tilstand(simulering, AvventerGodkjenningRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            vedtaksperiode.revurderInntekt(hendelse)
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerHistorikk)
            if (søknad.hasErrorsOrWorse()) return
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            super.håndter(vedtaksperiode, inntektsmelding)
            vedtaksperiode.tilstand(inntektsmelding, AvventerHistorikk)
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
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanOverstyreInntektForFlereArbeidsgivere(hendelse)) return
            if (vedtaksperiode.person.harPeriodeSomBlokkererOverstyring(hendelse.skjæringstidspunkt)) return
            vedtaksperiode.arbeidsgiver.addInntekt(hendelse)
            vedtaksperiode.person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
                hendelse,
                vedtaksperiode.skjæringstidspunkt,
                vedtaksperiode.jurist(),
                null,
                null
            )
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
        }

        override fun håndterOverstyringAvGhostInntekt(
            vedtaksperiode: Vedtaksperiode,
            hendelse: OverstyrInntekt
        ): Boolean {
            vedtaksperiode.person.lagreInntekt(hendelse)
            vedtaksperiode.person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
                hendelse,
                vedtaksperiode.skjæringstidspunkt,
                vedtaksperiode.jurist(),
                hendelse.forklaring,
                hendelse.subsumsjon
            )
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
            return true
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            overstyrArbeidsforhold: OverstyrArbeidsforhold,
            subsumsjon: Subsumsjon?
        ): Boolean {
            vedtaksperiode.person.lagreOverstyrArbeidsforhold(overstyrArbeidsforhold, vedtaksperiode.jurist())
            vedtaksperiode.person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
                overstyrArbeidsforhold,
                vedtaksperiode.skjæringstidspunkt,
                vedtaksperiode.jurist(),
                null,
                subsumsjon
            )
            vedtaksperiode.tilstand(overstyrArbeidsforhold, AvventerHistorikk)
            return true
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerHistorikkFraInfotrygd(påminnelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            if (vedtaksperiode.utbetalinger.erHistorikkEndretSidenBeregning(infotrygdhistorikk)) return vedtaksperiode.tilstand(
                hendelse,
                AvventerHistorikk
            ) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
            }

            vedtaksperiode.trengerGodkjenning(hendelse)
        }

    }

    private fun trengerGodkjenning(hendelse: IAktivitetslogg) {
        utbetalinger.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            førstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING,
            inntektskilde = inntektskilde,
            arbeidsforholdId = inntektsmeldingInfo?.arbeidsforholdId,
            orgnummereMedRelevanteArbeidsforhold = person.relevanteArbeidsgivere(skjæringstidspunkt),
            aktivitetslogg = person.aktivitetslogg.logg(this)
        )
    }

    internal fun harPassertPunktetHvorViOppdagerFlereArbeidsgivere(): Boolean =
        listOf(
            AVVENTER_GODKJENNING,
            AVVENTER_GODKJENNING_REVURDERING,
            AVVENTER_SIMULERING,
            AVVENTER_SIMULERING_REVURDERING,
            AVSLUTTET,
            REVURDERING_FEILET,
            AVSLUTTET_UTEN_UTBETALING
        ).contains(tilstand.type)

    internal fun gjenopptaBehandling(hendelse: IAktivitetslogg, arbeidsgivere: Iterable<Arbeidsgiver>) {
        hendelse.kontekst(arbeidsgiver)
        kontekst(hendelse)
        hendelse.info("Forsøker å gjenoppta $this")
        tilstand.gjenopptaBehandling(this, arbeidsgivere, hendelse)
    }

    private fun nesteRevurderingstilstand(hendelse: IAktivitetslogg, overstyrt: Vedtaksperiode, pågående: Vedtaksperiode?) {
        // Jeg og revurderingen jeg hører til har en utbetaling in flight
        if (utbetalinger.utbetales()) return

        // en annen revurdering sin utbetaling er in flight
        if (pågående?.utbetalinger?.utbetales() == true) return tilstand(hendelse, AvventerRevurdering)

        // en annen arbeidsgiver revurderes
        if (overstyrt.arbeidsgiver != this.arbeidsgiver) return tilstand(hendelse, AvventerRevurdering)

        // Jeg har et senere skjæringstidspunkt enn den overstyrte perioden
        if (skjæringstidspunkt > overstyrt.skjæringstidspunkt) return tilstand(hendelse, AvventerRevurdering)

        // Det finnes en pågående revurdering _og_ jeg har et senere skjæringstidspunkt enn denne
        if (pågående != null && skjæringstidspunkt > pågående.skjæringstidspunkt) return tilstand(hendelse, AvventerRevurdering)

        // Jeg hører ikke sammen med den overstyrte perioden sin utbetaling (korrelasjonsid), men jeg har samme skjæringstidspunkt. Det er en Infotrygdperiode mellom meg og den overstyrte perioden.
        if (utbetalinger.hørerIkkeSammenMed(overstyrt.utbetalinger) && skjæringstidspunkt == overstyrt.skjæringstidspunkt) return tilstand(hendelse, AvventerRevurdering)

        // Jeg er ikke in flight, og det er heller ingen andre. Det er min arbeidsgiver som revurderes.
        // Jeg har samme som eller et tidligere skjæringstidspunkt enn den overstyrte perioden.
        // Hvis det finnes en pågående revurdering så har jeg samme som eller et tidligere skjæringstidspunkt enn revurderingen.
        // Jeg _kan_ høre til den overstyrte periodens utbetaling, eller jeg _kan_ ha et tidligere skjæringstidspunkt enn
        // den overstyrte perioden, _eller_ begge deler.
        tilstand(hendelse, AvventerGjennomførtRevurdering)
    }

    // ny revurderingsflyt
    private fun startRevurdering(arbeidsgivere: List<Arbeidsgiver>, hendelse: IAktivitetslogg, overstyrt: Vedtaksperiode, pågående: Vedtaksperiode?, førsteRevurderingsdag: LocalDate) {
        if (overstyrt etter this && this.periode().endInclusive < førsteRevurderingsdag) return
        kontekst(hendelse)
        tilstand.startRevurdering(arbeidsgivere, this, hendelse, overstyrt, pågående)
    }

    private fun validerYtelser(ytelser: Ytelser, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk) {
        kontekst(ytelser)
        tilstand.valider(this, periode, skjæringstidspunkt, arbeidsgiver, ytelser, infotrygdhistorikk)
    }

    internal object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING_REVURDERING
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerGodkjenning(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            vedtaksperiode.revurderInntekt(hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                when {
                    vedtaksperiode.utbetalinger.erAvvist() -> RevurderingFeilet.also {
                        utbetalingsgodkjenning.warn("Utbetaling av revurdert periode ble avvist av saksbehandler. Utbetalingen må annulleres")
                    }
                    vedtaksperiode.utbetalinger.harUtbetalinger() -> TilUtbetaling
                    else -> Avsluttet
                }
            )
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }
    }

    // TODO: slett tilstand
    internal object AvventerArbeidsgivereRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_ARBEIDSGIVERE_REVURDERING
        override val kanReberegnes = false

        // Skal denne trigge polling i Speil? Se VenterPåKiling

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val kanForkastes = false
        override val kanReberegnes = false


        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som har gått til utbetaling")
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            when {
                vedtaksperiode.utbetalinger.harFeilet() -> vedtaksperiode.tilstand(hendelse, UtbetalingFeilet) {
                    hendelse.error("Utbetaling ble ikke gjennomført")
                }
                vedtaksperiode.utbetalinger.erUtbetalt() -> vedtaksperiode.tilstand(hendelse, Avsluttet) {
                    hendelse.info("OK fra Oppdragssystemet")
                }
            }
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            when {
                vedtaksperiode.utbetalinger.erUbetalt() -> vedtaksperiode.tilstand(påminnelse, AvventerHistorikk)
                vedtaksperiode.utbetalinger.erUtbetalt() -> vedtaksperiode.tilstand(påminnelse, Avsluttet)
                vedtaksperiode.utbetalinger.harFeilet() -> vedtaksperiode.tilstand(påminnelse, UtbetalingFeilet)
            }
        }
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET
        override val kanForkastes = false
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            sjekkUtbetalingstatus(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.utbetalinger.kanIkkeForsøkesPåNy()) {
                return vedtaksperiode.utbetalinger.reberegnUtbetaling({
                    vedtaksperiode.person.startRevurdering(vedtaksperiode, påminnelse)
                }) {
                    vedtaksperiode.tilstand(påminnelse, AvventerHistorikk) {
                        påminnelse.info("Reberegner periode ettersom utbetaling er avvist og ikke kan forsøkes på nytt")
                    }
                }
            }
            sjekkUtbetalingstatus(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode med utbetaling som har feilet")
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            if (vedtaksperiode != overstyrt) return
            check(pågående != null && vedtaksperiode == pågående) { "Skal ikke kunne restarte revurdering i $type dersom det ikke er oss selv som har igangsatt revurderingen" }
            vedtaksperiode.tilstand(hendelse, AvventerGjennomførtRevurdering)
        }

        private fun sjekkUtbetalingstatus(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (!vedtaksperiode.utbetalinger.erUtbetalt()) return
            vedtaksperiode.tilstand(hendelse, Avsluttet) {
                hendelse.info("OK fra Oppdragssystemet")
            }
        }
    }

    internal object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING

        override val erFerdigBehandlet = true
        override val kanForkastes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            check(!vedtaksperiode.utbetalinger.harUtbetaling()) { "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. " }
            vedtaksperiode.sendVedtakFattet(hendelse)
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            if (!vedtaksperiode.harNødvendigInntektForVilkårsprøving() || !arbeidsgivere.harNødvendigInntekt(vedtaksperiode.skjæringstidspunkt)) return vedtaksperiode.tilstand(hendelse, AvventerRevurdering) {
                hendelse.info("Avventer inntekt for minst én annen arbeidsgiver")
            }
            return vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            val revurderingIkkeStøttet = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING(vedtaksperiode)).isNotEmpty()
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, this) // håndterInntektsmelding krever tilstandsendring, men vi må avvente til vi starter revurdering

            if (inntektsmelding.hasErrorsOrWorse()) return
            if (vedtaksperiode.ingenUtbetaling()) {
                vedtaksperiode.emitVedtaksperiodeEndret(inntektsmelding) // på stedet hvil!
                return inntektsmelding.info("Revurdering medfører ingen utbetaling, blir stående i avsluttet uten utbetaling")
            }
            // støttes ikke før vi støtter revurdering av eldre skjæringstidspunkt
            if (revurderingIkkeStøttet) {
                sikkerlogg.info(
                    "inntektsmelding i auu: Kan ikke reberegne {} for {} {} fordi nyere skjæringstidspunkt blokkerer",
                    keyValue("vedtaksperiodeId", vedtaksperiode.id),
                    keyValue("aktørId", vedtaksperiode.aktørId),
                    keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer)
                )
                inntektsmelding.info("Revurdering blokkeres fordi det finnes nyere skjæringstidspunkt, og det mangler funksjonalitet for å håndtere dette.")
                return vedtaksperiode.emitVedtaksperiodeEndret(inntektsmelding) // på stedet hvil!
            }
            inntektsmelding.info("Igangsetter revurdering ettersom det skal utbetales noe i perioden likevel")
            vedtaksperiode.person.startRevurdering(vedtaksperiode, inntektsmelding)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet uten utbetaling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            if (Toggle.RevurdereInntektMedFlereArbeidsgivere.disabled && vedtaksperiode.inntektskilde == Inntektskilde.FLERE_ARBEIDSGIVERE) {
                hendelse.error("Forespurt revurdering av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
            }
        }
    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override val erFerdigBehandlet = true
        override val kanForkastes = false
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.lås()
            check(vedtaksperiode.utbetalinger.erAvsluttet()) {
                "forventer at utbetaling skal være avsluttet"
            }
            vedtaksperiode.sendVedtakFattet(hendelse)
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.skjæringstidspunktFraInfotrygd = vedtaksperiode.person.skjæringstidspunkt(vedtaksperiode.periode)
            vedtaksperiode.låsOpp()
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        // ny revurderingsflyt
        override fun startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            overstyrt: Vedtaksperiode,
            pågående: Vedtaksperiode?
        ) {
            vedtaksperiode.skjæringstidspunktFraInfotrygd = null
            vedtaksperiode.nesteRevurderingstilstand(hendelse, overstyrt, pågående)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.låsOpp()
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.lås()
            vedtaksperiode.person.startRevurdering(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.kanRevurdereInntektForFlereArbeidsgivere(hendelse)) return
            vedtaksperiode.person.nyInntekt(hendelse)
            vedtaksperiode.person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
                hendelse,
                vedtaksperiode.skjæringstidspunkt,
                vedtaksperiode.jurist(),
                    null,
                    null
                )
                vedtaksperiode.person.startRevurdering(vedtaksperiode, hendelse)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            overstyrArbeidsforhold: OverstyrArbeidsforhold,
            subsumsjon: Subsumsjon?
        ): Boolean {
            vedtaksperiode.person.lagreOverstyrArbeidsforhold(overstyrArbeidsforhold, vedtaksperiode.jurist())
            vedtaksperiode.person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(
                overstyrArbeidsforhold,
                vedtaksperiode.skjæringstidspunkt,
                vedtaksperiode.jurist(),
                null,
                subsumsjon
            )
            vedtaksperiode.person.startRevurdering(vedtaksperiode, overstyrArbeidsforhold)
            return true
        }

        override fun nyPeriodeFørMedNyFlyt(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Søknad) {
            if (Toggle.RevurdereOutOfOrder.disabled) return super.nyPeriodeFørMedNyFlyt(vedtaksperiode, ny, hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

    }

    internal object RevurderingFeilet : Vedtaksperiodetilstand {
        override val type: TilstandType = REVURDERING_FEILET
        override val kanReberegnes = false

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (vedtaksperiode.utbetalinger.harAvsluttede()) return hendelse.info("Gjenopptar ikke revurdering feilet fordi perioden har tidligere avsluttede utbetalinger. Må behandles manuelt vha annullering.")
            hendelse.error("Forkaster avvist revurdering ettersom vedtaksperioden ikke har tidligere utbetalte utbetalinger.")
            vedtaksperiode.forkast(hendelse, SENERE_INCLUSIVE(vedtaksperiode))
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            påminnelse.info("Forsøker å gjenoppta behandling i tilfelle perioder er stuck")
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val erFerdigBehandlet = true
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            sendOppgaveEvent(vedtaksperiode, hendelse)
        }

        private fun sendOppgaveEvent(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (!skalOppretteOppgave(vedtaksperiode)) return
            val inntektsmeldingIds =
                vedtaksperiode.arbeidsgiver.finnSammenhengendePeriode(vedtaksperiode.skjæringstidspunkt)
                    .mapNotNull { it.inntektsmeldingInfo }.ider()
            vedtaksperiode.person.sendOppgaveEvent(
                hendelse = hendelse,
                periode = vedtaksperiode.periode(),
                hendelseIder = vedtaksperiode.hendelseIder() + inntektsmeldingIds
            )
        }

        private fun skalOppretteOppgave(vedtaksperiode: Vedtaksperiode) =
            vedtaksperiode.inntektsmeldingInfo != null ||
                    vedtaksperiode.arbeidsgiver.finnSammenhengendePeriode(vedtaksperiode.skjæringstidspunkt)
                        .any { it.inntektsmeldingInfo != null } ||
                    vedtaksperiode.sykdomstidslinje.any { it.kommerFra(Søknad::class) }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er sendt til Infotrygd")
        }
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private const val IKKE_HÅNDTERT: Boolean = false

        private val AVSLUTTET_OG_SENERE = listOf(
            Avsluttet,
            AvventerRevurdering,
            AvventerGjennomførtRevurdering,
            AvventerArbeidsgivereRevurdering,
            AvventerHistorikkRevurdering,
            AvventerVilkårsprøvingRevurdering,
            AvventerSimuleringRevurdering,
            AvventerGodkjenningRevurdering,
            RevurderingFeilet
        )

        internal val SENERE_INCLUSIVE = fun(senereEnnDenne: Vedtaksperiode): VedtaksperiodeFilter {
            return fun(vedtaksperiode: Vedtaksperiode) = vedtaksperiode >= senereEnnDenne
        }

        internal val OVERLAPPENDE = fun(periode: Periode): VedtaksperiodeFilter {
            return fun(other: Vedtaksperiode) = other.periode.overlapperMed(periode)
        }

        // Fredet funksjonsnavn
        internal val TIDLIGERE_OG_ETTERGØLGENDE = fun(periode: Periode): VedtaksperiodeFilter {
            // forkaster perioder som er før/overlapper med oppgitt periode, eller som er sammenhengende med
            // perioden som overlapper (per arbeidsgiver!).
            return fun(segSelv: Vedtaksperiode) =
                segSelv.person.nåværendeVedtaksperioder(OVERLAPPENDE(periode)).any { other ->
                    other.arbeidsgiver == segSelv.arbeidsgiver && (segSelv <= other || other.skjæringstidspunkt == segSelv.skjæringstidspunkt)
                }
        }

        internal val OVERLAPPER_ELLER_FORLENGER = fun (vedtaksperiode: Vedtaksperiode): VedtaksperiodeFilter {
            val overlappende = vedtaksperiode.person.vedtaksperioder(OVERLAPPENDE(vedtaksperiode.periode()))
            val forlengelser = vedtaksperiode.person.vedtaksperioder(ALLE).fold(mutableListOf(vedtaksperiode)) { identifiserteForlengelser, potensiellForlengelse ->
                if (identifiserteForlengelser.any { forlengelse -> forlengelse.erVedtaksperiodeRettFør(potensiellForlengelse) }) {
                    identifiserteForlengelser.add(potensiellForlengelse)
                }
                identifiserteForlengelser
            }
            return fun (other: Vedtaksperiode) = other in overlappende || other in forlengelser
        }

        internal val IKKE_FERDIG_REVURDERT: VedtaksperiodeFilter = { it.tilstand == AvventerGjennomførtRevurdering }

        internal val KLAR_TIL_BEHANDLING: VedtaksperiodeFilter = {
            it.tilstand == AvventerBlokkerendePeriode || it.tilstand == AvventerGodkjenning
        }

        internal val ALLE_AVVENTER_ARBEIDSGIVERE: VedtaksperiodeFilter = { other: Vedtaksperiode ->
            other.tilstand.erFerdigBehandlet || other.tilstand == AvventerBlokkerendePeriode
        }

        internal val IKKE_FERDIG_BEHANDLET: VedtaksperiodeFilter = { !it.tilstand.erFerdigBehandlet }

        internal val ER_ELLER_HAR_VÆRT_AVSLUTTET: VedtaksperiodeFilter =
            { it.tilstand is AvsluttetUtenUtbetaling || it.utbetalinger.harAvsluttede() }

        internal val MED_SKJÆRINGSTIDSPUNKT = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.skjæringstidspunkt == skjæringstidspunkt }
        }

        internal val SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode ->
                MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt)(vedtaksperiode) && !vedtaksperiode.ingenUtbetaling()
            }
        }

        internal val NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING = { segSelv: Vedtaksperiode ->
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            { vedtaksperiode: Vedtaksperiode ->
                vedtaksperiode.utbetalinger.erAvsluttet() && vedtaksperiode.skjæringstidspunkt > skjæringstidspunkt && vedtaksperiode.skjæringstidspunkt > segSelv.periode.endInclusive
            }
        }

        internal val ALLE: VedtaksperiodeFilter = { true }

        internal fun Iterable<Vedtaksperiode>.harNødvendigInntekt(skjæringstidspunkt: LocalDate) = this
            .filter(SKAL_INNGÅ_I_SYKEPENGEGRUNNLAG(skjæringstidspunkt))
            .all { it.harNødvendigInntektForVilkårsprøving() }

        internal fun List<Vedtaksperiode>.lagUtbetalinger(
            builder: Utbetaling.Builder,
            vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk
        ) {
            forEach { periode ->
                periode.arbeidsgiver.lagUtbetaling(builder, vilkårsgrunnlagHistorikk)
                periode.utbetalinger.lagUtbetaling(builder, periode, periode.organisasjonsnummer)
            }
        }

        internal fun List<Vedtaksperiode>.lagRevurdering(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
            hendelse: ArbeidstakerHendelse
        ) {
            RevurderingUtbetalinger(this, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.organisasjonsnummer, hendelse).utbetal(arbeidsgiverUtbetalinger)
        }

        internal fun aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode: Vedtaksperiode): Aktivitetslogg {
            val tidligereUbetalt =
                vedtaksperiode.arbeidsgiver.finnSykeperioderAvsluttetUtenUtbetalingRettFør(vedtaksperiode)
            val aktivitetskontekster = listOf(vedtaksperiode) + tidligereUbetalt
            return vedtaksperiode.person.aktivitetslogg.logg(*aktivitetskontekster.toTypedArray())
        }

        internal fun Iterable<Vedtaksperiode>.nåværendeVedtaksperiode(filter: VedtaksperiodeFilter) =
            firstOrNull(filter)

        internal fun List<Vedtaksperiode>.medSkjæringstidspunkt(skjæringstidspunkt: LocalDate) =
            this.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        internal fun List<Vedtaksperiode>.harUtbetaling() = any { it.erUtbetalt() }

        internal fun overlapperMedForkastet(forkastede: Iterable<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse) {
            forkastede
                .filter { it.periode.overlapperMed(hendelse.periode()) }
                .forEach {
                    hendelse.error("Søknad overlapper med forkastet vedtaksperiode")
                    hendelse.info("Søknad overlapper med forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                }
        }

        internal fun forlengerForkastet(forkastede: Iterable<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse) {
            forkastede
                .filter { it.sykdomstidslinje.erRettFør(hendelse.sykdomstidslinje()) }
                .forEach {
                    if (Toggle.IkkeForlengInfotrygdperioder.enabled) {
                        hendelse.error("Søknad forlenger en forkastet periode")
                        hendelse.info("Søknad forlenger forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                    } else {
                        hendelse.info("Søknad forlenger forkastet vedtaksperiode")
                    }
                }
        }

        internal fun sjekkOmOverlapperMedForkastet(
            forkastede: Iterable<Vedtaksperiode>,
            inntektsmelding: Inntektsmelding
        ) =
            forkastede.any { it.periode.overlapperMed(inntektsmelding.periode()) }

        internal fun Iterable<Vedtaksperiode>.harOverlappendeUtbetaltePerioder(periode: Periode) =
            any { it.periode().overlapperMed(periode) && it.harPassertPunktetHvorViOppdagerFlereArbeidsgivere() }

        internal fun List<Vedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            filter { it.utbetalinger.harId(utbetalingId) }.map { it.id }

        internal fun List<Vedtaksperiode>.periode(): Periode {
            val fom = minOf { it.periode.start }
            val tom = maxOf { it.periode.endInclusive }
            return Periode(fom, tom)
        }

        internal fun kunOvergangFraInfotrygd(vedtaksperiode: Vedtaksperiode, vedtaksperioder: List<Vedtaksperiode>) =
            vedtaksperioder
                .filter { it.periode().overlapperMed(vedtaksperiode.periode()) }
                .all { it.periodetype == OVERGANG_FRA_IT }

        internal fun arbeidsgiverperiodeFor(
            person: Person,
            sykdomshistorikkId: UUID,
            perioder: List<Vedtaksperiode>,
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            subsumsjonObserver: SubsumsjonObserver
        ): List<Arbeidsgiverperiode> {
            val samletSykdomstidslinje =
                Sykdomstidslinje.gammelTidslinje(perioder.map { it.sykdomstidslinje }).merge(sykdomstidslinje, replace)
            return person.arbeidsgiverperiodeFor(
                organisasjonsnummer,
                sykdomshistorikkId,
                samletSykdomstidslinje,
                subsumsjonObserver
            )
        }

        internal fun List<Vedtaksperiode>.avventerRevurdering() = pågående() != null

        internal fun Iterable<Vedtaksperiode>.senerePerioderPågående(vedtaksperiode: Vedtaksperiode) =
            this.pågående()?.let { it etter vedtaksperiode } ?: false

        // Ny revurderingsflyt
        // Finner eldste vedtaksperiode, foretrekker eldste arbeidsgiver ved likhet (ag1 før ag2)
        internal fun List<Vedtaksperiode>.kanStarteRevurdering(arbeidsgivere: List<Arbeidsgiver>, vedtaksperiode: Vedtaksperiode): Boolean {
            val pågående = pågående() ?: nesteRevurderingsperiode(arbeidsgivere, vedtaksperiode)
            return vedtaksperiode == pågående && arbeidsgivere.harNødvendigInntekt(vedtaksperiode.skjæringstidspunkt)
        }

        private fun List<Vedtaksperiode>.nesteRevurderingsperiode(arbeidsgivere: List<Arbeidsgiver>, other: Vedtaksperiode) =
            sortedWith(compareBy({ it }, { arbeidsgivere.indexOf(it.arbeidsgiver) })).firstOrNull(IKKE_FERDIG_BEHANDLET) ?: other

        internal fun Map<Arbeidsgiver, List<Vedtaksperiode>>.startRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            overstyrt: Vedtaksperiode,
            hendelse: IAktivitetslogg
        ) {
            val vedtaksperioder = this.values.flatten()
            val pågående = vedtaksperioder.pågående()

            val førsteRevurderingsdag = vedtaksperioder.førsteRevurderingsdag() ?: overstyrt.periode().start
            vedtaksperioder.forEach { it.startRevurdering(arbeidsgivere, hendelse, overstyrt, pågående, førsteRevurderingsdag) }

            val siste = vedtaksperioder.lastOrNull { it.tilstand == AvventerGjennomførtRevurdering } ?: return
            siste.kontekst(hendelse)
            hendelse.info("Forsøker å igangsette revurdering for $siste")
            if (vedtaksperioder.pågående() != null) return hendelse.info("Det er en pågående utbetaling, avventer igangsetting av revurdering.")
            siste.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        private fun Iterable<Vedtaksperiode>.pågående() =
            firstOrNull {
                it.tilstand in setOf(
                    AvventerHistorikkRevurdering,
                    AvventerSimuleringRevurdering,
                    AvventerGodkjenningRevurdering,
                    TilUtbetaling,
                    UtbetalingFeilet
                )
            }

        private fun List<Vedtaksperiode>.førsteRevurderingsdag() =
            filter {
                it.tilstand in setOf(
                    AvventerGjennomførtRevurdering,
                    AvventerHistorikkRevurdering,
                    AvventerSimuleringRevurdering,
                    AvventerGodkjenningRevurdering,
                    TilUtbetaling,
                    UtbetalingFeilet
                )
            }.takeIf { it.isNotEmpty() }?.periode()?.start

        internal fun gjenopptaRevurdering(
            arbeidsgivere: List<Arbeidsgiver>,
            hendelse: IAktivitetslogg,
            perioder: List<Vedtaksperiode>,
            første: Vedtaksperiode,
            arbeidsgiver: Arbeidsgiver
        ) {
            mapOf(arbeidsgiver to perioder).startRevurdering(arbeidsgivere, første, hendelse)
        }

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
            inntektskilde: Inntektskilde,
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
            inntektskilde = inntektskilde,
            opprettet = opprettet,
            oppdatert = oppdatert,
            jurist = medVedtaksperiode
        )

        internal fun List<Vedtaksperiode>.validerYtelser(ytelser: Ytelser, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk) {
            filter { it.skjæringstidspunkt == skjæringstidspunkt }
                .forEach { it.validerYtelser(ytelser, skjæringstidspunkt, infotrygdhistorikk) }
        }

        internal fun List<Vedtaksperiode>.skjæringstidspunktperiode(skjæringstidspunkt: LocalDate): Periode {
            val sisteDato = filter { it.skjæringstidspunkt == skjæringstidspunkt }.maxOf { it.periode.endInclusive }
            return skjæringstidspunkt til sisteDato
        }

        // Forstår hvordan bygge utbetaling for revurderinger
        internal class RevurderingUtbetalinger(
            vedtaksperioder: List<Vedtaksperiode>,
            skjæringstidspunkt: LocalDate,
            private val organisasjonsnummer: String,
            private val hendelse: ArbeidstakerHendelse
        ) {
            private val beregningsperioder = vedtaksperioder.revurderingsperioder(skjæringstidspunkt)
            private val utbetalingsperioder = vedtaksperioder.utbetalingsperioder(skjæringstidspunkt)

            internal fun utbetal(arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger) {
                val maksimumSykepenger = arbeidsgiverUtbetalinger.utbetal(this, beregningsperioder.periode(), organisasjonsnummer)
                utbetalingsperioder.forEach {
                    it.lagRevurdering(maksimumSykepenger, it.aktivitetsloggkopi())
                }
            }

            internal fun gjødsle(tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>, infotrygdhistorikk: Infotrygdhistorikk) {
                beregningsperioder.forEach {
                    Refusjonsgjødsler(
                        tidslinjer.getValue(it.arbeidsgiver) + it.arbeidsgiver.utbetalingstidslinje(infotrygdhistorikk),
                        it.arbeidsgiver.refusjonshistorikk,
                        infotrygdhistorikk,
                        it.arbeidsgiver.organisasjonsnummer()
                    ).gjødsle(it.aktivitetsloggkopi(), it.periode())
                }
            }

            internal fun filtrer(filtere: List<UtbetalingstidslinjerFilter>, tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>) {
                beregningsperioder.forEach { vedtaksperiode ->
                    val aktivitetslogg = vedtaksperiode.aktivitetsloggkopi()
                    val jurist = vedtaksperiode.jurist()
                    val periode = vedtaksperiode.periode()
                    filtere.forEach {
                        it.filter(tidslinjer.values.toList(), periode, aktivitetslogg, jurist)
                    }
                }
            }

            private fun Vedtaksperiode.aktivitetsloggkopi(): IAktivitetslogg {
                return hendelse.barn().also {
                    it.kontekst(person)
                    kontekst(it)
                }
            }

            private fun List<Vedtaksperiode>.revurderingsperioder(skjæringstidspunkt: LocalDate) =
                filter(ALLE_REVURDERINGSTILSTANDER)
                    .filter { it.skjæringstidspunkt == skjæringstidspunkt }

            private fun List<Vedtaksperiode>.utbetalingsperioder(skjæringstidspunkt: LocalDate) = filter(KAN_OPPRETTE_REVURDERING(skjæringstidspunkt))

            private companion object {
                private val ALLE_REVURDERINGSTILSTANDER: VedtaksperiodeFilter = {
                    it.tilstand in listOf(
                        AvventerRevurdering,
                        AvventerGjennomførtRevurdering,
                        AvventerHistorikkRevurdering,
                        AvventerSimuleringRevurdering,
                        AvventerGodkjenningRevurdering,
                        TilUtbetaling,
                        UtbetalingFeilet
                    )
                }

                /* perioder som kan opprette revurdering er sist på skjæringstidspunktet per arbeidsgiver (hensyntatt pingpong).
                       de som står i AvventerHistorikkRevurdering er allerede funnet, men det kan være flere i samme arbeidsgiver pga. ping-pong eller en annen arbeidsgiver som venter.
                       Felles for disse er at de står i AvventerRevurdering, og de kan identifiseres ved at de ikke blir forlenget av noen andre vedtaksperioder,
                        denne sjekken finner også de som naturligvis er sist per skjæringstidspunkt per arbeidsgiver også */
                private val KAN_OPPRETTE_REVURDERING = { skjæringstidspunkt: LocalDate ->
                    { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.skjæringstidspunkt == skjæringstidspunkt &&
                            (vedtaksperiode.tilstand == AvventerHistorikkRevurdering || (vedtaksperiode.tilstand == AvventerRevurdering && vedtaksperiode.arbeidsgiver.finnVedtaksperiodeRettEtter(vedtaksperiode) == null)) }
                }
            }
        }
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
}

enum class Inntektskilde {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE
}

internal typealias VedtaksperiodeFilter = (Vedtaksperiode) -> Boolean
