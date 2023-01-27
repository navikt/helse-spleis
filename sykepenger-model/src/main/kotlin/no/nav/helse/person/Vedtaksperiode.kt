package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Toggle
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.delvisOverlappMed
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Påminnelse
import no.nav.helse.hendelser.Simulering
import no.nav.helse.hendelser.Sykmelding
import no.nav.helse.hendelser.Søknad
import no.nav.helse.hendelser.Utbetalingshistorikk
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
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigOpplysningerFraArbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigRefusjonsopplysninger
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.ForlengelseFraInfotrygd.IKKE_ETTERSPURT
import no.nav.helse.person.InntektsmeldingInfo.Companion.ider
import no.nav.helse.person.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.person.TilstandType.AVSLUTTET
import no.nav.helse.person.TilstandType.AVSLUTTET_UTEN_UTBETALING
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
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.TilstandType.UTBETALING_FEILET
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
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OO_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RE_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SI_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_3
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_15
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_16
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_19
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_20
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_28
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
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VV_8
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.etterlevelse.MaskinellJurist
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.serde.reflection.AktivitetsloggMap
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.sammenlign
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
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet,
    jurist: MaskinellJurist
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val jurist = jurist.medVedtaksperiode(id, hendelseIder, sykmeldingsperiode)
    private val skjæringstidspunkt get() = person.skjæringstidspunkt(sykdomstidslinje.sykdomsperiode() ?: periode)
    private val periodetype get() = arbeidsgiver.periodetype(periode)
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
        utbetalinger = VedtaksperiodeUtbetalinger(arbeidsgiver),
        utbetalingstidslinje = Utbetalingstidslinje(),
        opprettet = LocalDateTime.now(),
        jurist = jurist
    ) {
        kontekst(søknad)
    }

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        val periodetypeMemoized = this::periodetype.memoized()
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
            periodetypeMemoized,
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
            periodetypeMemoized,
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

    internal fun jurist() = jurist.medVedtaksperiode(id, hendelseIder.toSet(), sykmeldingsperiode)

    internal fun harId(vedtaksperiodeId: UUID) = id == vedtaksperiodeId

    internal fun hendelseIder() = hendelseIder.ider()

    internal fun håndter(sykmelding: Sykmelding) {
        sykmelding.trimLeft(periode.endInclusive)
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
        val sammenhengendePerioder = arbeidsgiver.finnSammenhengendeVedtaksperioder(this)
        val overlapper = overlapperMedSammenhengende(inntektsmelding, sammenhengendePerioder, other, vedtaksperioder)
        return overlapper.also {
            if (erAlleredeHensyntatt(inntektsmelding)) {
                sikkerlogg.info("Vedtaksperiode med id=$id har allerede hensyntatt inntektsmeldingen med id=${inntektsmelding.meldingsreferanseId()}, replayes det en inntektsmelding unødvendig?")
                inntektsmelding.trimLeft(periode.endInclusive)
                return@also
            }
            if (it) inntektsmelding.leggTil(hendelseIder)
            if (!it) return@also inntektsmelding.trimLeft(periode.endInclusive)
            kontekst(inntektsmelding)
            if (!inntektsmelding.erRelevant(periode, sammenhengendePerioder.map { periode -> periode.periode })) return@also
            person.nyeArbeidsgiverInntektsopplysninger(skjæringstidspunkt, inntektsmelding, jurist())
            tilstand.håndter(this, inntektsmelding)
            inntektsmelding.trimLeft(periode.endInclusive)
        }
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
            dager.leggTil(hendelseIder)
        }
    }

    private fun forventerInntektOgRefusjonFraInntektsmelding() = tilstand != AvsluttetUtenUtbetaling || forventerInntekt()

    internal fun håndter(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
        if (erAlleredeHensyntatt(inntektOgRefusjon.meldingsreferanseId())) return
        kontekst(inntektOgRefusjon)
        inntektOgRefusjon.leggTil(hendelseIder)
        inntektOgRefusjon.nyeRefusjonsopplysninger(skjæringstidspunkt, person, jurist())
        tilstand.håndter(this, inntektOgRefusjon)
    }

    private fun håndterInntektOgRefusjon(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding, nesteTilstand: () -> Vedtaksperiodetilstand) {
        inntektsmeldingInfo = inntektOgRefusjon.addInntektsmelding(skjæringstidspunkt, arbeidsgiver, jurist)
        inntektOgRefusjon.valider(periode, skjæringstidspunkt)
        inntektOgRefusjon.info("Fullført behandling av inntektsmelding")
        if (inntektOgRefusjon.harFunksjonelleFeilEllerVerre()) return forkast(inntektOgRefusjon)
        tilstand(inntektOgRefusjon, nesteTilstand())
    }

    private fun håndterDager(dager: DagerFraInntektsmelding) {
        dager.leggTilArbeidsdagerFør(periode.start)
        // Ettersom tilstanden kan strekke perioden tilbake må det gjøres _før_ vi hånderer dagene
        // slik at det som håndteres er perioden slik den er når det er strukket tilbake.
        tilstand.håndterStrekkingAvPeriode(this, dager)
        // Håndterer dagene som vedtaksperioden skal håndtere og oppdaterer sykdomstidslinjen
        sykdomstidslinje = dager.håndter(periode, arbeidsgiver)!!
    }

    private fun erAlleredeHensyntatt(inntektsmelding: Inntektsmelding) =
        hendelseIder.ider().contains(inntektsmelding.meldingsreferanseId())

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

    internal fun håndter(hendelse: OverstyrTidslinje) = hendelse.erRelevant(periode).also { erRelevant ->
        if (!erRelevant) return IKKE_HÅNDTERT
        kontekst(hendelse)
        hendelse.leggTil(hendelseIder)
        if (!hendelse.alleredeHåndtert()) {
            hendelse.markerHåndtert()
            tilstand.håndter(this, hendelse)
        }
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
        if (ny.periode.erRettFør(this.periode)) return hendelse.funksjonellFeil(`Mottatt søknad out of order`)
    }

    private fun påvirkerArbeidsgiverperioden(ny: Vedtaksperiode): Boolean {
        val dagerMellom = ny.periode.periodeMellom(this.periode.start)?.count() ?: return false
        // dersom "ny" slutter på en fredag, så starter ikke oppholdstelling før påfølgende mandag.
        // det kan derfor være mer enn 16 dager avstand mellom periodene, og arbeidsgiverperioden kan være den samme
        // Derfor bruker vi tallet 18 fremfor kanskje det forventende 16…
        return dagerMellom < 18L
    }

    internal fun håndterRevurdertUtbetaling(revurdertUtbetaling: Utbetaling, aktivitetslogg: IAktivitetslogg, other: Vedtaksperiode) {
        kontekst(aktivitetslogg)
        tilstand.håndterRevurdertUtbetaling(this, revurdertUtbetaling, aktivitetslogg, other)
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
    private fun harNødvendigInntektForVilkårsprøving() =
        person.harNødvendigInntektForVilkårsprøving(skjæringstidspunkt)

    internal fun harNødvendigOpplysningerFraArbeidsgiver() = tilstand != AvventerInntektsmeldingEllerHistorikk || !forventerInntekt()

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
        person.igangsettOverstyring(hendelse, Revurderingseventyr.sykdomstidslinje(skjæringstidspunkt, periode))
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

    private fun sykefraværstilfelle() = person.sykefraværstilfelle(skjæringstidspunkt)

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

    private fun håndterInntektsmelding(hendelse: Inntektsmelding, nesteTilstand: () -> Vedtaksperiodetilstand) {
        periode = hendelse.oppdaterFom(periode)
        oppdaterHistorikk(hendelse)
        inntektsmeldingInfo = arbeidsgiver.addInntektsmelding(skjæringstidspunkt, hendelse, jurist())
        hendelse.valider(periode, skjæringstidspunkt, finnArbeidsgiverperiode(), jurist())
        hendelse.info("Fullført behandling av inntektsmelding")
        if (hendelse.harFunksjonelleFeilEllerVerre()) return forkast(hendelse)
        tilstand(hendelse, nesteTilstand())
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
    }

    private fun håndterSøknad(søknad: Søknad, nesteTilstand: () -> Vedtaksperiodetilstand? = { null }) {
        periode = periode.oppdaterFom(søknad.periode())
        oppdaterHistorikk(søknad)
        søknad.valider(periode, jurist())
        søknad.validerInntektskilder(vilkårsgrunnlag == null)
        if (søknad.harFunksjonelleFeilEllerVerre()) {
            return forkast(søknad)
        }
        nesteTilstand()?.also { tilstand(søknad, it) }
    }

    private fun overlappendeSøknadIkkeStøttet(søknad: Søknad, egendefinertFeiltekst: Varselkode? = null) {
        søknad.funksjonellFeil(egendefinertFeiltekst ?: RV_SØ_16)
        forkast(søknad)
    }

    private fun håndterOverlappendeSøknad(søknad: Søknad, nesteTilstand: Vedtaksperiodetilstand? = null) {
        if (periode.delvisOverlappMed(søknad.periode())) return overlappendeSøknadIkkeStøttet(søknad, `Mottatt søknad som delvis overlapper`)
        håndterSøknad(søknad) { nesteTilstand }
    }

    private fun håndterOverlappendeSøknadRevurdering(søknad: Søknad) {
        if (periode.delvisOverlappMed(søknad.periode())) return søknad.funksjonellFeil(`Mottatt søknad som delvis overlapper`)
        if (søknad.harArbeidsdager()) søknad.varsel(RV_SØ_15)
        else {
            søknad.valider(periode, jurist())
            søknad.validerInntektskilder(vilkårsgrunnlag == null)
            søknad.info("Søknad har trigget en revurdering")
            if (tilstand == Avsluttet) låsOpp()
            oppdaterHistorikk(søknad)
            if (tilstand == Avsluttet) lås()
        }

        person.igangsettOverstyring(søknad, Revurderingseventyr.korrigertSøknad(skjæringstidspunkt, periode))
    }

    private fun håndterVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand) {
        vilkårsgrunnlag.lagre(person, skjæringstidspunkt)

        val grunnlagForSykepengegrunnlag = person.beregnSykepengegrunnlag(skjæringstidspunkt, jurist())
        val sammenligningsgrunnlag = person.beregnSammenligningsgrunnlag(skjæringstidspunkt, jurist())

        vilkårsgrunnlag.valider(
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            skjæringstidspunkt,
            person.antallArbeidsgivereMedRelevantArbeidsforhold(skjæringstidspunkt),
            jurist()
        )
        person.lagreVilkårsgrunnlag(vilkårsgrunnlag.grunnlagsdata())
        if (vilkårsgrunnlag.harFunksjonelleFeilEllerVerre()) {
            return forkast(vilkårsgrunnlag)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag vurdert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun håndterUtbetalingHendelse(hendelse: UtbetalingHendelse, onUtbetalt: () -> Unit) {
        when {
            utbetalinger.harFeilet() -> tilstand(hendelse, UtbetalingFeilet) {
                hendelse.funksjonellFeil(RV_UT_5)
            }
            utbetalinger.erUtbetalt() -> {
                onUtbetalt()
            }
        }
    }

    private fun ferdigstillVedtak(hendelse: IAktivitetslogg) {
        sendVedtakFattet()
        person.gjenopptaBehandling(hendelse)
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

    private fun trengerArbeidsgiveropplysninger() {
        val arbeidsgiverperiode = finnArbeidsgiverperiode()
        val arbeidsgiverperiodeperioder = arbeidsgiverperiode?.perioder.orEmpty()
        val inntekt = person.vilkårsgrunnlagFor(skjæringstidspunkt)?.inntekt(arbeidsgiver.organisasjonsnummer())
        val beregningsmåneder = 3.downTo(1).map {
            YearMonth.from(skjæringstidspunkt).minusMonths(it.toLong())
        }

        val trengerArbeidsgiverperiode = arbeidsgiver.erFørsteSykedagEtter(periode().start, arbeidsgiverperiode)
            || arbeidsgiverperiodeperioder.maxByOrNull { it.endInclusive }?.overlapperMed(periode())
            ?: false

        val forespurteOpplysninger = listOf(
            (inntekt == null) to PersonObserver.Inntekt(forslag = PersonObserver.Inntektsforslag(beregningsmåneder)),
            (inntekt != null) to inntekt?.let { PersonObserver.FastsattInntekt(fastsattInntekt = it) },
            true to PersonObserver.Refusjon,
            trengerArbeidsgiverperiode to PersonObserver.Arbeidsgiverperiode(arbeidsgiverperiodeperioder)
        ).mapNotNull { if (it.first) it.second else null }

        val vedtaksperioderKnyttetTilArbeidsgiverperiode = arbeidsgiver.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode)

        person.trengerArbeidsgiveropplysninger(
            PersonObserver.TrengerArbeidsgiveropplysningerEvent(
                organisasjonsnummer = organisasjonsnummer,
                sykmeldingsperioder = vedtaksperioderKnyttetTilArbeidsgiverperiode.map { it.periode() },
                vedtaksperiodeId = id,
                forespurteOpplysninger = forespurteOpplysninger.toList()
            )
        )
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
        person.inntektsmeldingReplay(id)
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

    /**
     * Skedulering av utbetaling opp mot andre arbeidsgivere
     */
    private fun forsøkUtbetaling(
        maksimumSykepenger: Alder.MaksimumSykepenger,
        hendelse: ArbeidstakerHendelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
        vedtaksperioder
            .filter { this.periode.overlapperMed(it.periode) && this.skjæringstidspunkt == it.skjæringstidspunkt }
            .forEach { it.lagUtbetaling(maksimumSykepenger, hendelse) }
        høstingsresultater(hendelse)
    }

    private fun lagUtbetaling(maksimumSykepenger: Alder.MaksimumSykepenger, hendelse: ArbeidstakerHendelse) {
        val grunnlagsdata = requireNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for $skjæringstidspunkt, men har ikke. Lages det utbetaling for en periode som " +
                    "ikke skal lage utbetaling?"
        }
        utbetalingstidslinje = utbetalinger.lagUtbetaling(fødselsnummer, id, periode, grunnlagsdata, maksimumSykepenger, hendelse)
    }

    private fun høstingsresultater(hendelse: ArbeidstakerHendelse) {
        val ingenUtbetaling = !utbetalinger.harUtbetalinger()
        when {
            ingenUtbetaling -> {
                tilstand(hendelse, AvventerGodkjenning) {
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

    private fun forsøkRevurdering(
        arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
        hendelse: ArbeidstakerHendelse
    ) {
        person.lagRevurdering(this, arbeidsgiverUtbetalinger, hendelse)
        høstingsresultaterRevurdering(hendelse)
    }

    internal fun lagRevurdering(aktivitetslogg: IAktivitetslogg, orgnummer: String, maksimumSykepenger: Alder.MaksimumSykepenger) {
        aktivitetslogg.kontekst(person)
        kontekst(aktivitetslogg)
        utbetalinger.lagRevurdering(this, fødselsnummer, aktivitetslogg, maksimumSykepenger, periode)
        loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(orgnummer, aktivitetslogg)
    }

    private fun loggDersomViTrekkerTilbakePengerPåAnnenArbeidsgiver(orgnummer: String, aktivitetslogg: IAktivitetslogg) {
        if (!utbetalinger.trekkerTilbakePenger()) return

        if (orgnummer != organisasjonsnummer || person.blitt6GBegrensetSidenSist(skjæringstidspunkt)) {
            aktivitetslogg.info("En endring hos en arbeidsgiver har medført at det trekkes tilbake penger hos andre arbeidsgivere")
        }
    }

    private fun høstingsresultaterRevurdering(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Videresender utbetaling til alle vedtaksperioder innenfor samme fagsystemid som er til revurdering")
        when {
            !utbetalinger.erUbetalt() -> {
                tilstand(hendelse, RevurderingFeilet) {
                    hendelse.info("""Det har ikke blitt laget noen utbetaling fordi Infotrygd har overlappende utbetaling""")
                }
            }
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

    private fun mottaUtbetalingTilRevurdering(utbetaling: Utbetaling) {
        val grunnlagsdata = requireNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for $skjæringstidspunkt, men har ikke. Tildeles det utbetaling til " +
                    "en vedtaksperiode som ikke skal ha utbetaling?"
        }
        utbetalingstidslinje = utbetalinger.mottaRevurdering(id, grunnlagsdata, utbetaling, periode)
    }

    private fun Vedtaksperiodetilstand.påminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
        if (!påminnelse.gjelderTilstand(type)) return vedtaksperiode.person.vedtaksperiodeIkkePåminnet(id, organisasjonsnummer, type)
        vedtaksperiode.person.vedtaksperiodePåminnet(id, organisasjonsnummer, påminnelse)
        if (påminnelse.nåddMakstid(::makstid)) return håndterMakstid(vedtaksperiode, påminnelse)
        håndter(vedtaksperiode, påminnelse)
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    private fun finnArbeidsgiverperiode() =
        arbeidsgiver.arbeidsgiverperiode(
            periode,
            SubsumsjonObserver.NullObserver
        ) // TODO: skal vi logge ved beregning av agp?

    private fun forventerInntekt(): Boolean {
        return Arbeidsgiverperiode.forventerInntekt(finnArbeidsgiverperiode(), periode, sykdomstidslinje, jurist())
    }

    private fun sendOppgaveEvent(hendelse: IAktivitetslogg) {
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
        utbetalinger.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            førstegangsbehandling = periodetype == FØRSTEGANGSBEHANDLING,
            inntektskilde = requireNotNull(person.vilkårsgrunnlagFor(skjæringstidspunkt)?.inntektskilde()),
            arbeidsforholdId = inntektsmeldingInfo?.arbeidsforholdId,
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

    private fun validerYtelser(ytelser: Ytelser, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk) {
        kontekst(ytelser)
        tilstand.valider(this, periode, skjæringstidspunkt, arbeidsgiver, ytelser, infotrygdhistorikk)
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
        hendelse.leggTil(hendelseIder)
        tilstand.håndtertInntektPåSkjæringstidspunktet(this, hendelse)
    }

    fun slutterEtter(dato: LocalDate) = periode.slutterEtter(dato)

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

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad)

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {}

        fun håndterStrekkingAvPeriode(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {}
        fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            val arbeidsgiverperiodeFør = vedtaksperiode.finnArbeidsgiverperiode()
            vedtaksperiode.håndterDager(dager)
            val arbeidsgiverperiodeEtter = vedtaksperiode.finnArbeidsgiverperiode()
            if (!arbeidsgiverperiodeFør.sammenlign(arbeidsgiverperiodeEtter)) {
                // Hvis AGP er uendret så legger vi ikke til varsel om at det er mottatt flere inntektsmeldinger
                // Det kan derimot være at inntekt- og refusjon legger på varselet
                dager.varsel(RV_IM_4)
            }
            return true
        }
        fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            inntektOgRefusjon.varsel(RV_IM_4)
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

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerHistorikkFraInfotrygd(påminnelse, LocalDateTime.now().minusHours(24))
        }

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

        fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg,
            other: Vedtaksperiode
        ) {}

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
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
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
            val harSenereUtbetalinger = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING(vedtaksperiode)).isNotEmpty()
            val harSenereAUU = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_UTEN_UTBETALING(vedtaksperiode)).isNotEmpty()
            if (harSenereUtbetalinger || harSenereAUU) {
                søknad.varsel(RV_OO_1)
            }
            vedtaksperiode.håndterSøknad(søknad) {
                val rettFør = vedtaksperiode.arbeidsgiver.finnVedtaksperiodeRettFør(vedtaksperiode)
                when {
                    rettFør != null && rettFør.tilstand !in setOf(AvsluttetUtenUtbetaling, AvventerInntektsmeldingEllerHistorikk) -> AvventerBlokkerendePeriode
                    else -> AvventerInntektsmeldingEllerHistorikk
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

    internal object AvventerRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if(!vedtaksperiode.harNødvendigInntektForVilkårsprøving()) {
                hendelse.info("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding")
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (!vedtaksperiode.harNødvendigInntektForVilkårsprøving())
                return hendelse.info("Mangler nødvendig inntekt for vilkårsprøving og kan derfor ikke gjenoppta revurdering.")
            vedtaksperiode.tilstand(hendelse, AvventerGjennomførtRevurdering)
            vedtaksperiode.arbeidsgiver.gjenopptaRevurdering(vedtaksperiode, hendelse)
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
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.inntektsmeldingInfo = vedtaksperiode.arbeidsgiver.addInntektsmelding(vedtaksperiode.skjæringstidspunkt, inntektsmelding, vedtaksperiode.jurist())
            FunksjonelleFeilTilVarsler.wrap(inntektsmelding) {
                inntektsmelding.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.finnArbeidsgiverperiode(), vedtaksperiode.jurist())
            }
            inntektsmelding.info("Fullført behandling av inntektsmelding")
            vedtaksperiode.trengerIkkeInntektsmelding()
            vedtaksperiode.person.gjenopptaBehandling(inntektsmelding)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            inntektOgRefusjon.wrap {
                vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon) { this }
            }
            vedtaksperiode.trengerIkkeInntektsmelding()
            vedtaksperiode.person.gjenopptaBehandling(inntektOgRefusjon)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            super.håndter(vedtaksperiode, påminnelse)
            if (!vedtaksperiode.harNødvendigInntektForVilkårsprøving()) {
                påminnelse.info("Varsler arbeidsgiver at vi har behov for inntektsmelding.")
                vedtaksperiode.trengerInntektsmelding()
            }
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg,
            other: Vedtaksperiode
        ) {
            if (vedtaksperiode.skjæringstidspunkt != other.skjæringstidspunkt) return // vi deler ikke skjæringstidspunkt; revurderingen gjelder en eldre vedtaksperiode
            if (vedtaksperiode.utbetalinger.hørerIkkeSammenMed(utbetaling)) return // vi deler skjæringstidspunkt, men ikke utbetaling (Infotrygdperiode mellom)
            aktivitetslogg.info("Mottatt revurdert utbetaling fra en annen vedtaksperiode")
            vedtaksperiode.mottaUtbetalingTilRevurdering(utbetaling)
        }

        override fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.valider(ytelser, periode, skjæringstidspunkt, arbeidsgiver.organisasjonsnummer())
            ytelser.valider(periode, skjæringstidspunkt)
        }
    }

    internal object AvventerGjennomførtRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GJENNOMFØRT_REVURDERING

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg,
            other: Vedtaksperiode
        ) {
            aktivitetslogg.info("Mottatt revurdert utbetaling fra en annen vedtaksperiode")
            vedtaksperiode.mottaUtbetalingTilRevurdering(utbetaling)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            hendelse.info("Gjenopptar ikke behandling fordi perioden avventer på at revurderingen ferdigstilles.")
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

        override fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.valider(ytelser, periode, skjæringstidspunkt, arbeidsgiver.organisasjonsnummer())
            ytelser.valider(periode, skjæringstidspunkt)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
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
                vedtaksperiode.validerYtelserForSkjæringstidspunkt(ytelser)
                person.valider(ytelser, vilkårsgrunnlag, vedtaksperiode.skjæringstidspunkt, true)
                person.fyllUtPeriodeMedForventedeDager(ytelser, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt)
                val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                vedtaksperiode.forsøkRevurdering(arbeidsgiverUtbetalinger, ytelser)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndterRevurdertUtbetaling(
            vedtaksperiode: Vedtaksperiode,
            utbetaling: Utbetaling,
            aktivitetslogg: IAktivitetslogg,
            other: Vedtaksperiode
        ) {
            aktivitetslogg.info("Mottatt revurdert utbetaling")
            vedtaksperiode.mottaUtbetalingTilRevurdering(utbetaling)
        }

        override fun valider(
            vedtaksperiode: Vedtaksperiode,
            periode: Periode,
            skjæringstidspunkt: LocalDate,
            arbeidsgiver: Arbeidsgiver,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            infotrygdhistorikk.valider(ytelser, periode, skjæringstidspunkt, arbeidsgiver.organisasjonsnummer())
            ytelser.valider(periode, skjæringstidspunkt)
        }
    }

    internal object AvventerVilkårsprøvingRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

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

    internal object AvventerInntektsmeldingEllerHistorikk : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK

        override fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(180)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if(Toggle.Splarbeidsbros.enabled) {
                vedtaksperiode.trengerArbeidsgiveropplysninger()
            }
            vedtaksperiode.trengerInntektsmeldingReplay()
            vedtaksperiode.trengerInntektsmelding()
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
            vedtaksperiode.trengerIkkeInntektsmelding()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) {
                when {
                    !vedtaksperiode.arbeidsgiver.kanBeregneSykepengegrunnlag(vedtaksperiode.skjæringstidspunkt) -> AvsluttetUtenUtbetaling
                    else -> AvventerBlokkerendePeriode
                }
            }
        }

        override fun håndterStrekkingAvPeriode(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            vedtaksperiode.periode = dager.oppdatertFom(vedtaksperiode.periode)
            dager.håndterGjenståendeFør(vedtaksperiode.periode, vedtaksperiode.arbeidsgiver)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            vedtaksperiode.håndterDager(dager)
            if (vedtaksperiode.forventerInntekt()) return true
            vedtaksperiode.tilstand(dager, AvsluttetUtenUtbetaling)
            return true
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding) {
            vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon) { when {
                !vedtaksperiode.arbeidsgiver.kanBeregneSykepengegrunnlag(vedtaksperiode.skjæringstidspunkt) -> AvsluttetUtenUtbetaling
                else -> AvventerBlokkerendePeriode
            }}
        }
        override fun håndtertInntektPåSkjæringstidspunktet(
            vedtaksperiode: Vedtaksperiode,
            hendelse: SykdomstidslinjeHendelse
        ) {
            when {
                !vedtaksperiode.arbeidsgiver.kanBeregneSykepengegrunnlag(vedtaksperiode.skjæringstidspunkt) -> vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
                else -> vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        private fun harNødvendigOpplysningerFraArbeidsgiver(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) =
            vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) &&
                    vedtaksperiode.arbeidsgiver.harNødvendigRefusjonsopplysninger(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode, hendelse)

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}

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
                    if (!vedtaksperiode.forventerInntekt()) {
                        vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
                    } else if (vedtaksperiode.vilkårsgrunnlag is InfotrygdVilkårsgrunnlag) {
                        info("Oppdaget at perioden startet i infotrygd")
                        vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
                    } else if (harNødvendigOpplysningerFraArbeidsgiver(vedtaksperiode, hendelse)) {
                        info("Har nå nødvendige opplysninger fra arbeidsgiver")
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

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            when {
                arbeidsgivere.avventerSøknad(vedtaksperiode.skjæringstidspunkt) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding i samme måned som skjæringstidspunktet"
                )
                arbeidsgivere.avventerSøknad(vedtaksperiode.periode) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én arbeidsgiver venter på søknad for sykmelding i samme måned som skjæringstidspunktet"
                )
                !vedtaksperiode.forventerInntekt() -> vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
                vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() -> {
                    hendelse.funksjonellFeil(RV_SV_2)
                    vedtaksperiode.forkast(hendelse)
                }
                !vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) -> {
                    hendelse.funksjonellFeil(RV_SV_3)
                    vedtaksperiode.forkast(hendelse)
                }
                !arbeidsgivere.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én arbeidsgiver ikke har tilstrekkelig inntekt for skjæringstidspunktet"
                )
                !arbeidsgivere.harNødvendigOpplysningerFraArbeidsgiver(vedtaksperiode.periode) -> return hendelse.info(
                    "Gjenopptar ikke behandling fordi minst én overlappende periode venter på nødvendig opplysninger fra arbeidsgiver"
                )
                !arbeidsgivere.harNødvendigRefusjonsopplysninger(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode, hendelse) -> {
                    hendelse.funksjonellFeil(RV_RE_2)
                    vedtaksperiode.forkast(hendelse)
                }
                else -> {
                    vedtaksperiode.tilstand(hendelse, if (vedtaksperiode.vilkårsgrunnlag == null) AvventerVilkårsprøving else AvventerHistorikk)
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.varsel(RV_IM_4)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
            if (vedtaksperiode.arbeidsgiver.harSykmeldingsperiodeFør(vedtaksperiode.periode.endInclusive.plusDays(1))) {
                sikkerlogg.warn("Har sykmeldingsperiode før eller lik tom. VedtaksperiodeId=${vedtaksperiode.id}, aktørId=${påminnelse.aktørId()}")
            }
        }
        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }
    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING
        override fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(5)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikk)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            inntektsmelding.varsel(RV_IM_4)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
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
                        arbeidsgiver.organisasjonsnummer()
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
                    person.valider(this, vilkårsgrunnlag, vedtaksperiode.skjæringstidspunkt, arbeidsgiver.finnVedtaksperiodeRettFør(vedtaksperiode) != null)
                }
                onSuccess {
                    if (vedtaksperiode.inntektsmeldingInfo == null) {
                        arbeidsgiver.finnTidligereInntektsmeldinginfo(vedtaksperiode.skjæringstidspunkt)?.also { vedtaksperiode.kopierManglende(it) }
                    }
                }
                lateinit var arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
                valider(RV_UT_16) {
                    person.fyllUtPeriodeMedForventedeDager(
                        ytelser,
                        vedtaksperiode.periode,
                        vedtaksperiode.skjæringstidspunkt
                    )
                    arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                    val beregningsperiode = vedtaksperiode.finnArbeidsgiverperiode()?.periode(vedtaksperiode.periode.endInclusive) ?: vedtaksperiode.periode
                    arbeidsgiver.beregn(this, arbeidsgiverUtbetalinger, beregningsperiode, mapOf(vedtaksperiode.periode to (this to vedtaksperiode.jurist())))
                }
                onSuccess {
                    if (vedtaksperiode.person.harKunEttAnnetRelevantArbeidsforholdEnn(
                            vedtaksperiode.skjæringstidspunkt,
                            vedtaksperiode.organisasjonsnummer
                        )
                    ) {
                        ytelser.varsel(RV_VV_8)
                    } else if (vedtaksperiode.skalHaWarningForFlereArbeidsforholdUtenSykdomEllerUlikStartdato(vilkårsgrunnlag)) {
                        ytelser.varsel(RV_VV_2)
                    }
                    vedtaksperiode.forsøkUtbetaling(arbeidsgiverUtbetalinger.maksimumSykepenger, ytelser)
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
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetalinger.valider(simulering).harFunksjonelleFeilEllerVerre())
                return vedtaksperiode.forkast(simulering)
            if (simulering.harNegativtTotalbeløp()) {
                simulering.varsel(Varselkode.RV_SI_3)
            }
            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return
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
            vedtaksperiode.utbetalinger.simuler(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            FunksjonelleFeilTilVarsler.wrap(simulering) {
                if (vedtaksperiode.utbetalinger.valider(simulering).harVarslerEllerVerre()) {
                    simulering.varsel(RV_SI_2)
                }
            }
            if (simulering.harNegativtTotalbeløp()) {
                simulering.varsel(Varselkode.RV_SI_3)
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
            vedtaksperiode.trengerHistorikkFraInfotrygd(påminnelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            if (vedtaksperiode.vilkårsgrunnlag == null) return vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode) {
                hendelse.info("Mangler vilkårsgrunnlag for ${vedtaksperiode.skjæringstidspunkt}")
            }
            if (vedtaksperiode.utbetalinger.erHistorikkEndretSidenBeregning(infotrygdhistorikk)) return vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
            }

            vedtaksperiode.trengerGodkjenning(hendelse)
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

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerHistorikkRevurdering) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
            if (vedtaksperiode.vilkårsgrunnlag == null) return vedtaksperiode.tilstand(påminnelse, AvventerHistorikkRevurdering) {
                påminnelse.info("Reberegner perioden ettersom skjæringstidspunktet har flyttet seg")
            }
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
                AvventerHistorikkRevurdering
            ) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
            }
            vedtaksperiode.trengerGodkjenning(hendelse)
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
            vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                when {
                    vedtaksperiode.utbetalinger.erAvvist() -> RevurderingFeilet.also {
                        utbetalingsgodkjenning.varsel(RV_UT_1)
                    }
                    vedtaksperiode.utbetalinger.harUtbetalinger() -> TilUtbetaling
                    else -> Avsluttet
                }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

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
                vedtaksperiode.utbetalinger.erUbetalt() -> vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode)
                vedtaksperiode.utbetalinger.erUtbetalt() -> vedtaksperiode.tilstand(påminnelse, Avsluttet)
                vedtaksperiode.utbetalinger.harFeilet() -> vedtaksperiode.tilstand(påminnelse, UtbetalingFeilet)
            }
        }
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.overlappendeSøknadIkkeStøttet(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            sjekkUtbetalingstatus(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.utbetalinger.kanIkkeForsøkesPåNy()) {
                return vedtaksperiode.utbetalinger.reberegnUtbetaling(påminnelse, {
                    vedtaksperiode.tilstand(påminnelse, AvventerHistorikkRevurdering)
                }) {
                    vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode) {
                        påminnelse.info("Reberegner periode ettersom utbetaling er avvist og ikke kan forsøkes på nytt")
                    }
                }
            }
            sjekkUtbetalingstatus(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode med utbetaling som har feilet")
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}

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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            check(!vedtaksperiode.utbetalinger.harUtbetaling()) { "Forventet ikke at perioden har fått utbetaling: kun perioder innenfor arbeidsgiverperioden skal sendes hit. " }
            vedtaksperiode.ferdigstillVedtak(hendelse)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!vedtaksperiode.forventerInntekt()) return
            if (!revurdering.inngåSomRevurdering(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            revurdering.loggDersomKorrigerendeSøknad(hendelse, "Startet revurdering grunnet korrigerende søknad")
            hendelse.info(RV_RV_1.varseltekst)
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            søknad.info("Prøver å igangsette revurdering grunnet korrigerende søknad")
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
            if (!søknad.harFunksjonelleFeilEllerVerre() && !vedtaksperiode.forventerInntekt()) {
                vedtaksperiode.emitVedtaksperiodeEndret(søknad) // TODO: for å unngå at flex oppretter oppgaver
            }
        }
        override fun håndterStrekkingAvPeriode(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            vedtaksperiode.periode = dager.oppdatertFom(vedtaksperiode.periode)
            dager.håndterGjenståendeFør(vedtaksperiode.periode, vedtaksperiode.arbeidsgiver)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            if (!revurderingStøttet(vedtaksperiode, dager)) {
                vedtaksperiode.emitVedtaksperiodeEndret(dager)
                return false
            }
            vedtaksperiode.håndterDager(dager)
            if (!vedtaksperiode.forventerInntekt()) {
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
            if (!revurderingStøttet(vedtaksperiode, inntektOgRefusjon)) return
            vedtaksperiode.håndterInntektOgRefusjon(inntektOgRefusjon) { this }
            inntektOgRefusjon.info("Varsler revurdering i tilfelle inntektsmelding påvirker andre perioder.")
            vedtaksperiode.person.igangsettOverstyring(
                inntektOgRefusjon,
                Revurderingseventyr.arbeidsgiverperiode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            if (!revurderingStøttet(vedtaksperiode, inntektsmelding)) {
                return vedtaksperiode.emitVedtaksperiodeEndret(inntektsmelding)
            }

            vedtaksperiode.håndterInntektsmelding(inntektsmelding) {this} // håndterInntektsmelding krever tilstandsendring, men vi må avvente til vi starter revurdering
            if (inntektsmelding.harFunksjonelleFeilEllerVerre()) return

            if (!vedtaksperiode.forventerInntekt()) {
                vedtaksperiode.emitVedtaksperiodeEndret(inntektsmelding) // på stedet hvil!
            }
            inntektsmelding.info("Varsler revurdering i tilfelle inntektsmelding påvirker andre perioder.")
            vedtaksperiode.person.igangsettOverstyring(
                inntektsmelding,
                Revurderingseventyr.arbeidsgiverperiode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            )
        }

        private fun revurderingStøttet(
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg
        ): Boolean {
            val filter =
                if (Toggle.InntektsmeldingKanTriggeRevurdering.enabled) NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING else NYERE_ELLER_SAMME_SKJÆRINGSTIDSPUNKT_ER_UTBETALT
            val revurderingIkkeStøttet = vedtaksperiode.person.vedtaksperioder(filter(vedtaksperiode)).isNotEmpty()

            // støttes ikke før vi støtter revurdering av eldre skjæringstidspunkt
            if (revurderingIkkeStøttet) {
                sikkerlogg.info(
                    "inntektsmelding i auu: Kan ikke reberegne {} for {} {} fordi nyere skjæringstidspunkt blokkerer",
                    keyValue("vedtaksperiodeId", vedtaksperiode.id),
                    keyValue("aktørId", vedtaksperiode.aktørId),
                    keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer)
                )
                hendelse.info("Revurdering blokkeres fordi det finnes nyere skjæringstidspunkt, og det mangler funksjonalitet for å håndtere dette.")
                return false // på stedet hvil!
            }
            return true
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
            if (!vedtaksperiode.forventerInntekt()) return
            if (vedtaksperiode.vilkårsgrunnlag == null) return påminnelse.info("AUU-periode som potensielt burde omgjøres og mangler vilkårsgrunnlag")
            påminnelse.info("AUU-periode som potensielt burde omgjøres og har vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet uten utbetaling")
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

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.skjæringstidspunktFraInfotrygd = vedtaksperiode.person.skjæringstidspunkt(vedtaksperiode.sykdomstidslinje.sykdomsperiode() ?: vedtaksperiode.periode)
            vedtaksperiode.låsOpp()
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!revurdering.inngåSomRevurdering(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.låsOpp()
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.lås()
            vedtaksperiode.person.igangsettOverstyring(
                hendelse,
                Revurderingseventyr.sykdomstidslinje(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
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
            vedtaksperiode.sendOppgaveEvent(hendelse)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            påminnelse.info("Forsøker å gjenoppta behandling i tilfelle perioder er stuck")
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val erFerdigBehandlet = true

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            vedtaksperiode.sendOppgaveEvent(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            throw IllegalStateException("Kan ikke håndtere søknad mens perioden er i TilInfotrygd")
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
            hendelse.info("Overstyrer ikke en vedtaksperiode som er sendt til Infotrygd")
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            throw IllegalStateException("Revurdering håndteres av en periode i til_infotrygd")
        }
    }

    internal companion object {
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
        private const val IKKE_HÅNDTERT: Boolean = false

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
                vedtaksperiode.utbetalinger.erAvsluttet() && vedtaksperiode.skjæringstidspunkt >= skjæringstidspunkt
            }
        }

        internal val NYERE_SKJÆRINGSTIDSPUNKT_UTEN_UTBETALING = { segSelv: Vedtaksperiode ->
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            { vedtaksperiode: Vedtaksperiode ->
                vedtaksperiode.tilstand == AvsluttetUtenUtbetaling && vedtaksperiode.skjæringstidspunkt > skjæringstidspunkt && vedtaksperiode.skjæringstidspunkt > segSelv.periode.endInclusive
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

        internal fun List<Vedtaksperiode>.skalHåndtere(inntektOgRefusjon: InntektOgRefusjonFraInntektsmelding): Vedtaksperiode? {
            inntektOgRefusjon.strategier.forEach { strategy ->
                forEach { vedtaksperiode ->
                    val match = inntektOgRefusjon.skalHåndteresAv(vedtaksperiode.periode, strategy) { vedtaksperiode.forventerInntektOgRefusjonFraInntektsmelding() }
                    if (match) return vedtaksperiode
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

        internal fun List<Vedtaksperiode>.validerYtelser(ytelser: Ytelser, skjæringstidspunkt: LocalDate, infotrygdhistorikk: Infotrygdhistorikk) {
            filter { it.skjæringstidspunkt == skjæringstidspunkt }
                .forEach { it.validerYtelser(ytelser, skjæringstidspunkt, infotrygdhistorikk) }
        }

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

        // Forstår hvordan bygge utbetaling for revurderinger
        internal class RevurderingUtbetalinger(
            vedtaksperioder: List<Vedtaksperiode>,
            skjæringstidspunkt: LocalDate,
            private val orgnummer: String,
            private val hendelse: ArbeidstakerHendelse
        ) {
            private val beregningsperioder = vedtaksperioder.revurderingsperioder(skjæringstidspunkt)
            private val utbetalingsperioder = vedtaksperioder
                .utbetalingsperioder()
                .associateBy { it.arbeidsgiver }

            internal fun utbetal(arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger) {
                arbeidsgiverUtbetalinger.utbetal(hendelse, this, beregningsperioder.periode(), orgnummer, utbetalingsperioder)
            }

            internal fun filtrer(filtere: List<UtbetalingstidslinjerFilter>, tidslinjer: Map<Arbeidsgiver, Utbetalingstidslinje>) {
                if (tidslinjer.all { it.value.isEmpty() }) return
                val perioder = beregningsperioder
                    .map { Triple(it.periode, it.aktivitetsloggkopi(), it.jurist) }
                filtere.forEach {
                    it.filter(tidslinjer.values.toList(), perioder)
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

            /* perioder som kan opprette revurdering er sist på skjæringstidspunktet per arbeidsgiver (hensyntatt pingpong). */
            private fun List<Vedtaksperiode>.utbetalingsperioder(): List<Vedtaksperiode> {
                val første = first { it.tilstand == AvventerHistorikkRevurdering }
                val andre = filterNot { it.arbeidsgiver == første.arbeidsgiver }
                    .filter { it.tilstand == AvventerRevurdering && it.arbeidsgiver.finnVedtaksperiodeRettEtter(it) == null }
                    .filter { it.skjæringstidspunkt == første.skjæringstidspunkt }
                return listOf(første) + andre
            }

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
