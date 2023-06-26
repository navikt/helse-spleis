package no.nav.helse.person

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID
import net.logstash.logback.argument.StructuredArguments.keyValue
import net.logstash.logback.argument.StructuredArguments.kv
import no.nav.helse.Alder
import no.nav.helse.Toggle
import no.nav.helse.etterlevelse.MaskinellJurist
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.etterlevelse.SubsumsjonObserver.Companion.NullObserver
import no.nav.helse.hendelser.AnmodningOmForkasting
import no.nav.helse.hendelser.ArbeidstakerHendelse
import no.nav.helse.hendelser.FunksjonelleFeilTilVarsler
import no.nav.helse.hendelser.Inntektsmelding
import no.nav.helse.hendelser.InntektsmeldingReplayUtført
import no.nav.helse.hendelser.OverstyrArbeidsforhold
import no.nav.helse.hendelser.OverstyrArbeidsgiveropplysninger
import no.nav.helse.hendelser.OverstyrTidslinje
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder
import no.nav.helse.hendelser.Periode.Companion.periode
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
import no.nav.helse.hendelser.Ytelser.Companion.familieYtelserPeriode
import no.nav.helse.hendelser.inntektsmelding.DagerFraInntektsmelding
import no.nav.helse.hendelser.til
import no.nav.helse.hendelser.utbetaling.AnnullerUtbetaling
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
import no.nav.helse.januar
import no.nav.helse.memoized
import no.nav.helse.person.Arbeidsgiver.Companion.avventerSøknad
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigInntektForVilkårsprøving
import no.nav.helse.person.Arbeidsgiver.Companion.trengerInntektsmelding
import no.nav.helse.person.Arbeidsgiver.Companion.vedtaksperioder
import no.nav.helse.person.Dokumentsporing.Companion.ider
import no.nav.helse.person.Dokumentsporing.Companion.sisteInntektsmeldingId
import no.nav.helse.person.Dokumentsporing.Companion.skjønnsmessigFastsettelse
import no.nav.helse.person.Dokumentsporing.Companion.søknadIder
import no.nav.helse.person.Dokumentsporing.Companion.toMap
import no.nav.helse.person.ForlengelseFraInfotrygd.IKKE_ETTERSPURT
import no.nav.helse.person.PersonObserver.ArbeidsgiveropplysningerKorrigertEvent.KorrigerendeInntektektsopplysningstype.SAKSBEHANDLER
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
import no.nav.helse.person.TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING
import no.nav.helse.person.TilstandType.AVVENTER_VILKÅRSPRØVING_REVURDERING
import no.nav.helse.person.TilstandType.REVURDERING_FEILET
import no.nav.helse.person.TilstandType.START
import no.nav.helse.person.TilstandType.TIL_INFOTRYGD
import no.nav.helse.person.TilstandType.TIL_UTBETALING
import no.nav.helse.person.Venteårsak.Companion.fordi
import no.nav.helse.person.Venteårsak.Companion.utenBegrunnelse
import no.nav.helse.person.Venteårsak.Companion.venterPåInntektsmelding
import no.nav.helse.person.Venteårsak.Companion.venterPåSøknad
import no.nav.helse.person.Venteårsak.Hva.BEREGNING
import no.nav.helse.person.Venteårsak.Hva.GODKJENNING
import no.nav.helse.person.Venteårsak.Hva.HJELP
import no.nav.helse.person.Venteårsak.Hva.INNTEKTSMELDING
import no.nav.helse.person.Venteårsak.Hva.SKJØNNSMESSIG_FASTSETTELSE
import no.nav.helse.person.Venteårsak.Hva.SØKNAD
import no.nav.helse.person.Venteårsak.Hva.UTBETALING
import no.nav.helse.person.Venteårsak.Hvorfor.ALLEREDE_UTBETALT
import no.nav.helse.person.Venteårsak.Hvorfor.HAR_SYKMELDING_SOM_OVERLAPPER_PÅ_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_INNTEKT_FOR_VILKÅRSPRØVING_PÅ_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_REFUSJONSOPPLYSNINGER_PÅ_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_ANDRE_ARBEIDSGIVERE
import no.nav.helse.person.Venteårsak.Hvorfor.MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_SAMME_ARBEIDSGIVER
import no.nav.helse.person.Venteårsak.Hvorfor.OVERSTYRING_IGANGSATT
import no.nav.helse.person.Venteårsak.Hvorfor.VIL_AVSLUTTES
import no.nav.helse.person.Venteårsak.Hvorfor.VIL_UTBETALES
import no.nav.helse.person.VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.arbeidsforhold
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSammenligningsgrunnlag
import no.nav.helse.person.aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlag
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
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_AY_10
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IM_4
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_33
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_IT_36
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_OO_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_RV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SV_2
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_28
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_29
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_30
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_31
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_32
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_33
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_34
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_35
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_36
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_37
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_SØ_38
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_1
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_16
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_5
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_VT_1
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje.Companion.slåSammenForkastedeSykdomstidslinjer
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.TagBuilder
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingslinjer.UtbetalingPeriodetype
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.Utbetalingssituasjon.IKKE_UTBETALT
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.Utbetalingssituasjon.INGENTING_Å_UTBETALE
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode.Companion.Utbetalingssituasjon.UTBETALT
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
        person.vedtaksperiodeOpprettet(id, organisasjonsnummer, periode, skjæringstidspunkt, opprettet)
    }

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        val skjæringstidspunktMemoized = this::skjæringstidspunkt.memoized()
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
            inntektsmeldingInfo
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
            inntektsmeldingInfo
        )
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun jurist() = jurist.medVedtaksperiode(id, hendelseIder.toSet().toMap(), sykmeldingsperiode)

    internal fun hendelseIder() = hendelseIder.ider()

    internal fun håndter(sykmelding: Sykmelding) {
        sykmelding.trimLeft(periode.endInclusive)
    }

    internal fun håndter(søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
        if (!søknad.erRelevant(this.periode)) return
        kontekst(søknad)
        val sisteTomFør = arbeidsgiver.finnTidligereVedtaksperioder(periode().start).maxOfOrNull { it.periode().endInclusive }
        søknad.trimEgenmeldingsdager(sisteTomFør, sykmeldingsperiode.start)
        søknadHåndtert(søknad)
        tilstand.håndter(this, søknad, arbeidsgivere)
        søknad.trimLeft(periode.endInclusive)
    }

    private fun inntektsmeldingHåndtert(inntektsmelding: Inntektsmelding): Boolean {
        if (!inntektsmelding.leggTil(hendelseIder)) return true
        person.emitInntektsmeldingHåndtert(inntektsmelding.meldingsreferanseId(), id, organisasjonsnummer)
        return false
    }

    private fun inntektsmeldingHåndtert(dager: DagerFraInntektsmelding): Boolean {
        if (!dager.leggTil(hendelseIder)) return true
        person.emitInntektsmeldingHåndtert(dager.meldingsreferanseId(), id, organisasjonsnummer)
        return false
    }

    private fun søknadHåndtert(søknad: Søknad) {
        søknad.leggTil(hendelseIder)
        person.emitSøknadHåndtert(søknad.meldingsreferanseId(), id, organisasjonsnummer)
    }

    internal fun håndter(anmodningOmForkasting: AnmodningOmForkasting) {
        if (!anmodningOmForkasting.erRelevant(id)) return
        kontekst(anmodningOmForkasting)
        tilstand.håndter(this, anmodningOmForkasting)
    }

    private fun etterkomAnmodningOmForkasting(anmodningOmForkasting: AnmodningOmForkasting) {
        if (!arbeidsgiver.kanForkastes(this)) return anmodningOmForkasting.info("Kan ikke etterkomme anmodning om forkasting")
        anmodningOmForkasting.info("Etterkommer anmodning om forkasting")
        forkast(anmodningOmForkasting)
    }

    internal fun håndter(inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {
        if (!inntektsmeldingReplayUtført.erRelevant(this.id)) return
        kontekst(inntektsmeldingReplayUtført)
        tilstand.håndter(this, inntektsmeldingReplayUtført)
    }

    internal fun håndter(dager: DagerFraInntektsmelding) {
        kontekst(dager)
        val skalHåndtereDager = tilstand.skalHåndtereDager(this, dager)
        if (!skalHåndtereDager || dager.alleredeHåndtert(hendelseIder)) {
            dager.vurdertTilOgMed(periode.endInclusive)

            // om vedtaksperioden ikke skal håndtere dagene kan dette være fordi inntektsmeldingen har oppgitt
            // første fraværsdag slik at arbeidsgiverperioden ikke håndteres. Vi vil likevel kunne trenge varsel,
            // så for å produsere evt. varsel håndteres det av perioden(e) som overlaper med oppgitt agp eller første fraværsdag
            if (!skalHåndtereDager) {
                dager.valider(this.periode, finnArbeidsgiverperiode())
                if (dager.harFunksjonelleFeilEllerVerre()) forkast(dager)
            }
            return
        }
        tilstand.håndter(this, dager)
        dager.vurdertTilOgMed(periode.endInclusive)
    }

    private fun håndterDager(dager: DagerFraInntektsmelding) {
        håndterDagerFør(dager)
        dager.håndter(periode, ::finnArbeidsgiverperiode) { arbeidsgiver.oppdaterSykdom(it) }?.let { oppdatertSykdomstidslinje ->
            sykdomstidslinje = oppdatertSykdomstidslinje
            inntektsmeldingHåndtert(dager)
        }
    }

    private fun håndterDagerFør(dager: DagerFraInntektsmelding) {
        val periodstartFør = periode.start
        dager.leggTilArbeidsdagerFør(periode.start)
        periode = dager.oppdatertFom(periode)
        dager.håndterPeriodeRettFør(periode) { arbeidsgiver.oppdaterSykdom(it) }
        if (periode.start == periodstartFør) return
        dager.info("Perioden ble strukket tilbake fra $periodstartFør til ${periode.start} (${DAYS.between(periode.start, periodstartFør)} dager)")
    }

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
        val arbeidsgiverperiodeFørOverstyring = arbeidsgiver.arbeidsgiverperiode(periode())
        tilstand.håndter(this, hendelse)
        val arbeidsgiverperiodeEtterOverstyring = arbeidsgiver.arbeidsgiverperiode(periode())
        if (arbeidsgiverperiodeFørOverstyring != arbeidsgiverperiodeEtterOverstyring) {
            hendelseIder.sisteInntektsmeldingId()?.let {
                person.arbeidsgiveropplysningerKorrigert(
                    PersonObserver.ArbeidsgiveropplysningerKorrigertEvent(
                    korrigerendeInntektsopplysningId = hendelse.meldingsreferanseId(),
                    korrigerendeInntektektsopplysningstype = SAKSBEHANDLER,
                    korrigertInntektsmeldingId = it
                ))
            }
        }
        hendelse.trimLeft(periode.endInclusive)
    }

    internal fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold, vedtaksperioder: Iterable<Vedtaksperiode>): Boolean {
        if (!overstyrArbeidsforhold.erRelevant(skjæringstidspunkt)) return false
        kontekst(overstyrArbeidsforhold)
        vedtaksperioder.filter(MED_SKJÆRINGSTIDSPUNKT(skjæringstidspunkt)).forEach {
            overstyrArbeidsforhold.leggTil(it.hendelseIder)
        }
        person.vilkårsprøvEtterNyInformasjonFraSaksbehandler(overstyrArbeidsforhold, this.skjæringstidspunkt, jurist())
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
            jurist()
        )
        return true
    }

    private fun påvirkerArbeidsgiverperioden(ny: Vedtaksperiode): Boolean {
        val dagerMellom = ny.periode.periodeMellom(this.periode.start)?.count() ?: return false
        return dagerMellom < MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD
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

    internal fun erForlengelse() = arbeidsgiver
        .finnVedtaksperiodeRettFør(this)
        ?.takeIf { it.forventerInntekt() } != null

    private fun erForlengelseAvForkastet() = arbeidsgiver
        .finnForkastetVedtaksperiodeRettFør(this)
        ?.takeIf { it.forventerInntektHensyntarForkastede() } != null

    private fun manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() =
        person.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag(skjæringstidspunkt)

    private fun harTilstrekkeligInformasjonTilUtbetaling(hendelse: IAktivitetslogg) =
        arbeidsgiver.harTilstrekkeligInformasjonTilUtbetaling(skjæringstidspunkt, periode, hendelse)

    private fun låsOpp() = arbeidsgiver.låsOpp(periode)
    private fun lås() = arbeidsgiver.lås(periode)

    internal fun kanForkastes(arbeidsgiverUtbetalinger: List<Utbetaling>): Boolean {
        if (tilstand == Start) return true // For vedtaksperioder som forkates på "direkten"
        if (!utbetalinger.kanForkastes(arbeidsgiverUtbetalinger)) return false
        val overlappendeUtbetalinger = arbeidsgiverUtbetalinger.filter { it.overlapperMed(periode) }
        return Utbetaling.kanForkastes(overlappendeUtbetalinger, arbeidsgiverUtbetalinger)
    }

    internal fun forkast(hendelse: IAktivitetslogg, utbetalinger: List<Utbetaling>): VedtaksperiodeForkastetEventBuilder? {
        if (!kanForkastes(utbetalinger)) return null
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        this.utbetalinger.forkast(hendelse)
        val trengerArbeidsgiveropplysninger = forventerInntektHensyntarForkastede() && !erForlengelse() && !erForlengelseAvForkastet()
        val sykmeldingsperioder = sykmeldingsperioderKnyttetTilArbeidsgiverperiode()
        val vedtaksperiodeForkastetEventBuilder = VedtaksperiodeForkastetEventBuilder(tilstand.type, trengerArbeidsgiveropplysninger, sykmeldingsperioder)
        tilstand(hendelse, TilInfotrygd)
        return vedtaksperiodeForkastetEventBuilder
    }

    private fun sykmeldingsperioderKnyttetTilArbeidsgiverperiode(): List<Periode> {
        val forkastedeVedtaksperioder = arbeidsgiver.vedtaksperioderKnyttetTilArbeidsgiverperiodeInkludertForkastede(finnArbeidsgiverperiodeHensyntarForkastede())
        return (forkastedeVedtaksperioder.map { it.sykmeldingsperiode } + listOf(sykmeldingsperiode)).distinct()
    }

    internal inner class VedtaksperiodeForkastetEventBuilder(private val gjeldendeTilstand: TilstandType, private val trengerArbeidsgiveropplysninger: Boolean, private val sykmeldingsperioder: List<Periode>) {
        internal fun buildAndEmit() {
            person.vedtaksperiodeForkastet(
                PersonObserver.VedtaksperiodeForkastetEvent(
                    fødselsnummer = fødselsnummer,
                    aktørId = aktørId,
                    organisasjonsnummer = organisasjonsnummer,
                    vedtaksperiodeId = id,
                    gjeldendeTilstand = gjeldendeTilstand,
                    hendelser = hendelseIder(),
                    fom = periode.start,
                    tom = periode.endInclusive,
                    forlengerPeriode = person.nåværendeVedtaksperioder { it.tilstand !== AvsluttetUtenUtbetaling && (it.periode.overlapperMed(periode) || it.periode.erRettFør(periode)) }.isNotEmpty(),
                    harPeriodeInnenfor16Dager = person.nåværendeVedtaksperioder { it.tilstand !== AvsluttetUtenUtbetaling && påvirkerArbeidsgiverperioden(it) }.isNotEmpty(),
                    trengerArbeidsgiveropplysninger = trengerArbeidsgiveropplysninger,
                    sykmeldingsperioder = sykmeldingsperioder
                )
            )
        }
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
            ?.takeIf { nyArbeidsgiverperiodeEtterEndring(it) } ?: this
        person.igangsettOverstyring(hendelse, Revurderingseventyr.sykdomstidslinje(vedtaksperiodeTilRevurdering.skjæringstidspunkt, vedtaksperiodeTilRevurdering.periode))
    }

    private fun nyArbeidsgiverperiodeEtterEndring(other: Vedtaksperiode): Boolean {
        // hvorvidt man delte samme utbetaling før
        if (!this.utbetalinger.harAvsluttede()) return false
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
        emitVedtaksperiodeEndret(previousState)
        tilstand.entering(this, event)
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        periode = hendelse.oppdaterFom(this.periode)
        val rettEtterFørEndring = arbeidsgiver.finnVedtaksperiodeRettEtter(this)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
        lagreTidsnæreopplysninger(hendelse)
        val periodeEtter = rettEtterFørEndring ?: arbeidsgiver.finnVedtaksperiodeRettEtter(this)
        periodeEtter?.lagreTidsnæreopplysninger(hendelse)
    }

    private fun lagreTidsnæreopplysninger(hendelse: IAktivitetslogg) {
        val periodeFør = arbeidsgiver.finnVedtaksperiodeFør(this)?.takeUnless { it.erVedtaksperiodeRettFør(this) }
        val oppholdsperiodeMellom = periodeFør?.sykdomstidslinje?.oppholdsperiodeMellom(this.sykdomstidslinje)

        utbetalinger.lagreTidsnæreInntekter(arbeidsgiver, skjæringstidspunkt, aktivitetsloggkopi(hendelse), oppholdsperiodeMellom)
    }

    private fun håndterSøknad(søknad: Søknad, nesteTilstand: () -> Vedtaksperiodetilstand? = { null }) {
        oppdaterHistorikk(søknad)
        søknad.valider(periode, jurist())
        søknad.validerInntektskilder(vilkårsgrunnlag == null)
        if (manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag()) søknad.funksjonellFeil(RV_SV_2)
        if (søknad.harFunksjonelleFeilEllerVerre()) {
            return forkast(søknad)
        }
        søknad.loggEgenmeldingsstrekking()
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
        else {
            søknad.valider(periode, jurist())
            søknad.validerInntektskilder(vilkårsgrunnlag == null)
            søknad.info("Søknad har trigget en revurdering")
            oppdaterHistorikkBlock(søknad)
        }

        person.igangsettOverstyring(søknad, Revurderingseventyr.korrigertSøknad(skjæringstidspunkt, periode))
    }

    private fun håndterVilkårsgrunnlag(vilkårsgrunnlag: Vilkårsgrunnlag, tilstandUtenAvvik: Vedtaksperiodetilstand, tilstandMedAvvik: Vedtaksperiodetilstand) {
        val sykepengegrunnlag = vilkårsgrunnlag.avklarSykepengegrunnlag(person, jurist())
        vilkårsgrunnlag.valider(sykepengegrunnlag, jurist())
        val grunnlagsdata = vilkårsgrunnlag.grunnlagsdata()
        grunnlagsdata.validerFørstegangsvurdering(vilkårsgrunnlag)
        person.lagreVilkårsgrunnlag(grunnlagsdata)
        vilkårsgrunnlag.info("Vilkårsgrunnlag vurdert")
        if (vilkårsgrunnlag.harFunksjonelleFeilEllerVerre()) return forkast(vilkårsgrunnlag)
        if (sykepengegrunnlag.avventerFastsettelseEtterSkjønn()) return tilstand(vilkårsgrunnlag, tilstandMedAvvik)
        tilstand(vilkårsgrunnlag, tilstandUtenAvvik)
    }

    private fun håndterUtbetalingHendelse(hendelse: UtbetalingHendelse, onUtbetalt: () -> Unit) {
        if (hendelse.harFunksjonelleFeilEllerVerre()) return hendelse.funksjonellFeil(RV_UT_5)
        if (!utbetalinger.erAvsluttet()) return
        onUtbetalt()
    }

    private fun ferdigstillVedtak(hendelse: IAktivitetslogg) {
        sendVedtakFattet()
        person.gjenopptaBehandling(hendelse)
    }

    private fun trengerYtelser(hendelse: IAktivitetslogg, periode: Periode = periode()) {
        val søkevinduFamilieytelser = periode.familieYtelserPeriode
        foreldrepenger(hendelse, søkevinduFamilieytelser)
        pleiepenger(hendelse, søkevinduFamilieytelser)
        omsorgspenger(hendelse, søkevinduFamilieytelser)
        opplæringspenger(hendelse, søkevinduFamilieytelser)
        institusjonsopphold(hendelse, periode)
        arbeidsavklaringspenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
        dagpenger(hendelse, periode.start.minusMonths(2), periode.endInclusive)
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
            arbeidsgiver.vedtaksperioderKnyttetTilArbeidsgiverperiode(arbeidsgiverperiode).map { it.sykmeldingsperiode }

        val forespurteOpplysninger = listOfNotNull(
            forespurtInntekt(fastsattInntekt),
            forespurtFastsattInntekt(fastsattInntekt),
            forespurtRefusjon(fastsattInntekt),
            forespurtArbeidsgiverperiode(arbeidsgiverperiode)
        )

        person.trengerArbeidsgiveropplysninger(
            PersonObserver.TrengerArbeidsgiveropplysningerEvent(
                organisasjonsnummer = organisasjonsnummer,
                vedtaksperiodeId = id,
                skjæringstidspunkt = skjæringstidspunkt,
                sykmeldingsperioder = relevanteSykmeldingsperioder,
                egenmeldingsperioder = sykdomstidslinje.egenmeldingerFraSøknad(),
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

    private fun trengerInntektsmeldingReplay() {
        val sammenhengende = checkNotNull(arbeidsgiver.finnSammenhengendeVedtaksperioder(this)
            .map { it.periode }
            .periode())
        person.inntektsmeldingReplay(id, skjæringstidspunkt, organisasjonsnummer, sammenhengende)
    }

    private fun emitVedtaksperiodeVenter(vedtaksperiodeVenter: VedtaksperiodeVenter) {
        person.vedtaksperiodeVenter(vedtaksperiodeVenter.event(aktørId, fødselsnummer))
    }

    private fun emitVedtaksperiodeEndret(previousState: Vedtaksperiodetilstand) {
        val event = PersonObserver.VedtaksperiodeEndretEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = id,
            gjeldendeTilstand = tilstand.type,
            forrigeTilstand = previousState.type,
            hendelser = hendelseIder(),
            makstid = person.makstid(this, LocalDateTime.now()),
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
        nyUtbetaling(grunnlagsdata, utbetaling, utbetalingstidslinje)
    }

    private fun nyUtbetaling(grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, utbetaling: Utbetaling, utbetalingstidslinjeKilde: Utbetalingstidslinje) {
        utbetalingstidslinje = utbetalinger.nyUtbetaling(id, grunnlagsdata, sykdomstidslinje, periode, utbetaling, utbetalingstidslinjeKilde)
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

        if (Toggle.ForenkleRevurdering.enabled) {
            return nyUtbetaling(grunnlagsdata, utbetaling, utbetalingstidslinje)
        }

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
                it.nyUtbetaling(grunnlagsdata, utbetaling, utbetalingstidslinje)
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
        if (påminnelse.nåddMakstid(vedtaksperiode, person)) return håndterMakstid(vedtaksperiode, påminnelse)
        håndter(vedtaksperiode, påminnelse)
    }

    override fun toString() =
        "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    private fun finnArbeidsgiverperiode() = arbeidsgiver.arbeidsgiverperiode(periode)

    private fun finnArbeidsgiverperiodeHensyntarForkastede() = arbeidsgiver.arbeidsgiverperiodeInkludertForkastet(periode, sykdomstidslinje)

    private fun forventerInntekt(subsumsjonObserver: SubsumsjonObserver = jurist()): Boolean {
        return Arbeidsgiverperiode.forventerInntekt(finnArbeidsgiverperiode(), periode, sykdomstidslinje, subsumsjonObserver)
    }

    internal fun forventerInntektHensyntarForkastede(): Boolean {
        return Arbeidsgiverperiode.forventerInntekt(finnArbeidsgiverperiodeHensyntarForkastede(), periode, sykdomstidslinje, null)
    }

    private fun loggInnenforArbeidsgiverperiode() {
        if (forventerInntekt()) return
        sikkerlogg.info(
            "Vedtaksperioden {} for {} er egentlig innenfor arbeidsgiverperioden ved {}",
            keyValue("vedtaksperiodeId", id), keyValue("fnr", fødselsnummer), keyValue("tilstand", tilstand.type)
        )
    }

    private fun trengerGodkjenning(hendelse: IAktivitetslogg) {
        val vilkårsgrunnlag = requireNotNull(person.vilkårsgrunnlagFor(skjæringstidspunkt))
        val tagBuilder = TagBuilder()
        vilkårsgrunnlag.tags(tagBuilder)
        val erForlengelse = erForlengelse()
        utbetalinger.godkjenning(
            hendelse = hendelse,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = when (erForlengelse) {
                true -> when (vilkårsgrunnlag) {
                    is InfotrygdVilkårsgrunnlag -> UtbetalingPeriodetype.INFOTRYGDFORLENGELSE
                    else -> UtbetalingPeriodetype.FORLENGELSE
                }
                false -> when (vilkårsgrunnlag) {
                    is InfotrygdVilkårsgrunnlag -> UtbetalingPeriodetype.OVERGANG_FRA_IT
                    else -> UtbetalingPeriodetype.FØRSTEGANGSBEHANDLING
                }
            },
            førstegangsbehandling = !erForlengelse,
            tagBuilder = tagBuilder,
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
        nesteTilstandForAktivRevurdering(hendelse)
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
        if (revurdering.ikkeRelevant(skjæringstidspunkt)) return
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

    internal fun håndtertInntektPåSkjæringstidspunktet(skjæringstidspunkt: LocalDate, inntektsmelding: Inntektsmelding) {
        if (skjæringstidspunkt != this.skjæringstidspunkt) return
        kontekst(inntektsmelding)
        tilstand.håndtertInntektPåSkjæringstidspunktet(this, inntektsmelding)
    }

    private fun vedtaksperiodeVenter(venterPå: Vedtaksperiode) {
        val venteårsak = person.venteårsak(venterPå) ?: return
        val builder = VedtaksperiodeVenter.Builder()
        builder.venterPå(venterPå.id, venterPå.organisasjonsnummer, venteårsak)
        builder.venter(
            vedtaksperiodeId = id,
            orgnummer = organisasjonsnummer,
            ventetSiden = oppdatert,
            venterTil = venterTil(venterPå)
        )
        builder.hendelseIder(hendelseIder())
        emitVedtaksperiodeVenter(builder.build())
    }

    private fun venterTil(venterPå: Vedtaksperiode) =
        if (id == venterPå.id) person.makstid(this, oppdatert)
        else minOf(person.makstid(this, oppdatert), person.makstid(venterPå, venterPå.oppdatert))

    internal fun venteårsak(arbeidsgivere: List<Arbeidsgiver>) =
        tilstand.venteårsak(this, arbeidsgivere)

    internal fun makstid(tilstandsendringstidspunkt: LocalDateTime, arbeidsgivere: List<Arbeidsgiver>) =
        tilstand.makstid(tilstandsendringstidspunkt, this, arbeidsgivere)

    fun slutterEtter(dato: LocalDate) = periode.slutterEtter(dato)

    private fun aktivitetsloggkopi(hendelse: IAktivitetslogg) =
        hendelse.barn().also { kopi ->
            this.kontekst(kopi)
        }

    private fun utbetalingsperioder(strategi: Utbetalingstrategi): List<Pair<Vedtaksperiode, Utbetalingstrategi>> {
        val skjæringstidspunktet = this.skjæringstidspunkt
        val revurderinger = if (Toggle.ForenkleRevurdering.enabled) {
            person.nåværendeVedtaksperioder {
                // perioder som er sist på skjæringstidspunktet per arbeidsgiver (hensyntatt pingpong)
                it.arbeidsgiver !== this.arbeidsgiver
                        && it.tilstand == AvventerRevurdering
                        && this.periode.overlapperMed(it.periode)
                        && it.skjæringstidspunkt == skjæringstidspunktet
            }
        } else {
            person.nåværendeVedtaksperioder {
                // perioder som er sist på skjæringstidspunktet per arbeidsgiver (hensyntatt pingpong)
                it.arbeidsgiver !== this.arbeidsgiver
                        && it.tilstand == AvventerRevurdering
                        && it.arbeidsgiver.finnVedtaksperiodeRettEtter(it) == null
                        && it.skjæringstidspunkt == skjæringstidspunktet
            }
        }
        val førstegangsvurderinger = person.nåværendeVedtaksperioder {
                it.arbeidsgiver !== this.arbeidsgiver
                        && it.tilstand == AvventerBlokkerendePeriode
                        && this.periode.overlapperMed(it.periode)
                        && skjæringstidspunktet == it.skjæringstidspunkt
            }
        return listOf(this to strategi) +
                revurderinger.map { it to Vedtaksperiode::lagRevurdering } +
                førstegangsvurderinger.map { it to Vedtaksperiode::lagUtbetaling }
    }

    private fun beregnUtbetalinger(
        hendelse: PersonHendelse,
        arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger,
        beregningsperiode: Periode,
        beregningsperioder: List<Triple<Periode, IAktivitetslogg, SubsumsjonObserver>>,
        segSelvUtbetalingstrategi: Utbetalingstrategi
    ): Boolean {
        val utbetalingsperioder = utbetalingsperioder(segSelvUtbetalingstrategi)
        check(utbetalingsperioder.all { (it) -> it.skjæringstidspunkt == this.skjæringstidspunkt }) {
            "ugyldig situasjon: skal beregne utbetaling for vedtaksperioder med ulike skjæringstidspunkter"
        }
        val grunnlagsdata = checkNotNull(vilkårsgrunnlag) {
            "krever vilkårsgrunnlag for ${skjæringstidspunkt}, men har ikke. Lages det utbetaling for en periode som ikke skal lage utbetaling?"
        }
        try {
            val (maksimumSykepenger, tidslinjerPerArbeidsgiver) = arbeidsgiverUtbetalinger.beregn(skjæringstidspunkt, beregningsperiode, beregningsperioder)
            utbetalingsperioder.forEach { (other, utbetalingStrategy) ->
                val utbetalingstidslinje = tidslinjerPerArbeidsgiver.getValue(other.arbeidsgiver)
                utbetalingStrategy(other, other.aktivitetsloggkopi(hendelse), this, grunnlagsdata, maksimumSykepenger, utbetalingstidslinje)
            }
        } catch (err: UtbetalingstidslinjeBuilderException) {
            err.logg(hendelse)
        }
        return !hendelse.harFunksjonelleFeilEllerVerre()
    }

    internal fun sykefraværsfortelling(list: List<Sykefraværstilfelleeventyr>) =
        list.bliMed(this.id, this.organisasjonsnummer, this.periode)

    internal fun skjønsmessigFastsettelse(skjæringstidspunkt: LocalDate, meldingsreferanseId: UUID) {
        if (this.skjæringstidspunkt != skjæringstidspunkt) return
        hendelseIder.add(skjønnsmessigFastsettelse(meldingsreferanseId))
    }

    // Gang of four State pattern
    internal sealed interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType
        val erFerdigBehandlet: Boolean get() = false

        fun håndterRevurdering(hendelse: PersonHendelse, block: () -> Unit) { FunksjonelleFeilTilVarsler.wrap(hendelse, block) }
        fun håndterFørstegangsbehandling(hendelse: PersonHendelse, vedtaksperiode: Vedtaksperiode, block: () -> Unit) {
            if (vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode)) return block()
            // Om førstegangsbehandling ikke kan forkastes (typisk Out of Order/ omgjøring av AUU) så håndteres det som om det er en revurdering
            håndterRevurdering(hendelse, block)
        }

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ): LocalDateTime = LocalDateTime.MAX

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
        fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>): Venteårsak?

        // venter du på noe?
        fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode)

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>)

        fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {}
        fun skalHåndtereDager(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) =
            dager.skalHåndteresAv(vedtaksperiode.periode)
        fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            if (!dager.påvirker(vedtaksperiode.sykdomstidslinje)) return
            dager.varsel(RV_IM_4, "Inntektsmeldingen ville påvirket sykdomstidslinjen i ${type.name}")
        }

        fun håndtertInntektPåSkjæringstidspunktet(vedtaksperiode: Vedtaksperiode, hendelse: Inntektsmelding) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.info("Forventet ikke vilkårsgrunnlag i %s".format(type.name))
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, anmodningOmForkasting: AnmodningOmForkasting) {
            val kanForkastes = vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode)
            if (kanForkastes) return anmodningOmForkasting.info("Avslår anmodning om forkasting i ${type.name} (kan forkastes)")
            anmodningOmForkasting.info("Avslår anmodning om forkasting i ${type.name} (kan ikke forkastes)")
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
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            utbetalingsgodkjenning.info("Forventet ikke utbetalingsgodkjenning i %s".format(type.name))
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            simulering.info("Forventet ikke simulering i %s".format(type.name))
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            hendelse.info("Forventet ikke utbetaling i %s".format(type.name))
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Forventet ikke overstyring fra saksbehandler i %s".format(type.name))
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
            if (!revurdering.inngåSomRevurdering(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            if (Toggle.ForenkleRevurdering.disabled) {
                vedtaksperiode.varsleForrigePeriodeRevurderingOmgjort(hendelse)
            }
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        fun revurderingOmgjort(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}

    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START
        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            HJELP.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) { }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            val harSenereUtbetalinger = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_MED_UTBETALING(vedtaksperiode)).isNotEmpty()
            val harSenereAUU = vedtaksperiode.person.vedtaksperioder(NYERE_SKJÆRINGSTIDSPUNKT_UTEN_UTBETALING(vedtaksperiode)).isNotEmpty()
            if (harSenereUtbetalinger || harSenereAUU) {
                søknad.varsel(RV_OO_1)
            }
            vedtaksperiode.arbeidsgiver.harForkastetVedtaksperiodeSomBlokkererBehandling(søknad, vedtaksperiode, arbeidsgivere)
            vedtaksperiode.håndterSøknad(søknad) {
                val rettFør = vedtaksperiode.arbeidsgiver.finnVedtaksperiodeRettFør(vedtaksperiode)
                when {
                    rettFør != null && rettFør.tilstand !in setOf(AvsluttetUtenUtbetaling, AvventerInfotrygdHistorikk, AvventerInntektsmelding) -> AvventerBlokkerendePeriode
                    else -> AvventerInfotrygdHistorikk
                }
            }
            søknad.info("Fullført behandling av søknad")
            if (søknad.harFunksjonelleFeilEllerVerre()) return
            vedtaksperiode.person.igangsettOverstyring(søknad, Revurderingseventyr.nyPeriode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
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
        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {}

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = null
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
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
            if (!vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt)) {
                hendelse.info("Revurdering førte til at sykefraværstilfellet trenger inntektsmelding")
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>): Venteårsak? {
            if (!harNødvendigInntektForVilkårsprøving(vedtaksperiode))
                return INNTEKTSMELDING fordi MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_SAMME_ARBEIDSGIVER
            if (!harNødvendigInntektForVilkårsprøving(vedtaksperiode, arbeidsgivere)) {
                loggDersomStuckRevurdering(vedtaksperiode, arbeidsgivere)
                return INNTEKTSMELDING fordi MANGLER_TILSTREKKELIG_INFORMASJON_TIL_UTBETALING_ANDRE_ARBEIDSGIVERE
            }
            if (arbeidsgivere.trengerInntektsmelding(vedtaksperiode.periode))
                return INNTEKTSMELDING fordi MANGLER_REFUSJONSOPPLYSNINGER_PÅ_ANDRE_ARBEIDSGIVERE
            return null
        }

        private fun loggDersomStuckRevurdering(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) {
            val vilkårsgrunnlag = vedtaksperiode.person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt) ?: return
            val arbeidsgivereISykepengegrunnlag = arbeidsgivere.filter { vilkårsgrunnlag.erArbeidsgiverRelevant(it.organisasjonsnummer()) }
            if (harNødvendigInntektForVilkårsprøving(vedtaksperiode, arbeidsgivere = arbeidsgivereISykepengegrunnlag)) {
                sikkerlogg.info("Periode sitter fast i revurdering pga ny arbeidsgiver som ikke er i sykepengegrunnlaget. {}, {}", kv("vedtaksperiodeId", "${vedtaksperiode.id}"), kv("fødselsnummer", vedtaksperiode.fødselsnummer))
            }
        }

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(nestemann)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            if (!harNødvendigInntektForVilkårsprøving(vedtaksperiode))
                return hendelse.info("Mangler nødvendig inntekt for vilkårsprøving og kan derfor ikke gjenoppta revurdering.")
            if (!harNødvendigInntektForVilkårsprøving(vedtaksperiode, arbeidsgivere))
                return hendelse.info("Mangler nødvendig inntekt for vilkårsprøving på annen arbeidsgiver og kan derfor ikke gjenoppta revurdering.")
            if (arbeidsgivere.trengerInntektsmelding(vedtaksperiode.periode))
                return hendelse.info("Trenger inntektsmelding for overlappende periode på annen arbeidsgiver og kan derfor ikke gjenoppta revurdering.")
            if (Toggle.ForenkleRevurdering.enabled) return vedtaksperiode.nesteTilstandForAktivRevurdering(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerGjennomførtRevurdering)
            vedtaksperiode.arbeidsgiver.gjenopptaRevurdering(vedtaksperiode, hendelse)
        }

        private fun harNødvendigInntektForVilkårsprøving(vedtaksperiode: Vedtaksperiode, arbeidsgivere: Iterable<Arbeidsgiver> = listOf(vedtaksperiode.arbeidsgiver)): Boolean {
            val skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
            return arbeidsgivere.harNødvendigInntektForVilkårsprøving(skjæringstidspunkt)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt)) {
                påminnelse.info("Varsler arbeidsgiver at vi har behov for inntektsmelding.")
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }
    }

    private fun nesteTilstandForAktivRevurdering(hendelse: IAktivitetslogg) {
        val vilkårsgrunnlag = vilkårsgrunnlag ?: return tilstand(hendelse, AvventerVilkårsprøvingRevurdering) {
            hendelse.info("Trenger å utføre vilkårsprøving før vi kan beregne utbetaling for revurderingen.")
        }
        if (vilkårsgrunnlag.trengerFastsettelseEtterSkjønn()) return tilstand(hendelse, AvventerSkjønnsmessigFastsettelseRevurdering) {
            hendelse.info("Trenger å skjønnsfastsette sykepengegrunnlaget før vi kan beregne utbetaling for revurderingen")
        }
        tilstand(hendelse, AvventerHistorikkRevurdering)
    }

    internal object AvventerGjennomførtRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GJENNOMFØRT_REVURDERING

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!påminnelse.skalReberegnes()) return
            return vedtaksperiode.person.igangsettOverstyring(påminnelse, Revurderingseventyr.reberegning(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            HJELP.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(nestemann)
        }

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

        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
            val periode = vedtaksperiode.sykefraværstilfelle()
            vedtaksperiode.trengerYtelser(hendelse, periode)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            BEREGNING fordi OVERSTYRING_IGANGSATT

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }
        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes())
                return vedtaksperiode.person.igangsettOverstyring(påminnelse, Revurderingseventyr.reberegning(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
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

            håndterRevurdering(ytelser) {
                if (Toggle.ForenkleRevurdering.enabled) {
                    // når toggle er enabled er det samme kode som førstegangsvurderinger
                    val beregningsperiode = vedtaksperiode.finnArbeidsgiverperiode()?.periode(vedtaksperiode.periode.endInclusive) ?: vedtaksperiode.periode
                    val beregningsperioder = listOf(Triple(vedtaksperiode.periode, ytelser, vedtaksperiode.jurist()))

                    val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                    vedtaksperiode.beregnUtbetalinger(
                        ytelser,
                        arbeidsgiverUtbetalinger,
                        beregningsperiode,
                        beregningsperioder,
                        Vedtaksperiode::lagRevurdering
                    )

                    infotrygdhistorikk.valider(ytelser, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.organisasjonsnummer)
                    ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt, arbeidsgiverUtbetalinger.maksimumSykepenger.sisteDag())
                } else {
                    // beregningsperiode brukes for å avgjøre hvilke perioder vi skal -kreve- inntekt for
                    // beregningsperioder brukes for å lage varsel
                    val utvalg = beregningsperioder(person, vedtaksperiode)

                    person.valider(ytelser, vilkårsgrunnlag, vedtaksperiode.organisasjonsnummer, vedtaksperiode.skjæringstidspunkt)

                    val beregningsperiode = utvalg.periode()
                    val beregningsperioder = utvalg.map { Triple(it.periode, it.aktivitetsloggkopi(ytelser), it.jurist) }

                    val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                    vedtaksperiode.beregnUtbetalinger(
                        ytelser,
                        arbeidsgiverUtbetalinger,
                        beregningsperiode,
                        beregningsperioder,
                        Vedtaksperiode::lagRevurdering
                    )
                    utvalg.forEach {
                        infotrygdhistorikk.valider(it.aktivitetsloggkopi(ytelser), it.periode, it.skjæringstidspunkt, it.organisasjonsnummer)
                        it.kontekst(ytelser) // overskriver kontekst for ytelser-hendelsen
                        ytelser.valider(it.periode, it.skjæringstidspunkt, arbeidsgiverUtbetalinger.maksimumSykepenger.sisteDag())
                        vedtaksperiode.kontekst(ytelser) // endre kontekst tilbake for ytelser-hendelsen
                    }
                }
                vedtaksperiode.høstingsresultater(ytelser, AvventerSimuleringRevurdering, AvventerGodkjenningRevurdering)
            }
        }

        private fun beregningsperioder(person: Person, vedtaksperiode: Vedtaksperiode) =
            (listOf(vedtaksperiode) + person
                .vedtaksperioder {
                    it.tilstand in listOf(AvventerGjennomførtRevurdering, AvventerRevurdering)
                            && it.skjæringstidspunkt == vedtaksperiode.skjæringstidspunkt
                })

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
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

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = null
        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {}
        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            håndterRevurdering(vilkårsgrunnlag) { vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikkRevurdering, AvventerSkjønnsmessigFastsettelseRevurdering) }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }

    }

    internal object AvventerInntektsmelding : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_INNTEKTSMELDING
        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(180)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerInntektsmeldingReplay()
            vedtaksperiode.trengerInntektsmelding()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            INNTEKTSMELDING.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
            vedtaksperiode.trengerIkkeInntektsmelding()
            vedtaksperiode.person.trengerIkkeInntektsmeldingReplay(vedtaksperiode.id)
        }

        override fun skalHåndtereDager(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding): Boolean {
            if (super.skalHåndtereDager(vedtaksperiode, dager)) return true
            val sammenhengende = vedtaksperiode.arbeidsgiver.finnSammenhengendeVedtaksperioder(vedtaksperiode)
                .map { it.periode }
                .periode() ?: return false
            if (!dager.skalHåndteresAv(sammenhengende)) return false
            dager.info("Vedtaksperioden ${vedtaksperiode.periode} håndterer dager fordi den sammenhengende perioden $sammenhengende overlapper med inntektsmelding")
            return true
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            dager.valider(vedtaksperiode.periode)
            if (dager.harFunksjonelleFeilEllerVerre()) {
                vedtaksperiode.forkast(dager)
                return
            }
            vedtaksperiode.håndterDager(dager)
            if(vedtaksperiode.sykdomstidslinje.egenmeldingerFraSøknad().isNotEmpty()) {
                sikkerlogg.warn("Det er egenmeldingsdager fra søknaden på sykdomstidlinjen, selv etter at inntektsmeldingen har oppdatert historikken. Undersøk hvorfor inntektsmeldingen ikke har overskrevet disse. Da er kanskje denne aktørId-en til hjelp: ${vedtaksperiode.aktørId}.")
            }
            if (vedtaksperiode.forventerInntekt()) return
            vedtaksperiode.tilstand(dager, AvsluttetUtenUtbetaling)
        }

        override fun håndtertInntektPåSkjæringstidspunktet(vedtaksperiode: Vedtaksperiode, hendelse: Inntektsmelding) {
            vedtaksperiode.inntektsmeldingHåndtert(hendelse)
            vurderOmKanGåVidere(vedtaksperiode, hendelse)
        }
        
        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            vurderOmKanGåVidere(vedtaksperiode, hendelse)
        }

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
            if (vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag()) {
                påminnelse.info("Mangler nødvendig inntekt ved tidligere beregnet sykepengegrunnlag")
            }
            vurderOmKanGåVidere(vedtaksperiode, påminnelse)
        }

        override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, arbeidsgivere: Iterable<Arbeidsgiver>, hendelse: IAktivitetslogg) {
            vurderOmKanGåVidere(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmeldingReplayUtført: InntektsmeldingReplayUtført) {
            if(vedtaksperiode.forventerInntekt() && !vedtaksperiode.erForlengelse()) {
                vedtaksperiode.trengerArbeidsgiveropplysninger()
            }
            vurderOmKanGåVidere(vedtaksperiode, inntektsmeldingReplayUtført)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, anmodningOmForkasting: AnmodningOmForkasting) {
            vedtaksperiode.etterkomAnmodningOmForkasting(anmodningOmForkasting)
        }

        private fun vurderOmKanGåVidere(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (!vedtaksperiode.forventerInntekt()) return vedtaksperiode.tilstand(hendelse, AvsluttetUtenUtbetaling)
            if (vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag()) {
                hendelse.funksjonellFeil(RV_SV_2)
                return vedtaksperiode.forkast(hendelse)
            }
            if (!vedtaksperiode.harTilstrekkeligInformasjonTilUtbetaling(hendelse)) return
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }
    }

    internal object AvventerBlokkerendePeriode : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_BLOKKERENDE_PERIODE
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ) = tilstand(vedtaksperiode, arbeidsgivere).makstid(tilstandsendringstidspunkt)

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>): Venteårsak? {
            return tilstand(vedtaksperiode, arbeidsgivere).venteårsak()
        }
        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(nestemann)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndtertInntektPåSkjæringstidspunktet(vedtaksperiode: Vedtaksperiode, hendelse: Inntektsmelding) {
            if (vedtaksperiode.inntektsmeldingHåndtert(hendelse)) return
            hendelse.varsel(RV_IM_4)
        }

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) = tilstand(vedtaksperiode, arbeidsgivere).gjenopptaBehandling(vedtaksperiode, hendelse)

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag()) {
                påminnelse.info("Mangler nødvendig inntekt ved tidligere beregnet sykepengegrunnlag")
            }
            vedtaksperiode.person.gjenopptaBehandling(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, anmodningOmForkasting: AnmodningOmForkasting) {
            vedtaksperiode.etterkomAnmodningOmForkasting(anmodningOmForkasting)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {}

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }
        private fun tilstand(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>
        ) = when {
            arbeidsgivere.avventerSøknad(vedtaksperiode.periode) -> AvventerTidligereEllerOverlappendeSøknad
            !vedtaksperiode.forventerInntekt() -> ForventerIkkeInntekt
            vedtaksperiode.manglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag() -> ManglerNødvendigInntektVedTidligereBeregnetSykepengegrunnlag
            !vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) -> ManglerNødvendigInntektForVilkårsprøving
            !arbeidsgivere.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt) -> ManglerNødvendigInntektForVilkårsprøvingForAndreArbeidsgivere
            arbeidsgivere.trengerInntektsmelding(vedtaksperiode.periode) -> TrengerInntektsmelding
            vedtaksperiode.vilkårsgrunnlag == null -> KlarForVilkårsprøving
            vedtaksperiode.vilkårsgrunnlag!!.trengerFastsettelseEtterSkjønn() -> KlarForFastsettelseEtterSkjønn
            else -> KlarForBeregning
        }

        private sealed interface Tilstand {
            fun venteårsak(): Venteårsak? = null
            fun makstid(tilstandsendringstidspunkt: LocalDateTime): LocalDateTime {
                val venteårsak = venteårsak()
                return if (venteårsak.venterPåInntektsmelding) tilstandsendringstidspunkt.plusDays(180)
                else if (venteårsak.venterPåSøknad) tilstandsendringstidspunkt.plusDays(90)
                else LocalDateTime.MAX
            }
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

        private object KlarForVilkårsprøving: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøving)
            }
        }

        private object KlarForFastsettelseEtterSkjønn: Tilstand {
            override fun gjenopptaBehandling(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
                if (Toggle.TjuefemprosentAvvik.disabled && (hendelse is OverstyrArbeidsforhold || hendelse is OverstyrArbeidsgiveropplysninger)) {
                    return vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
                }
                vedtaksperiode.tilstand(hendelse, AvventerSkjønnsmessigFastsettelse)
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
        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(5)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = null

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndtertInntektPåSkjæringstidspunktet(vedtaksperiode: Vedtaksperiode, hendelse: Inntektsmelding) {
            if (vedtaksperiode.inntektsmeldingHåndtert(hendelse)) return
            hendelse.varsel(RV_IM_4)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            håndterFørstegangsbehandling(vilkårsgrunnlag, vedtaksperiode) {
                vedtaksperiode.håndterVilkårsgrunnlag(vilkårsgrunnlag, AvventerHistorikk, AvventerSkjønnsmessigFastsettelse)
            }
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK

        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            checkNotNull(vedtaksperiode.vilkårsgrunnlag) { "Forventer vilkårsgrunnlag for å beregne utbetaling" }
            vedtaksperiode.loggInnenforArbeidsgiverperiode()
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            BEREGNING.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }
        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
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
            håndterFørstegangsbehandling(ytelser, vedtaksperiode) {
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

                    // skal ikke mangle vilkårsgrunnlag her med mindre skjæringstidspunktet har endret seg som følge
                    // av historikk fra IT
                    valider(RV_IT_33) {
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
                    lateinit var arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
                    valider(RV_UT_16) {
                        arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(vedtaksperiode.jurist())
                        val beregningsperiode = vedtaksperiode.finnArbeidsgiverperiode()?.periode(vedtaksperiode.periode.endInclusive) ?: vedtaksperiode.periode
                        val beregningsperioder = listOf(Triple(vedtaksperiode.periode, this, vedtaksperiode.jurist()))

                        vedtaksperiode.beregnUtbetalinger(
                            ytelser,
                            arbeidsgiverUtbetalinger,
                            beregningsperiode,
                            beregningsperioder,
                            Vedtaksperiode::lagUtbetaling
                        )
                    }
                    valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt, arbeidsgiverUtbetalinger.maksimumSykepenger.sisteDag()) }
                    onSuccess {
                        vedtaksperiode.høstingsresultater(ytelser, AvventerSimulering, AvventerGodkjenning)
                    }
                }
            }
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!revurdering.inngåSomEndring(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }
    }

    internal object AvventerSkjønnsmessigFastsettelse : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SKJØNNSMESSIG_FASTSETTELSE
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (Toggle.TjuefemprosentAvvik.enabled) return
            // omgjøringer må enn så lenge gå til godkjenning :/
            if (!vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode)) return vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
            vedtaksperiode.person.kandidatForSkjønnsmessigFastsettelse(vedtaksperiode.vilkårsgrunnlag!!)
            hendelse.funksjonellFeil(Varselkode.RV_IV_2)
            vedtaksperiode.forkast(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = SKJØNNSMESSIG_FASTSETTELSE.utenBegrunnelse
        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) = vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {}

        override fun igangsettOverstyring(
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            revurdering: Revurderingseventyr
        ) {
            if (!revurdering.inngåSomEndring(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, anmodningOmForkasting: AnmodningOmForkasting) {
            vedtaksperiode.etterkomAnmodningOmForkasting(anmodningOmForkasting)
        }
    }

    internal object AvventerSkjønnsmessigFastsettelseRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = TilstandType.AVVENTER_SKJØNNSMESSIG_FASTSETTELSE_REVURDERING
        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            // TODO: fjerne denne når skjønnsmessig fastsettelse funker i revurdering
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = SKJØNNSMESSIG_FASTSETTELSE fordi OVERSTYRING_IGANGSATT
        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) = vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)

        override fun gjenopptaBehandling(
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: Iterable<Arbeidsgiver>,
            hendelse: IAktivitetslogg
        ) {
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {}
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING

        override fun makstid(
            tilstandsendringstidspunkt: LocalDateTime,
            vedtaksperiode: Vedtaksperiode,
            arbeidsgivere: List<Arbeidsgiver>
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            UTBETALING.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }
        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!revurdering.inngåSomEndring(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            if (vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return
            aktivitetslogg.info("Forkaster utbetalingen fordi utbetalingen er ikke simulert, " +
                    "og perioden endrer tilstand")
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerBlokkerendePeriode) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            håndterFørstegangsbehandling(simulering, vedtaksperiode) {
                vedtaksperiode.utbetalinger.valider(simulering)
            }
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

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            UTBETALING fordi OVERSTYRING_IGANGSATT

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
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
            håndterRevurdering(simulering) {
                vedtaksperiode.utbetalinger.valider(simulering)
            }
            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) return simulering.info("Kan ikke gå videre da begge oppdragene ikke er simulert.")
            vedtaksperiode.tilstand(simulering, AvventerGodkjenningRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = GODKJENNING.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
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
            if (!revurdering.inngåSomEndring(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

    }

    internal object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) =
            GODKJENNING fordi OVERSTYRING_IGANGSATT

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(aktivitetslogg)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (påminnelse.skalReberegnes()) return vedtaksperiode.tilstand(påminnelse, AvventerHistorikkRevurdering) {
                påminnelse.info("Reberegner perioden ettersom det er ønsket")
            }
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
                if (utbetalingsgodkjenning.automatiskBehandling()) {
                    utbetalingsgodkjenning.info("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
                    return sikkerlogg.error("Revurderingen ble avvist automatisk - hindrer tilstandsendring for å unngå saker som blir stuck")
                }
                utbetalingsgodkjenning.varsel(RV_UT_1)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterOverlappendeSøknadRevurdering(søknad)
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = UTBETALING.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }
        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
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
                vedtaksperiode.utbetalinger.erAvsluttet() -> vedtaksperiode.tilstand(påminnelse, Avsluttet)
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

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>): Venteårsak {
            if (!vedtaksperiode.forventerInntekt(NullObserver)) return HJELP.utenBegrunnelse
            return when (vedtaksperiode.arbeidsgiver.utbetalingssituasjon(vedtaksperiode.finnArbeidsgiverperiode()!!, listOf(vedtaksperiode.periode))) {
                IKKE_UTBETALT -> HJELP fordi VIL_UTBETALES
                INGENTING_Å_UTBETALE -> HJELP fordi VIL_AVSLUTTES
                UTBETALT -> HJELP fordi ALLEREDE_UTBETALT
            }
        }

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            if (!vedtaksperiode.forventerInntekt(NullObserver)) return
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!vedtaksperiode.forventerInntekt()) return
            if (!revurdering.inngåSomEndring(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            revurdering.loggDersomKorrigerendeSøknad(hendelse, "Startet omgjøring grunnet korrigerende søknad")
            hendelse.info(RV_RV_1.varseltekst)
            if (!vedtaksperiode.arbeidsgiver.harNødvendigInntektForVilkårsprøving(vedtaksperiode.skjæringstidspunkt)) {
                hendelse.info("mangler nødvendige opplysninger fra arbeidsgiver")
                return vedtaksperiode.tilstand(hendelse, AvventerInntektsmelding)
            }
            vedtaksperiode.tilstand(hendelse, AvventerBlokkerendePeriode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            søknad.info("Prøver å igangsette revurdering grunnet korrigerende søknad")
            vedtaksperiode.håndterLåstOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, dager: DagerFraInntektsmelding) {
            dager.valider(vedtaksperiode.periode)
            if (dager.harFunksjonelleFeilEllerVerre()) {
                vedtaksperiode.forkast(dager)
                return
            }

            vedtaksperiode.låsOpp()
            vedtaksperiode.håndterDager(dager)
            vedtaksperiode.lås()

            vedtaksperiode.person.igangsettOverstyring(
                dager,
                Revurderingseventyr.arbeidsgiverperiode(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode)
            )
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: UtbetalingshistorikkEtterInfotrygdendring,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            super.håndter(vedtaksperiode, hendelse, infotrygdhistorikk)
            if (!vedtaksperiode.forventerInntekt()) return
            // for å hindre at eldre AUUer som vi har tenkt å omgjøre forkaster seg før vi har fått sjanse til å ta stilling til dem
            val tilfeldigValgtDato = 1.januar(2023) // (litt tilfeldig valgt dato, men for å stoppe -NYE- ting)
            if (Toggle.STOPPE_TILSIG_AUU.disabled && vedtaksperiode.periode.endInclusive < tilfeldigValgtDato) return
            // stoppe tilsig av vedtaksperioder i AUU som ender opp med å ville utbetale seg selv
            validation(hendelse) {
                onValidationFailed { vedtaksperiode.forkast(hendelse) }
                valider { infotrygdhistorikk.valider(this, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt, vedtaksperiode.organisasjonsnummer) }
                valider(RV_IT_36) { erUpåvirketAvInfotrygdendringer(vedtaksperiode) }
            }
        }

        private fun erUpåvirketAvInfotrygdendringer(vedtaksperiode: Vedtaksperiode): Boolean {
            if (!vedtaksperiode.forventerInntekt()) return true
            val arbeidsgiverperiode = vedtaksperiode.finnArbeidsgiverperiode() ?: return true
            val situasjon = vedtaksperiode.arbeidsgiver.utbetalingssituasjon(arbeidsgiverperiode, listOf(vedtaksperiode.periode))
            return situasjon != IKKE_UTBETALT
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!vedtaksperiode.forventerInntekt(NullObserver)) return
            if (!påminnelse.skalReberegnes()) return
            vedtaksperiode.person.igangsettOverstyring(påminnelse, Revurderingseventyr.reberegning(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderLåstTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, anmodningOmForkasting: AnmodningOmForkasting) {
            vedtaksperiode.person.forkastAuu(anmodningOmForkasting, vedtaksperiode)
            if (vedtaksperiode.tilstand == AvsluttetUtenUtbetaling) return anmodningOmForkasting.info("Kan ikke etterkomme anmodning om forkasting")
            anmodningOmForkasting.info("Etterkommer anmodning om forkasting")
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
            if (Toggle.ForenkleRevurdering.disabled) {
                vedtaksperiode.arbeidsgiver.ferdigstillRevurdering(hendelse, vedtaksperiode)
            }
            vedtaksperiode.kontekst(hendelse) // obs: 'ferdigstillRevurdering' påvirker kontekst på hendelsen
            vedtaksperiode.ferdigstillVedtak(hendelse)
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = HJELP.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {}

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.skjæringstidspunktFraInfotrygd = vedtaksperiode.person.skjæringstidspunkt(vedtaksperiode.sykdomstidslinje.sykdomsperiode() ?: vedtaksperiode.periode)
            vedtaksperiode.låsOpp()
        }

        override fun igangsettOverstyring(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg, revurdering: Revurderingseventyr) {
            if (!revurdering.inngåSomRevurdering(hendelse, vedtaksperiode, vedtaksperiode.periode)) return
            vedtaksperiode.jurist.`fvl § 35 ledd 1`()
            vedtaksperiode.tilstand(hendelse, AvventerRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.revurderLåstTidslinje(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
            vedtaksperiode.håndterLåstOverlappendeSøknadRevurdering(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (!påminnelse.skalReberegnes()) return
            påminnelse.info("Reberegner vedtaksperiode")
            vedtaksperiode.person.igangsettOverstyring(påminnelse, Revurderingseventyr.reberegning(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode))
        }
    }

    internal object RevurderingFeilet : Vedtaksperiodetilstand {
        override val type: TilstandType = REVURDERING_FEILET

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (Toggle.ForenkleRevurdering.disabled) {
                vedtaksperiode.arbeidsgiver.ferdigstillRevurdering(hendelse, vedtaksperiode)
            }
            vedtaksperiode.kontekst(hendelse) // 'ferdigstillRevurdering'  påvirker hendelsekontekst
            vedtaksperiode.person.gjenopptaBehandling(hendelse)
        }
        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>): Venteårsak? {
            if (vedtaksperiode.arbeidsgiver.kanForkastes(vedtaksperiode.utbetalinger)) return null
            return HJELP.utenBegrunnelse
        }

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {
            vedtaksperiode.vedtaksperiodeVenter(vedtaksperiode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
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
            hendelse.info("Vedtaksperioden kan ikke behandles i Spleis.")
        }

        override fun venteårsak(vedtaksperiode: Vedtaksperiode, arbeidsgivere: List<Arbeidsgiver>) = HJELP.utenBegrunnelse

        override fun venter(vedtaksperiode: Vedtaksperiode, nestemann: Vedtaksperiode) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad, arbeidsgivere: List<Arbeidsgiver>) {
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
        // dersom "ny" slutter på en fredag, så starter ikke oppholdstelling før påfølgende mandag.
        // det kan derfor være mer enn 16 dager avstand mellom periodene, og arbeidsgiverperioden kan være den samme
        // Derfor bruker vi tallet 18 fremfor kanskje det forventende 16…
        internal const val MINIMALT_TILLATT_AVSTAND_TIL_INFOTRYGD = 18L
        private val sikkerlogg = LoggerFactory.getLogger("tjenestekall")

        // Fredet funksjonsnavn
        internal val TIDLIGERE_OG_ETTERGØLGENDE = fun(segSelv: Vedtaksperiode): VedtaksperiodeFilter {
            // forkaster perioder som er før/overlapper med oppgitt periode, eller som er sammenhengende med
            // perioden som overlapper (per skjæringstidpunkt!).
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            val arbeidsgiverperiode = segSelv.finnArbeidsgiverperiode()
            return fun (other: Vedtaksperiode): Boolean {
                if (other.periode.start >= segSelv.periode.start) return true // Forkaster nyere perioder på tvers av arbeidsgivere
                if (arbeidsgiverperiode != null && other.arbeidsgiver === segSelv.arbeidsgiver && other.periode in arbeidsgiverperiode) return true // Forkaster samme arbeidsgiverperiode (kun for samme arbeidsgiver)
                return other.skjæringstidspunkt == skjæringstidspunkt // Forkaster alt med samme skjæringstidspunkt på tvers av arbeidsgivere
            }
        }

        internal val IKKE_FERDIG_REVURDERT: VedtaksperiodeFilter = { it.tilstand == AvventerGjennomførtRevurdering }
        internal val PÅGÅENDE_REVURDERING: VedtaksperiodeFilter = {
            it.tilstand in setOf(
                AvventerVilkårsprøvingRevurdering,
                AvventerHistorikkRevurdering,
                AvventerSimuleringRevurdering,
                AvventerGodkjenningRevurdering,
                AvventerSkjønnsmessigFastsettelseRevurdering
            )
        }

        internal val HAR_PÅGÅENDE_UTBETALINGER: VedtaksperiodeFilter = { it.utbetalinger.utbetales() }

        internal val KLAR_TIL_BEHANDLING: VedtaksperiodeFilter = {
            it.tilstand == AvventerBlokkerendePeriode || it.tilstand == AvventerGodkjenning
        }

        internal val IKKE_FERDIG_BEHANDLET: VedtaksperiodeFilter = { !it.tilstand.erFerdigBehandlet }

        internal val MED_SKJÆRINGSTIDSPUNKT = { skjæringstidspunkt: LocalDate ->
            { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.skjæringstidspunkt == skjæringstidspunkt }
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

        internal val NYERE_SKJÆRINGSTIDSPUNKT_UTEN_UTBETALING = { segSelv: Vedtaksperiode ->
            val skjæringstidspunkt = segSelv.skjæringstidspunkt
            { vedtaksperiode: Vedtaksperiode ->
                vedtaksperiode.tilstand == AvsluttetUtenUtbetaling && vedtaksperiode.skjæringstidspunkt > skjæringstidspunkt && vedtaksperiode.skjæringstidspunkt > segSelv.periode.endInclusive
            }
        }

        internal val AUU_SOM_VIL_UTBETALES: VedtaksperiodeFilter = {
            it.tilstand == AvsluttetUtenUtbetaling && it.forventerInntekt(NullObserver)
        }

        internal val AUU_UTBETALT_I_INFOTRYGD = { infotrygdhistorikk: Infotrygdhistorikk ->
            { vedtaksperiode: Vedtaksperiode -> vedtaksperiode.tilstand == AvsluttetUtenUtbetaling && infotrygdhistorikk.harUtbetaltI(vedtaksperiode.periode) }
        }

        internal fun Iterable<Vedtaksperiode>.nåværendeVedtaksperiode(filter: VedtaksperiodeFilter) =
            firstOrNull(filter)

        internal fun List<Vedtaksperiode>.trengerInntektsmelding() = filter {
            it.tilstand == AvventerInntektsmelding
        }.filter {
            it.forventerInntekt()
        }

        internal abstract class AuuGruppering protected constructor(
            protected val organisasjonsnummer: String,
            protected val auuer: List<Vedtaksperiode>
        ) {
            init {
                check(auuer.isNotEmpty()) { "Må inneholde minst en vedtaksperiode" }
                check(auuer.all { it.tilstand == AvsluttetUtenUtbetaling }) { "Alle vedtaksperioder må være AvsluttetUtenUtbetaling" }
                check(auuer.all { it.organisasjonsnummer == organisasjonsnummer }) { "Alle vedtaksperioder må høre til samme arbeidsgiver" }
            }

            protected val førsteAuu = auuer.min()
            protected val sisteAuu = auuer.max()
            protected val perioder = auuer.map { it.periode }
            protected val arbeidsgiver = sisteAuu.arbeidsgiver
            private val person = sisteAuu.person

            abstract fun påvirkerForkastingArbeidsgiverperioden(alleVedtaksperioder: List<Vedtaksperiode>): Boolean

            abstract fun identifiserAUUSomErUtbetaltISpleis()

            abstract fun identifiserForkastingScenarioer(hendelse: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk, alleVedtaksperioder: List<Vedtaksperiode>)

            internal fun forkast(hendelse: IAktivitetslogg, alleVedtaksperioder: List<Vedtaksperiode>, årsak: String = "${hendelse::class.simpleName}") {
                hendelse.info("Forkaste AUU: Vurderer om periodene $perioder kan forkastes på grunn av $årsak")
                if (!kanForkastes(hendelse, alleVedtaksperioder)) return
                hendelse.info("Forkaste AUU: Vedtaksperiodene $perioder forkastes på grunn av $årsak")
                val forkastes = auuer.map { it.id }
                person.søppelbøtte(hendelse) { it.id in forkastes }
            }

            protected fun kanForkastes(hendelse: IAktivitetslogg?, alleVedtaksperioder: List<Vedtaksperiode>): Boolean {
                if (auuer.any { !it.arbeidsgiver.kanForkastes(it) }) return false.also { hendelse?.info("Forkaste AUU: Kan ikke forkastes, har overlappende utbetalte utbetalinger på samme arbeidsgiver") }
                if (påvirkerForkastingArbeidsgiverperioden(alleVedtaksperioder)) return false.also { hendelse?.info("Forkaste AUU: Kan ikke forkastes, påvirker arbeidsgiverperiode på samme arbeidsgiver") }
                if (påvirkerForkastingSkjæringstidspunktPåPerson(hendelse, alleVedtaksperioder)) return false.also { hendelse?.info("Forkaste AUU: Kan ikke forkastes, påvirker skjæringstidspunkt på personen") }
                return true
            }

            private fun påvirkerForkastingSkjæringstidspunktPåPerson(hendelse: IAktivitetslogg?, alleVedtaksperioder: List<Vedtaksperiode>): Boolean {
                val auuenesVedtaksperiodeId = auuer.map { it.id }
                val vedtaksperioderSomMåBeholdeSkjæringstidspunkt = alleVedtaksperioder.filterNot { it.id in auuenesVedtaksperiodeId }.filterNot { it.tilstand == TilInfotrygd }
                val arbeidsgiversSykdomstidslinjeUtenAuuene = arbeidsgiver.sykdomstidslinjeUten(auuer.map { it.sykdomstidslinje })

                vedtaksperioderSomMåBeholdeSkjæringstidspunkt.forEach { vedtaksperiode ->
                    if (vedtaksperiode.skjæringstidspunkt != person.skjæringstidspunkt(arbeidsgiver, arbeidsgiversSykdomstidslinjeUtenAuuene, vedtaksperiode.periode)) {
                        hendelse?.info("Forkaste AUU: Kan ikke forkaste, vedtaksperioden ${vedtaksperiode.periode} på ${vedtaksperiode.organisasjonsnummer} ville fått endret skjæringstidspunkt")
                        return true
                    }
                }
                return false
            }

            internal companion object {
                internal fun List<Vedtaksperiode>.gruppérAuuer(filter: (vedtaksperiode: Vedtaksperiode) -> Boolean) =
                    this
                        .groupBy { it.organisasjonsnummer }
                        .flatMap { (organisasjonsnummer, vedtaksperioder) ->
                            vedtaksperioder
                                .filter { it.tilstand == AvsluttetUtenUtbetaling }
                                .groupBy { it.finnArbeidsgiverperiode() }
                                .flatMap { (arbeidsgiverperiode, vedtaksperioder) ->
                                    if (arbeidsgiverperiode == null) vedtaksperioder.map { AuuUtenAGP(organisasjonsnummer, it) }
                                    else listOf(AuuerMedSammeAGP(organisasjonsnummer, vedtaksperioder, arbeidsgiverperiode))
                                }

                        }
                        .filter { auuer -> auuer.auuer.any(filter) }
                        .sortedByDescending { it.sisteAuu }
                internal fun List<Vedtaksperiode>.auuGruppering(vedtaksperiode: Vedtaksperiode): AuuGruppering? {
                    if (vedtaksperiode.tilstand != AvsluttetUtenUtbetaling) return null
                    val arbeidsgiverperiode = vedtaksperiode.finnArbeidsgiverperiode() ?: return AuuUtenAGP(vedtaksperiode.organisasjonsnummer, vedtaksperiode)
                    return this
                        .filter { it.organisasjonsnummer == vedtaksperiode.organisasjonsnummer }
                        .filter { it.tilstand == AvsluttetUtenUtbetaling }
                        .filter { it.finnArbeidsgiverperiode() == arbeidsgiverperiode }
                        .let { AuuerMedSammeAGP(vedtaksperiode.organisasjonsnummer, it, arbeidsgiverperiode) }
                }
            }
        }

        internal class AuuerMedSammeAGP(
            organisasjonsnummer: String,
            auuer: List<Vedtaksperiode>,
            private val arbeidsgiverperiode: Arbeidsgiverperiode
        ): AuuGruppering(organisasjonsnummer, auuer) {
            init {
                check(auuer.all { it.finnArbeidsgiverperiode() == arbeidsgiverperiode }) { "Alle vedtaksperidoer må ha samme arbeidsgiverperioder" }
            }
            override fun påvirkerForkastingArbeidsgiverperioden(alleVedtaksperioder: List<Vedtaksperiode>) = alleVedtaksperioder
                .filter { it.organisasjonsnummer == organisasjonsnummer }
                .filter { it.tilstand != AvsluttetUtenUtbetaling }
                .filter { it.periode.starterEtter(sisteAuu.periode) }
                .any { it.finnArbeidsgiverperiode() == arbeidsgiverperiode }

            override fun identifiserAUUSomErUtbetaltISpleis() {
                when (arbeidsgiver.utbetalingssituasjon(arbeidsgiverperiode, perioder)) {
                    IKKE_UTBETALT -> sikkerLogg("det er utbetalingsdager som ikke er utbetalt")
                    INGENTING_Å_UTBETALE -> sikkerLogg("det er ingen utbetalingsdager")
                    UTBETALT -> sikkerLogg("alle utbetalingsdager er allerede utbetalt")
                }
            }

            override fun identifiserForkastingScenarioer(hendelse: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk, alleVedtaksperioder: List<Vedtaksperiode>) {
                val auuGrupperingsperiode = førsteAuu.periode.start til sisteAuu.periode.endInclusive
                val søkefelt = auuGrupperingsperiode.start.minusDays(16) til auuGrupperingsperiode.endInclusive.plusDays(16)

                if (!infotrygdhistorikk.harUtbetaltI(søkefelt)) return
                if (auuer.any { infotrygdhistorikk.harUtbetaltI(it.periode) }) return

                hendelse.info("Utbetalt i Infotrygd innenfor samme arbeidsgiverperiode.")

                val snute = arbeidsgiverperiode.fiktiv() || (auuGrupperingsperiode.start.minusDays(16) til auuGrupperingsperiode.start.minusDays(1)).let { infotrygdhistorikk.harUtbetaltI(it) }
                val hale = (auuGrupperingsperiode.endInclusive.plusDays(1) til auuGrupperingsperiode.endInclusive.plusDays(16)).let { infotrygdhistorikk.harUtbetaltI(it) }
                val gapdager = !snute && !hale

                val kanForkastes = kanForkastes(null, alleVedtaksperioder)
                val utbetalingssituasjon = arbeidsgiver.utbetalingssituasjon(arbeidsgiverperiode, perioder)
                val skalForkastes = kanForkastes && !snute && hale && utbetalingssituasjon != UTBETALT
                if (skalForkastes) {
                    return forkast(hendelse, alleVedtaksperioder, "Utbetalt i Infotrygd i etterkant innenfor samme agp")
                }

                sikkerlogg.info("Kandidat for å forkastes på grunn av utbetaling i Infotrygd innenfor samme arbeidsgiverperiode for {} på $organisasjonsnummer i $perioder med arbeidsgiverperiode = ${arbeidsgiverperiode.grupperSammenhengendePerioder()}, {}, {}, {}, {}, {}",
                    keyValue("fødselsnummer", førsteAuu.fødselsnummer),
                    keyValue("snute", snute),
                    keyValue("gapdager", gapdager),
                    keyValue("hale", hale),
                    keyValue("utbetalingssituasjon", utbetalingssituasjon),
                    keyValue("kanForkastes", kanForkastes)
                )
            }

            private fun sikkerLogg(melding: String) {
                val vedtaksperiode = auuer.firstOrNull { it.forventerInntekt(NullObserver) } ?: førsteAuu
                val fiktiv = if (arbeidsgiverperiode.fiktiv()) " (fiktiv)" else ""
                sikkerlogg.info("AuuerMedSammeAGP som vil utbetales: $melding. Perioder=$perioder, arbeidsgiverperiode=${arbeidsgiverperiode.grupperSammenhengendePerioder()}${fiktiv}, {}, {}, {}",
                    keyValue("fødselsnummer", vedtaksperiode.fødselsnummer),
                    keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer),
                    keyValue("vedtaksperiodeId", "${vedtaksperiode.id}"),
                )
            }
        }

        internal class AuuUtenAGP(
            organisasjonsnummer: String,
            auu: Vedtaksperiode
        ): AuuGruppering(organisasjonsnummer, listOf(auu)) {
            init {
                check(auu.finnArbeidsgiverperiode() == null) { "Vedtaksperiodens arbeidsgiverperiode må være null" }
            }
            override fun påvirkerForkastingArbeidsgiverperioden(alleVedtaksperioder: List<Vedtaksperiode>) = false
            override fun identifiserAUUSomErUtbetaltISpleis() {}
            override fun identifiserForkastingScenarioer(
                hendelse: IAktivitetslogg,
                infotrygdhistorikk: Infotrygdhistorikk,
                alleVedtaksperioder: List<Vedtaksperiode>
            ) {}
        }

        internal fun List<Vedtaksperiode>.venter(nestemann: Vedtaksperiode) {
            forEach { vedtaksperiode ->
                vedtaksperiode.tilstand.venter(vedtaksperiode, nestemann)
            }
        }

        internal fun harNyereForkastetPeriode(forkastede: Iterable<Vedtaksperiode>, vedtaksperiode: Vedtaksperiode, hendelse: SykdomstidslinjeHendelse) =
            forkastede
                .filter { it.periode.start > vedtaksperiode.periode.endInclusive }
                .onEach {
                    val sammeArbeidsgiver = it.organisasjonsnummer == vedtaksperiode.organisasjonsnummer
                    hendelse.funksjonellFeil(if (sammeArbeidsgiver) RV_SØ_31 else RV_SØ_32)
                    hendelse.info("Søknaden ${vedtaksperiode.periode} er før en forkastet vedtaksperiode ${it.id} (${it.periode})")
                }
                .isNotEmpty()

        internal fun harOverlappendeForkastetPeriode(forkastede: Iterable<Vedtaksperiode>, vedtaksperiode: Vedtaksperiode, hendelse: SykdomstidslinjeHendelse) =
            forkastede
                .filter { it.periode.overlapperMed(vedtaksperiode.periode()) }
                .onEach {
                    val delvisOverlappende = !it.periode.inneholder(vedtaksperiode.periode) // hvorvidt vedtaksperioden strekker seg utenfor den forkastede
                    val sammeArbeidsgiver = it.organisasjonsnummer == vedtaksperiode.organisasjonsnummer
                    hendelse.funksjonellFeil(when {
                        delvisOverlappende && sammeArbeidsgiver -> RV_SØ_35
                        delvisOverlappende && !sammeArbeidsgiver -> RV_SØ_36
                        !delvisOverlappende && sammeArbeidsgiver -> RV_SØ_33
                        !delvisOverlappende && !sammeArbeidsgiver -> RV_SØ_34
                        else -> throw IllegalStateException("dette er ikke mulig med mindre noen har tullet til noe")
                    })
                    hendelse.info("Søknad ${vedtaksperiode.periode} overlapper med en forkastet vedtaksperiode ${it.id} (${it.periode})")
                }
                .isNotEmpty()

        internal fun harKortGapTilForkastet(forkastede: Iterable<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse, vedtaksperiode: Vedtaksperiode) =
            forkastede
                .filter { other -> vedtaksperiode.påvirkerArbeidsgiverperioden(other) }
                .onEach {
                    hendelse.funksjonellFeil(RV_SØ_28)
                    hendelse.info("Søknad har et gap som er kortere enn 20 dager til en forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                }
                .isNotEmpty()

        internal fun forlengerForkastet(forkastede: List<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse, vedtaksperiode: Vedtaksperiode) =
            forkastede
                .filter { it.periode.erRettFør(vedtaksperiode.periode) }
                .onEach {
                    val sammeArbeidsgiver = it.organisasjonsnummer == vedtaksperiode.organisasjonsnummer
                    hendelse.funksjonellFeil(if (sammeArbeidsgiver) RV_SØ_37 else RV_SØ_38)
                    hendelse.info("Søknad forlenger forkastet vedtaksperiode ${it.id}, hendelse periode: ${hendelse.periode()}, vedtaksperiode periode: ${it.periode}")
                }
                .isNotEmpty()

        internal fun List<Vedtaksperiode>.slåSammenForkastedeSykdomstidslinjer(): Sykdomstidslinje =
            map { it.sykdomstidslinje }.slåSammenForkastedeSykdomstidslinjer()

        internal fun List<Vedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            filter { it.utbetalinger.harId(utbetalingId) }.map { it.id }

        internal fun List<Vedtaksperiode>.inneholder(id: UUID) = any { id == it.id }

        internal fun List<Vedtaksperiode>.periode(): Periode {
            val fom = minOf { it.periode.start }
            val tom = maxOf { it.periode.endInclusive }
            return Periode(fom, tom)
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
            sikkerlogg.warn("Ugyldig utbetalingstidslinje: utbetalingsdager med kilde Sykmelding: ${dager.grupperSammenhengendePerioder()} {}, {}, {}",
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

private typealias Utbetalingstrategi = (Vedtaksperiode, IAktivitetslogg, Vedtaksperiode, VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement, Alder.MaksimumSykepenger, Utbetalingstidslinje) -> Unit

enum class ForlengelseFraInfotrygd {
    IKKE_ETTERSPURT,
    JA,
    NEI
}

internal typealias VedtaksperiodeFilter = (Vedtaksperiode) -> Boolean
