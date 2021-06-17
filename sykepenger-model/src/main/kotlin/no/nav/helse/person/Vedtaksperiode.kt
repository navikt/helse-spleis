package no.nav.helse.person

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dødsinformasjon
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntekterForSykepengegrunnlag
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.Arbeidsgiver.Companion.SENERE_INCLUSIVE
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.ForkastetÅrsak.ERSTATTES
import no.nav.helse.person.ForkastetÅrsak.IKKE_STØTTET
import no.nav.helse.person.ForlengelseFraInfotrygd.*
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.MaksimumSykepengedagerfilter
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Vedtaksperiode private constructor(
    private val person: Person,
    private val arbeidsgiver: Arbeidsgiver,
    private val id: UUID,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val organisasjonsnummer: String,
    private var tilstand: Vedtaksperiodetilstand,
    private var skjæringstidspunktFraInfotrygd: LocalDate?,
    private var dataForSimulering: Simulering.SimuleringResultat?,
    private var sykdomstidslinje: Sykdomstidslinje,
    private val hendelseIder: MutableList<UUID>,
    private var inntektsmeldingInfo: InntektsmeldingInfo?,
    private var periode: Periode,
    private val sykmeldingsperiode: Periode,
    private val utbetalinger: MutableList<Utbetaling>,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
    private var forlengelseFraInfotrygd: ForlengelseFraInfotrygd = IKKE_ETTERSPURT,
    private var inntektskilde: Inntektskilde,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val regler = NormalArbeidstaker
    private val skjæringstidspunkt get() = skjæringstidspunktFraInfotrygd ?: person.skjæringstidspunkt(periode)
    private val utbetaling get() = utbetalinger.lastOrNull()

    internal constructor(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        hendelse: SykdomstidslinjeHendelse,
        inntektsmeldingInfo: InntektsmeldingInfo? = null
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = UUID.randomUUID(),
        aktørId = hendelse.aktørId(),
        fødselsnummer = hendelse.fødselsnummer(),
        organisasjonsnummer = hendelse.organisasjonsnummer(),
        tilstand = Start,
        skjæringstidspunktFraInfotrygd = null,
        dataForSimulering = null,
        sykdomstidslinje = hendelse.sykdomstidslinje(),
        hendelseIder = mutableListOf(),
        inntektsmeldingInfo = inntektsmeldingInfo,
        periode = hendelse.periode(),
        sykmeldingsperiode = hendelse.periode(),
        utbetalinger = mutableListOf(),
        utbetalingstidslinje = Utbetalingstidslinje(),
        inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        opprettet = LocalDateTime.now()
    )

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(
            this,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            sykmeldingsperiode,
            skjæringstidspunkt,
            periodetype(),
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskilde
        )
        sykdomstidslinje.accept(visitor)
        utbetalingstidslinje.accept(visitor)
        visitor.preVisitVedtakserperiodeUtbetalinger(utbetalinger)
        utbetalinger.forEach { it.accept(visitor) }
        visitor.postVisitVedtakserperiodeUtbetalinger(utbetalinger)
        visitor.visitDataForSimulering(dataForSimulering)
        visitor.postVisitVedtaksperiode(
            this,
            id,
            tilstand,
            opprettet,
            oppdatert,
            periode,
            sykmeldingsperiode,
            skjæringstidspunkt,
            periodetype(),
            forlengelseFraInfotrygd,
            hendelseIder,
            inntektsmeldingInfo,
            inntektskilde
        )
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Vedtaksperiode", mapOf("vedtaksperiodeId" to id.toString()))
    }

    internal fun håndter(sykmelding: Sykmelding) = overlapperMed(sykmelding).also {
        if (!it) return it
        kontekst(sykmelding)
        tilstand.håndter(this, sykmelding)
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        tilstand.håndter(this, søknad)
    }

    internal fun håndter(søknad: Søknad) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        tilstand.håndter(this, søknad)
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, other: UUID? = null): Boolean {
        val overlapper = overlapperMed(inntektsmelding) && (other == null || other == id)
        return overlapper.also {
            if (arbeidsgiver.harRefusjonOpphørt(periode.endInclusive) && !erAvsluttet()) {
                kontekst(inntektsmelding)
                inntektsmelding.error("Refusjon opphører i perioden")
                inntektsmelding.trimLeft(periode.endInclusive)
                arbeidsgiver.søppelbøtte(inntektsmelding, SENERE_INCLUSIVE(this), IKKE_STØTTET)
                return@also
            }
            if (!it) return@also inntektsmelding.trimLeft(periode.endInclusive)
            kontekst(inntektsmelding)
            tilstand.håndter(this, inntektsmelding)
        }
    }

    internal fun håndterHistorikkFraInfotrygd(hendelse: IAktivitetslogg, infotrygdhistorikk: Infotrygdhistorikk) {
        tilstand.håndter(person, arbeidsgiver, this, hendelse, infotrygdhistorikk)
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk, infotrygdhistorikk: Infotrygdhistorikk) {
        if (!utbetalingshistorikk.erRelevant(id)) return
        kontekst(utbetalingshistorikk)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingshistorikk, infotrygdhistorikk)
    }

    internal fun håndter(ytelser: Ytelser, infotrygdhistorikk: Infotrygdhistorikk, arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger) {
        if (!ytelser.erRelevant(id)) return
        kontekst(ytelser)
        tilstand.håndter(person, arbeidsgiver, this, ytelser, infotrygdhistorikk, arbeidsgiverUtbetalinger)
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        if (!utbetalingsgodkjenning.erRelevant(id.toString())) return
        if (!utbetaling().gjelderFor(utbetalingsgodkjenning)) return utbetalingsgodkjenning.info("Ignorerer løsning på godkjenningsbehov, utbetalingid på løsningen matcher ikke vedtaksperiodens nåværende utbetaling")
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
        if (utbetaling?.gjelderFor(hendelse) != true) return
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (!påminnelse.erRelevant(id)) return false
        kontekst(påminnelse)
        tilstand.påminnelse(this, påminnelse)
        return true
    }

    internal fun håndter(hendelse: GjenopptaBehandling): Boolean {
        if (tilstand.erFerdigbehandlet) return false
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
        return true
    }

    internal fun håndter(hendelse: OverstyrTidslinje) = hendelse.erRelevant(periode).also {
        if (!it) return it
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
    }

    internal fun nyPeriode(ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
        if (ny > this || ny == this) return
        kontekst(hendelse)
        tilstand.nyPeriodeFør(this, ny, hendelse)
        if (hendelse.hasErrorsOrWorse()) return
        if (ny.erSykeperiodeRettFør(this)) return tilstand.uferdigPeriodeRettFør(this, ny, hendelse)
        tilstand.uferdigPeriodeFør(this, ny, hendelse)
    }

    internal fun nyRevurderingFør(revurdert: Vedtaksperiode, hendelse: IAktivitetslogg) {
        if (revurdert > this || revurdert == this) return
        kontekst(hendelse)
        if (utbetaling?.hørerSammen(revurdert.utbetaling()) == false) return hendelse.error("Senere periode blokkerer revurdering av tidligere periode")
        if (hendelse.hasErrorsOrWorse()) return
        if (revurdert.erSykeperiodeRettFør(this)) return tilstand.uferdigPeriodeRettFør(this, revurdert, hendelse)
        tilstand.uferdigPeriodeFør(this, revurdert, hendelse)
    }

    override fun compareTo(other: Vedtaksperiode) = this.periode.endInclusive.compareTo(other.periode.endInclusive)

    internal fun erSykeperiodeRettFør(other: Vedtaksperiode) = this.sykdomstidslinje.erRettFør(other.sykdomstidslinje)
    internal fun erSykeperiodeAvsluttetUtenUtbetalingRettFør(other: Vedtaksperiode) =
        this.sykdomstidslinje.erRettFør(other.sykdomstidslinje) && this.tilstand == AvsluttetUtenUtbetaling

    private fun harSykeperiodeRettFør() = arbeidsgiver.finnSykeperiodeRettFør(this) != null

    internal fun periodetype() = arbeidsgiver.periodetype(periode)

    internal fun inntektskilde() = inntektskilde
    private fun inntekt() = arbeidsgiver.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start)
    private fun harInntekt() = inntekt() != null
    private fun avgjørTilstandForInntekt(): Vedtaksperiodetilstand {
        if (!harInntekt()) return AvventerInntektsmeldingFerdigForlengelse
        return AvventerHistorikk
    }

    internal fun ferdig(hendelse: IAktivitetslogg, årsak: ForkastetÅrsak) {
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        if (årsak !== ERSTATTES && !erAvsluttet() && this.tilstand !is RevurderingFeilet) tilstand(hendelse, TilInfotrygd)
        utbetaling?.forkast(hendelse)
        person.vedtaksperiodeAvbrutt(
            PersonObserver.VedtaksperiodeAvbruttEvent(
                vedtaksperiodeId = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                gjeldendeTilstand = tilstand.type
            )
        )
    }

    private fun erAvsluttet() =
        utbetaling?.erAvsluttet() == true || tilstand == AvsluttetUtenUtbetaling

    private fun erUtbetalt() = tilstand == Avsluttet && utbetalingstidslinje.harUtbetalinger()

    private fun sammeArbeidsgiverperiode(other: Vedtaksperiode): Boolean {
        val fagsystemId = utbetaling?.arbeidsgiverOppdrag()?.fagsystemId()
        return fagsystemId != null && fagsystemId == other.utbetaling?.arbeidsgiverOppdrag()?.fagsystemId()
    }

    internal fun revurder(hendelse: OverstyrTidslinje, other: Vedtaksperiode) {
        kontekst(hendelse)
        tilstand.revurder(this, hendelse, other)
    }

    internal fun sammeArbeidsgiverPeriodeOgUtbetalt(other: Vedtaksperiode) =
        sammeArbeidsgiverperiode(other) && erUtbetalt()

    internal fun periode() = periode

    private fun kontekst(hendelse: IAktivitetslogg) {
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) = hendelse.erRelevant(this.periode)

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

    private fun håndterInntektsmelding(hendelse: Inntektsmelding, hvisIngenErrors: () -> Unit) {
        arbeidsgiver.addInntekt(hendelse, skjæringstidspunkt)
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        inntektsmeldingInfo = InntektsmeldingInfo(id = hendelse.meldingsreferanseId(), arbeidsforholdId = hendelse.arbeidsforholdId)

        val tilstøtende = arbeidsgiver.finnSykeperiodeRettFør(this)
        hendelse.førsteFraværsdag?.also {
            when {
                tilstøtende == null -> if (it != skjæringstidspunkt)
                    hendelse.warn("Første fraværsdag i inntektsmeldingen er ulik skjæringstidspunktet. Kontrollér at inntektsmeldingen er knyttet til riktig periode.")
                tilstøtende.skjæringstidspunkt == skjæringstidspunkt && skjæringstidspunkt != hendelse.førsteFraværsdag ->
                    hendelse.warn("Første fraværsdag i inntektsmeldingen er forskjellig fra foregående tilstøtende periode")
            }
        }
        hendelse.valider(periode)
        if (hendelse.hasErrorsOrWorse()) {
            arbeidsgiver.søppelbøtte(hendelse, SENERE_INCLUSIVE(this), IKKE_STØTTET)
            return
        }
        hvisIngenErrors()
        hendelse.info("Fullført behandling av inntektsmelding")
    }

    private fun håndterInntektsmelding(hendelse: Inntektsmelding, hvisInntektGjelder: Vedtaksperiodetilstand, hvisInntektIkkeGjelder: Vedtaksperiodetilstand) {
        håndterInntektsmelding(hendelse) {
            tilstand(hendelse, if (hendelse.inntektenGjelderFor(skjæringstidspunkt til periode.endInclusive)) hvisInntektGjelder else hvisInntektIkkeGjelder)
        }
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        hendelse.padLeft(periode.start)
        hendelse.trimLeft(periode.endInclusive)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
        hendelseIder.add(hendelse.meldingsreferanseId())
    }

    private fun oppdaterHistorikkRevurdering(hendelse: SykdomstidslinjeHendelse) {
        hendelse.padLeft(periode.start)
        hendelse.trimLeft(periode.endInclusive)
        arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
        hendelseIder.add(hendelse.meldingsreferanseId())
    }

    private fun håndterSøknad(hendelse: SykdomstidslinjeHendelse, nesteTilstand: Vedtaksperiodetilstand?) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (!person.harFlereArbeidsgivereMedSykdom()) hendelse.validerEnArbeidsgiver()
        hendelse.valider(periode)
        if (hendelse.hasErrorsOrWorse()) {
            if (person.harFlereArbeidsgivereMedSykdom()) return person.invaliderAllePerioder(
                hendelse,
                "Invaliderer alle perioder pga flere arbeidsgivere og feil i søknad"
            )
            hendelse.error("Invaliderer alle perioder for arbeidsgiver pga feil i søknad")
            return tilstand(hendelse, TilInfotrygd)
        }
        nesteTilstand?.also { tilstand(hendelse, it) }
    }

    private fun overlappendeSøknadIkkeStøttet(søknad: Søknad, egendefinertFeiltekst: String? = null) {
        søknad.trimLeft(periode.endInclusive)
        søknad.error(egendefinertFeiltekst ?: "Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
        if (!tilstand.skalForkastesVedOverlapp) return
        tilstand(søknad, TilInfotrygd)
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
        søknad.warn("Korrigert søknad er mottatt med nye opplysninger - kontroller dagene i sykmeldingsperioden")
        håndterSøknad(søknad, nesteTilstand)
    }

    private fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand) {
        vilkårsgrunnlag.lagreInntekter(person, skjæringstidspunkt)
        val grunnlagForSykepengegrunnlag = person.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start)
        val sammenligningsgrunnlag = person.sammenligningsgrunnlag(skjæringstidspunkt)

        if (grunnlagForSykepengegrunnlag == null) return person.invaliderAllePerioder(
            vilkårsgrunnlag,
            "Har ikke inntekt på skjæringstidspunkt ved vilkårsvurdering"
        )

        if (vilkårsgrunnlag.valider(
                grunnlagForSykepengegrunnlag,
                sammenligningsgrunnlag ?: Inntekt.INGEN,
                skjæringstidspunkt,
                periodetype(),
                person.antallArbeidsgivereMedOverlappendeVedtaksperioder(this)
            ).hasErrorsOrWorse()
                .also {
                    mottaVilkårsvurdering(vilkårsgrunnlag.grunnlagsdata())
                    person.vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, skjæringstidspunkt)
                }
        ) {
            return person.invaliderAllePerioder(vilkårsgrunnlag, "Feil i vilkårsgrunnlag")
        }

        vilkårsgrunnlag.info("Vilkårsgrunnlag vurdert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun mottaVilkårsvurdering(grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        arbeidsgiver.finnSykeperiodeRettEtter(this)?.mottaVilkårsvurdering(grunnlagsdata)
    }

    private fun trengerYtelser(hendelse: IAktivitetslogg) {
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
        inntektsberegning(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
        if (Toggles.FlereArbeidsgivereUlikFom.enabled) {
            inntekterForSykepengegrunnlag(hendelse, beregningSlutt.minusMonths(2), beregningSlutt)
        }
        opptjening(hendelse)
        medlemskap(hendelse, periode.start, periode.endInclusive)
    }

    private fun trengerInntektsmelding() {
        this.person.trengerInntektsmelding(
            PersonObserver.ManglendeInntektsmeldingEvent(
                vedtaksperiodeId = this.id,
                organisasjonsnummer = this.organisasjonsnummer,
                fødselsnummer = this.fødselsnummer,
                fom = this.periode.start,
                tom = this.periode.endInclusive
            )
        )
    }

    private fun trengerIkkeInntektsmelding() {
        this.person.trengerIkkeInntektsmelding(
            PersonObserver.TrengerIkkeInntektsmeldingEvent(
                vedtaksperiodeId = this.id,
                organisasjonsnummer = this.organisasjonsnummer,
                fødselsnummer = this.fødselsnummer,
                fom = this.periode.start,
                tom = this.periode.endInclusive
            )
        )
    }

    private fun emitVedtaksperiodeReberegnet() {
        person.vedtaksperiodeReberegnet(id)
    }

    private fun emitVedtaksperiodeEndret(
        aktivitetslogg: IAktivitetslogg,
        previousState: Vedtaksperiodetilstand = tilstand
    ) {
        val event = PersonObserver.VedtaksperiodeEndretEvent(
            vedtaksperiodeId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            gjeldendeTilstand = tilstand.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = aktivitetslogg.toMap(),
            harVedtaksperiodeWarnings = person.aktivitetslogg.logg(this).let { it.hasWarningsOrWorse() && !it.hasErrorsOrWorse() },
            hendelser = hendelseIder,
            makstid = tilstand.makstid(this, LocalDateTime.now())
        )

        person.vedtaksperiodeEndret(event)
    }

    private fun vedtakFattet(hendelse: IAktivitetslogg) {
        val sykepengegrunnlag = person.sykepengegrunnlag(skjæringstidspunkt, periode.start) ?: Inntekt.INGEN
        Utbetaling.vedtakFattet(utbetaling, hendelse, person, id, periode, hendelseIder, skjæringstidspunkt, sykepengegrunnlag, inntekt() ?: Inntekt.INGEN)
    }

    private fun tickleForArbeidsgiveravhengighet(påminnelse: Påminnelse) {
        gjentaHistorikk(påminnelse, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun gjentaHistorikk(hendelse: ArbeidstakerHendelse, nesteTilstand: Vedtaksperiodetilstand) {
        hendelse.kontekst(arbeidsgiver)
        kontekst(hendelse)
        tilstand(hendelse, nesteTilstand)
    }

    private fun overlappendeVedtaksperioder() = person.nåværendeVedtaksperioder().filter { periode.overlapperMed(it.periode) }
    private fun Iterable<Vedtaksperiode>.alleAndreAvventerArbeidsgivere() =
        first() == this@Vedtaksperiode && drop(1).all { it.tilstand == AvventerArbeidsgivere }

    private fun forberedMuligUtbetaling(vilkårsgrunnlag: Vilkårsgrunnlag) {
        forberedMuligUtbetaling(vilkårsgrunnlag) { nesteTilstand ->
            håndter(vilkårsgrunnlag, nesteTilstand)
        }
    }

    private fun forberedMuligUtbetaling(inntektsmelding: Inntektsmelding) {
        forberedMuligUtbetaling(inntektsmelding) { nesteTilstandVedUtbetaling ->
            håndterInntektsmelding(inntektsmelding, nesteTilstandVedUtbetaling, AvsluttetUtenUtbetaling)
        }
    }

    private fun forberedMuligUtbetaling(arbeidstakerHendelse: ArbeidstakerHendelse, håndter: (Vedtaksperiodetilstand) -> Unit) {
        val overlappendeVedtaksperioder = overlappendeVedtaksperioder()

        håndter(
            when {
                overlappendeVedtaksperioder.alleAndreAvventerArbeidsgivere() -> AvventerHistorikk
                else -> AvventerArbeidsgivere
            }
        )

        if (tilstand == AvventerArbeidsgivere) {
            overlappendeVedtaksperioder.forEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }
        }
        gjentaHistorikk(arbeidstakerHendelse, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    /**
     * Skedulering av utbetaling opp mot andre arbeidsgivere
     */
    private fun forsøkUtbetaling(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        engineForTimeline.beregnGrenser(periode.endInclusive)

        val vedtaksperioder = person.nåværendeVedtaksperioder()
        vedtaksperioder.forEach { it.lagUtbetaling(engineForTimeline, hendelse) }
        val første = vedtaksperioder.first()
        if (første == this) return første.forsøkUtbetalingSteg2(vedtaksperioder.drop(1), hendelse)

        vedtaksperioder
            .filter { this.periode.overlapperMed(it.periode) }
            .forEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }

        this.tilstand(hendelse, AvventerArbeidsgivere)
        gjentaHistorikk(hendelse, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun lagUtbetaling(engineForTimeline: MaksimumSykepengedagerfilter, hendelse: ArbeidstakerHendelse) {
        val utbetaling = arbeidsgiver.lagUtbetaling(
            aktivitetslogg = hendelse,
            fødselsnummer = fødselsnummer,
            maksdato = engineForTimeline.maksdato(),
            forbrukteSykedager = engineForTimeline.forbrukteSykedager(),
            gjenståendeSykedager = engineForTimeline.gjenståendeSykedager(),
            periode = periode,
            forrige = utbetaling
        ).also {
            utbetalinger.add(it)
        }
        utbetalingstidslinje = utbetaling.utbetalingstidslinje(periode)
    }

    private fun forsøkUtbetalingSteg2(
        andreVedtaksperioder: List<Vedtaksperiode>,
        hendelse: ArbeidstakerHendelse
    ) {
        if (andreVedtaksperioder
                .filter { this.periode.overlapperMed(it.periode) }
                .all { it.tilstand == AvventerArbeidsgivere }
        )
            høstingsresultater(hendelse)
        else tilstand(hendelse, AvventerArbeidsgivere)
    }

    private fun høstingsresultater(hendelse: ArbeidstakerHendelse) {
        val ingenUtbetaling = !utbetaling().harUtbetalinger()
        val kunArbeidsgiverdager = utbetalingstidslinje.kunArbeidsgiverdager()
        val ingenWarnings = !person.aktivitetslogg.logg(this).hasWarningsOrWorse()

        when {
            ingenUtbetaling && kunArbeidsgiverdager && ingenWarnings -> {
                tilstand(hendelse, AvsluttetUtenUtbetaling) {
                    hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav og avluttes""")
                }
            }
            ingenUtbetaling -> {
                loggHvisForlengelse(hendelse)
                tilstand(hendelse, AvventerGodkjenning) {
                    if (kunArbeidsgiverdager)
                        hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav, men inneholder andre warnings""")
                    else
                        hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
                }
            }
            else -> {
                loggHvisForlengelse(hendelse)
                tilstand(hendelse, AvventerSimulering) {
                    hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
                }
            }
        }
    }

    private fun forsøkRevurdering(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        engineForTimeline.beregnGrenser(periode.endInclusive)

        val vedtaksperioder = person.nåværendeVedtaksperioder()
        vedtaksperioder.forEach { it.lagRevurdering(engineForTimeline, hendelse) }
        val første = vedtaksperioder.first()
        if (første == this) return første.forsøkRevurderingSteg2(vedtaksperioder.drop(1), hendelse)

        vedtaksperioder
            .filter { this.periode.overlapperMed(it.periode) }
            .forEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }

        this.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        gjentaHistorikk(hendelse, person, AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering)
    }

    private fun lagRevurdering(engineForTimeline: MaksimumSykepengedagerfilter, hendelse: ArbeidstakerHendelse) {
        val utbetaling = arbeidsgiver.lagRevurdering(
            aktivitetslogg = hendelse,
            fødselsnummer = fødselsnummer,
            maksdato = engineForTimeline.maksdato(),
            forbrukteSykedager = engineForTimeline.forbrukteSykedager(),
            gjenståendeSykedager = engineForTimeline.gjenståendeSykedager(),
            periode = periode,
            forrige = utbetaling
        ).also {
            utbetalinger.add(it)
        }
        utbetalingstidslinje = utbetaling.utbetalingstidslinje(periode)
    }

    private fun forsøkRevurderingSteg2(
        andreVedtaksperioder: List<Vedtaksperiode>,
        hendelse: ArbeidstakerHendelse
    ) {
        if (andreVedtaksperioder
                .filter { this.periode.overlapperMed(it.periode) }
                .all { it.tilstand == AvventerArbeidsgivereRevurdering }
        )
            høstingsresultaterRevurdering(hendelse)
        else tilstand(hendelse, AvventerArbeidsgivereRevurdering)
    }

    private fun høstingsresultaterRevurdering(
        hendelse: ArbeidstakerHendelse
    ) {
        when {
            !utbetaling().harUtbetalinger() && utbetalingstidslinje.kunArbeidsgiverdager() && !person.aktivitetslogg.logg(this).hasWarningsOrWorse() -> {
                tilstand(hendelse, AvsluttetUtenUtbetaling) {
                    hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav og avluttes""")
                }
            }
            !utbetaling().harUtbetalinger() -> {
                loggHvisForlengelse(hendelse)
                tilstand(hendelse, AvventerGodkjenningRevurdering) {
                    hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer godkjenning" fordi ingenting skal utbetales""")
                }
            }
            else -> {
                loggHvisForlengelse(hendelse)
                tilstand(hendelse, AvventerSimuleringRevurdering) {
                    hendelse.info("""Saken oppfyller krav for behandling, settes til "Avventer simulering"""")
                }
            }
        }
    }

    private fun loggHvisForlengelse(logg: IAktivitetslogg) {
        periodetype().also { periodetype ->
            if (periodetype != FØRSTEGANGSBEHANDLING) {
                logg.info("Perioden er en forlengelse, av type $periodetype")
            }
        }
    }

    private fun utbetaling() = checkNotNull(utbetaling) { "mangler utbetalinger" }

    private fun sendUtbetaltEvent(hendelse: IAktivitetslogg) {
        val sykepengegrunnlag = requireNotNull(person.sykepengegrunnlag(skjæringstidspunkt, periode.start)) {
            "Forventet sykepengegrunnlag ved opprettelse av utbetalt-event"
        }
        val inntekt = requireNotNull(inntekt()) {
            "Forventet inntekt ved opprettelse av utbetalt-event"
        }
        utbetaling().ferdigstill(hendelse, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
    }

    private fun håndterMuligForlengelse(
        hendelse: ArbeidstakerHendelse,
        tilstandHvisForlengelse: Vedtaksperiodetilstand,
        tilstandHvisGap: Vedtaksperiodetilstand
    ) {
        tilstand(hendelse, if (harSykeperiodeRettFør()) tilstandHvisForlengelse else tilstandHvisGap)
    }

    private fun fjernArbeidsgiverperiodeVedOverlappMedIT(infotrygdhistorikk: Infotrygdhistorikk) {
        val opprinneligPeriodeFom = periode.start
        val ytelseTom = infotrygdhistorikk.sisteSykepengedag(organisasjonsnummer) ?: return

        if (ytelseTom < opprinneligPeriodeFom) return
        if (sykdomstidslinje.fremTilOgMed(ytelseTom).harSykedager()) return

        val nyPeriodeFom = sykdomstidslinje.førsteIkkeUkjentDagEtter(ytelseTom.plusDays(1)) ?: sykmeldingsperiode.start

        periode = nyPeriodeFom til periode.endInclusive
        sykdomstidslinje = arbeidsgiver.fjernDager(opprinneligPeriodeFom til nyPeriodeFom.minusDays(1)).subset(periode)
    }

    private fun harArbeidsgivereMedOverlappendeUtbetaltePerioder(periode: Periode) =
        person.harArbeidsgivereMedOverlappendeUtbetaltePerioder(organisasjonsnummer, periode)

    override fun toString() = "${this.periode.start} - ${this.periode.endInclusive}"

    private fun Vedtaksperiodetilstand.påminnelse(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
        if (!påminnelse.gjelderTilstand(type)) return vedtaksperiode.person.vedtaksperiodeIkkePåminnet(påminnelse, vedtaksperiode.id, type)
        vedtaksperiode.person.vedtaksperiodePåminnet(vedtaksperiode.id, påminnelse)
        if (LocalDateTime.now() >= makstid(
                vedtaksperiode,
                påminnelse.tilstandsendringstidspunkt()
            )
        ) return håndterMakstid(vedtaksperiode, påminnelse)
        håndter(vedtaksperiode, påminnelse)
    }

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType
        val erFerdigbehandlet: Boolean get() = false
        val skalForkastesVedOverlapp: Boolean get() = true

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(110)

        fun håndterMakstid(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, TilInfotrygd) {
                påminnelse.error("Gir opp fordi tilstanden er nådd makstid")
            }
        }

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand", mapOf(
                    "tilstand" to type.name
                )
            )
        }

        fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.error("Mottatt sykmelding eller søknad out of order")
            vedtaksperiode.arbeidsgiver.søppelbøtte(hendelse, SENERE_INCLUSIVE(ny), IKKE_STØTTET)
        }

        fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.overlappIkkeStøttet(vedtaksperiode.periode)
            if (!skalForkastesVedOverlapp) return
            vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.overlappendeSøknadIkkeStøttet(søknad)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            søknad.trimLeft(vedtaksperiode.periode.endInclusive)
            søknad.error("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
            if (!skalForkastesVedOverlapp) return
            vedtaksperiode.tilstand(søknad, TilInfotrygd)
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
                onError { vedtaksperiode.tilstand(hendelse, TilInfotrygd) }
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
            arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
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
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            gjenopptaBehandling.info("Tidligere periode ferdig behandlet")
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: OverstyrTidslinje
        ) {
            hendelse.error("Forventet ikke overstyring fra saksbehandler i %s", type.name)
        }

        fun revurder(
            vedtaksperiode: Vedtaksperiode,
            hendelse: OverstyrTidslinje,
            other: Vedtaksperiode
        ) {
            hendelse.error("Forventet ikke overstyring fra saksbehandler i %s", type.name)
        }

        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.oppdaterHistorikk(sykmelding)
            if (vedtaksperiode.harArbeidsgivereMedOverlappendeUtbetaltePerioder(vedtaksperiode.periode)) {
                sykmelding.warn("Denne personen har en utbetaling for samme periode for en annen arbeidsgiver. Kontroller at beregningene for begge arbeidsgiverne er korrekte.")
            }
            if (sykmelding.valider(vedtaksperiode.periode).hasErrorsOrWorse())
                return vedtaksperiode.tilstand(sykmelding, TilInfotrygd)

            vedtaksperiode.tilstand(
                sykmelding, avgjørNesteTilstand(
                    vedtaksperiode = vedtaksperiode,
                    hendelse = sykmelding,
                    ferdigForlengelse = MottattSykmeldingFerdigForlengelse,
                    uferdigForlengelse = MottattSykmeldingUferdigForlengelse,
                    ferdigGap = MottattSykmeldingFerdigGap,
                    uferdigGap = MottattSykmeldingUferdigGap
                )
            )
            sykmelding.info("Fullført behandling av sykmelding")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad arbeidsgiver")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)

            // TODO: flytte til felles avgjørNesteTilstand
            if (periodeRettFør != null || ferdig) {
                if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) {
                    return vedtaksperiode.person.invaliderAllePerioder(
                        søknad,
                        "Invaliderer alle perioder for flere arbeidsgivere fordi forlengelser hos alle arbeidsgivere ikke gjelder samme periode"
                    )
                }
            }

            vedtaksperiode.håndterSøknad(
                søknad, avgjørNesteTilstand(
                    vedtaksperiode = vedtaksperiode,
                    hendelse = søknad,
                    ferdigForlengelse = vedtaksperiode.avgjørTilstandForInntekt(),
                    uferdigForlengelse = AvventerInntektsmeldingUferdigForlengelse,
                    ferdigGap = AvventerInntektsmeldingEllerHistorikkFerdigGap,
                    uferdigGap = AvventerInntektsmeldingUferdigGap
                )
            )
            søknad.info("Fullført behandling av søknad")
        }

        private fun avgjørNesteTilstand(
            vedtaksperiode: Vedtaksperiode,
            hendelse: SykdomstidslinjeHendelse,
            ferdigForlengelse: Vedtaksperiodetilstand,
            uferdigForlengelse: Vedtaksperiodetilstand,
            ferdigGap: Vedtaksperiodetilstand,
            uferdigGap: Vedtaksperiodetilstand,
        ): Vedtaksperiodetilstand {
            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val forlengelse = periodeRettFør != null
            val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)
            val refusjonOpphørt = vedtaksperiode.arbeidsgiver.harRefusjonOpphørt(vedtaksperiode.periode.endInclusive)

            if (!vedtaksperiode.person.forlengerIkkeBareAnnenArbeidsgiver(vedtaksperiode.arbeidsgiver, vedtaksperiode)) {
                vedtaksperiode.person.invaliderAllePerioder(hendelse, "Forlenger annen arbeidsgiver, men ikke seg selv")
                return TilInfotrygd
            }

            return when {
                forlengelse && refusjonOpphørt -> TilInfotrygd.also { hendelse.error("Refusjon er opphørt.") }
                forlengelse && ferdig -> ferdigForlengelse
                forlengelse && !ferdig -> uferdigForlengelse
                !forlengelse && ferdig -> ferdigGap
                !forlengelse && !ferdig -> uferdigGap
                else -> hendelse.severe("Klarer ikke bestemme hvilken tilstand vi skal til")
            }
        }
    }

    internal object MottattSykmeldingFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_FORLENGELSE
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusMonths(12)

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) return vedtaksperiode.person.invaliderAllePerioder(
                søknad, "Invaliderer alle perioder for flere arbeidsgivere fordi forlengelser hos alle arbeidsgivere ikke gjelder samme periode"
            )
            vedtaksperiode.håndterSøknad(søknad, vedtaksperiode.avgjørTilstandForInntekt())
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

    }

    internal object MottattSykmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusMonths(12)

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) return vedtaksperiode.person.invaliderAllePerioder(
                søknad,
                "Invaliderer alle perioder for flere arbeidsgivere fordi infotrygdforlengelser hos alle arbeidsgivere ikke gjelder samme periode"
            )
            vedtaksperiode.håndterSøknad(søknad, AvventerInntektsmeldingUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) {
                vedtaksperiode.tilstand(inntektsmelding, AvventerSøknadUferdigForlengelse)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.håndterMuligForlengelse(gjenopptaBehandling, MottattSykmeldingFerdigForlengelse, MottattSykmeldingFerdigGap)
        }
    }

    internal object MottattSykmeldingFerdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_GAP
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusMonths(12)

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerSøknadFerdigGap, AvventerArbeidsgiversøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) return vedtaksperiode.person.invaliderAllePerioder(
                søknad,
                "Invaliderer alle perioder for flere arbeidsgivere fordi infotrygdforlengelser hos alle arbeidsgivere ikke gjelder samme periode"
            )
            vedtaksperiode.håndterSøknad(søknad, AvventerInntektsmeldingEllerHistorikkFerdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

    }

    internal object MottattSykmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_GAP
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusMonths(12)

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling, MottattSykmeldingFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerInntektsmeldingUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerSøknadUferdigGap, AvventerArbeidsgiversøknadUferdigGap)
        }
    }

    internal object AvventerSøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (søknad.sykdomstidslinje().starterFør(vedtaksperiode.sykdomstidslinje)) {
                søknad.warn("Søknaden inneholder egenmeldingsdager som ikke er oppgitt i inntektsmeldingen. Vurder om arbeidsgiverperioden beregnes riktig")
                søknad.trimLeft(vedtaksperiode.sykdomstidslinje.førsteDag())
            }
            vedtaksperiode.håndterSøknad(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerArbeidsgiversøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgiversøknadUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgiversøknadUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

    }

    internal object AvventerArbeidsgiversøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerArbeidsgiversøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerRevurdering : Vedtaksperiodetilstand {
        // Reserverer denne tilstanden til vi skal sjonglere out-of-order revurdering
        override val type = AVVENTER_REVURDERING
    }

    internal object AvventerHistorikkRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
            hendelse.warn("Revurdering er igangsatt og må fullføres")
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.utbetaling?.forkast(hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
        ) {
            validation(ytelser) {
                onError {
                    ytelser.warn("Validering av ytelser ved revurdering feilet. Utbetalingen må annulleres")
                    vedtaksperiode.tilstand(ytelser, RevurderingFeilet)
                }
                valider { infotrygdhistorikk.valider(this, arbeidsgiver, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    arbeidsgiver.beregn(this, arbeidsgiverUtbetalinger, vedtaksperiode.periode)
                }
                onSuccess {
                    vedtaksperiode.forsøkRevurdering(arbeidsgiverUtbetalinger.tidslinjeEngine, ytelser)
                }
            }
        }
    }

    internal object AvventerArbeidsgivere : Vedtaksperiodetilstand {
        override val type = AVVENTER_ARBEIDSGIVERE

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tickleForArbeidsgiveravhengighet(påminnelse)
        }
    }

    internal object AvventerInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerUferdigGap, UtenUtbetalingMedInntektsmeldingUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerInntektsmeldingEllerHistorikkFerdigGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerInntektsmelding()
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding()
        }
    }

    internal object AvventerUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }
    }

    internal object UtenUtbetalingMedInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, UtenUtbetalingMedInntektsmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvsluttetUtenUtbetaling)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode))
                vedtaksperiode.tilstand(påminnelse, AvsluttetUtenUtbetaling)
        }
    }

    internal object UtenUtbetalingMedInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvsluttetUtenUtbetaling)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode))
                vedtaksperiode.tilstand(påminnelse, AvsluttetUtenUtbetaling)
        }
    }

    internal object AvventerInntektsmeldingFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.inntektsmeldingReplay(PersonObserver.InntektsmeldingReplayEvent(vedtaksperiode.fødselsnummer, vedtaksperiode.id))
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.forberedMuligUtbetaling(inntektsmelding)
        }
    }

    internal object AvventerInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(
                gjenopptaBehandling,
                vedtaksperiode.avgjørTilstandForInntekt(),
                AvventerInntektsmeldingEllerHistorikkFerdigGap
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerUferdigForlengelse, UtenUtbetalingMedInntektsmeldingUferdigForlengelse)
        }
    }

    internal object AvventerUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            return vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(gjenopptaBehandling, AvventerSøknadFerdigForlengelse, AvventerSøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerSøknadFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndterSøknad(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerSøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerSøknadFerdigGap)
        }
    }

    internal object AvventerInntektsmeldingEllerHistorikkFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.inntektsmeldingReplay(PersonObserver.InntektsmeldingReplayEvent(vedtaksperiode.fødselsnummer, vedtaksperiode.id))
            if (vedtaksperiode.arbeidsgiver.finnForkastetSykeperiodeRettFør(vedtaksperiode) == null) {
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.trengerHistorikkFraInfotrygd(hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.forberedMuligUtbetaling(inntektsmelding)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            hendelse: IAktivitetslogg,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            validation(hendelse) {
                onError { vedtaksperiode.tilstand(hendelse, TilInfotrygd) }
                valider {
                    infotrygdhistorikk.validerOverlappende(
                        this,
                        arbeidsgiver.avgrensetPeriode(vedtaksperiode.periode),
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                onError { person.invaliderAllePerioder(hendelse, null) }
                onSuccess {
                    if (arbeidsgiver.erForlengelse(vedtaksperiode.periode)) {
                        info("Oppdaget at perioden er en forlengelse")
                        return@onSuccess vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
                    }
                    if (!Toggles.PraksisendringEnabled.enabled) return@onSuccess

                    val førsteSykedag = vedtaksperiode.sykdomstidslinje.førsteSykedag() ?: return@onSuccess
                    val forrigeSkjæringstidspunkt = arbeidsgiver.forrigeSkjæringstidspunktInnenforArbeidsgiverperioden(
                        vedtaksperiode.regler,
                        førsteSykedag
                    ) ?: return@onSuccess

                    infotrygdhistorikk.addInntekter(person, this)
                    if (!arbeidsgiver.opprettReferanseTilInntekt(forrigeSkjæringstidspunkt, førsteSykedag)) return@onSuccess

                    vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerHistorikkFraInfotrygd(påminnelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding()
        }

    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) = tilstandsendringstidspunkt.plusDays(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.forberedMuligUtbetaling(vilkårsgrunnlag)
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
            if (vedtaksperiode.arbeidsgiver.harDagUtenSøknad(vedtaksperiode.periode)) {
                hendelse.error("Tidslinjen inneholder minst én dag med kilde sykmelding")
                return vedtaksperiode.tilstand(hendelse, TilInfotrygd)
            }
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.utbetaling?.forkast(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
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
            arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
        ) {
            vedtaksperiode.fjernArbeidsgiverperiodeVedOverlappMedIT(infotrygdhistorikk)
            val periodetype = vedtaksperiode.periodetype()
            vedtaksperiode.skjæringstidspunktFraInfotrygd = person.skjæringstidspunkt(vedtaksperiode.periode)
            validation(ytelser) {
                onError { vedtaksperiode.tilstand(ytelser, TilInfotrygd) }
                valider { infotrygdhistorikk.valider(this, arbeidsgiver, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                validerHvis("Har ikke overgang for alle arbeidsgivere i Infotrygd", periodetype == OVERGANG_FRA_IT) {
                    person.kunOvergangFraInfotrygd(vedtaksperiode)
                }
                validerHvis("Har utbetalinger fra andre arbeidsgivere etter skjæringstidspunktet", periodetype == OVERGANG_FRA_IT) {
                    person.ingenUkjenteArbeidsgivere(vedtaksperiode, vedtaksperiode.skjæringstidspunkt)
                }
                onSuccess { infotrygdhistorikk.addInntekter(person, this) }
                onSuccess { infotrygdhistorikk.lagreVilkårsgrunnlag(vedtaksperiode.skjæringstidspunkt, periodetype, person.vilkårsgrunnlagHistorikk) }

                onSuccess {
                    val vilkårsgrunnlag = person.vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt)
                        ?: return@håndter info("Mangler vilkårsgrunnlag for $vedtaksperiode.skjæringstidspunkt").also {
                            vedtaksperiode.tilstand(ytelser, AvventerVilkårsprøving)
                        }
                    if (vilkårsgrunnlag is VilkårsgrunnlagHistorikk.Grunnlagsdata && vilkårsgrunnlag.harMinimumInntekt == null) {
                        // Må gjøres frem til 1.oktober 2021. Etter denne datoen kan denne koden slettes,
                        // fordi da vil vi ikke ha noen forlengelser til perioder som har harMinimumInntekt = null til behandling lenger.
                        person.oppdaterHarMinimumInntekt(vedtaksperiode.skjæringstidspunkt, vilkårsgrunnlag)
                    }
                }
                valider {
                    val vilkårsgrunnlagElement = person.vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt)
                    if (vilkårsgrunnlagElement is VilkårsgrunnlagHistorikk.Grunnlagsdata) {
                        vilkårsgrunnlagElement.avviksprosent?.let { avvik -> avvik < Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT } ?: true
                    } else {
                        true
                    }
                }

                onSuccess {
                    when (periodetype) {
                        in listOf(OVERGANG_FRA_IT, INFOTRYGDFORLENGELSE) -> {
                            vedtaksperiode.forlengelseFraInfotrygd = JA
                        }
                        else -> {
                            vedtaksperiode.forlengelseFraInfotrygd = NEI
                            arbeidsgiver.forrigeAvsluttaPeriodeMedVilkårsvurdering(vedtaksperiode)?.also { vedtaksperiode.kopierManglende(it) }
                        }
                    }
                }
                harNødvendigInntekt(person, vedtaksperiode.skjæringstidspunkt)
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    arbeidsgiver.beregn(this, arbeidsgiverUtbetalinger, vedtaksperiode.periode)
                }
                onSuccess {
                    vedtaksperiode.forsøkUtbetaling(arbeidsgiverUtbetalinger.tidslinjeEngine, ytelser)
                }
            }
        }

    }

    private fun kopierManglende(other: Vedtaksperiode) {
        if (this.inntektsmeldingInfo == null)
            this.inntektsmeldingInfo = other.inntektsmeldingInfo?.also { this.hendelseIder.add(it.id) }
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetaling().valider(simulering).hasErrorsOrWorse())
                return vedtaksperiode.tilstand(simulering, TilInfotrygd)
            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetaling().simuler(hendelse)
        }
    }

    internal object AvventerSimuleringRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING_REVURDERING

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if(Toggles.KastAlleRevurderinger.enabled){
                hendelse.warn("Feiler revurdering for test.")
                vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
                return
            }
            vedtaksperiode.utbetaling().simuler(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.utbetaling().simuler(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetaling().valider(simulering).hasErrorsOrWorse()) {
                simulering.warn("Simulering av revurdert utbetaling feilet. Utbetalingen må annulleres")
                return vedtaksperiode.tilstand(simulering, RevurderingFeilet)
            }
            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenningRevurdering)
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigGap)
        }

        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerHistorikk)
            if (søknad.hasErrorsOrWorse()) return
            vedtaksperiode.emitVedtaksperiodeReberegnet()
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
                    vedtaksperiode.utbetaling().erAvvist() -> TilInfotrygd
                    vedtaksperiode.utbetaling().harUtbetalinger() -> TilUtbetaling
                    vedtaksperiode.utbetalingstidslinje.kunArbeidsgiverdager() -> AvsluttetUtenUtbetaling
                    else -> Avsluttet
                }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
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
            if (Toggles.RebregnUtbetalingVedHistorikkendring.enabled && infotrygdhistorikk.harEndretHistorikk(vedtaksperiode.utbetaling())) return vedtaksperiode.tilstand(
                hendelse,
                AvventerHistorikk
            ) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
            }
            vedtaksperiode.trengerGodkjenning(hendelse)
        }
    }

    private fun trengerGodkjenning(hendelse: IAktivitetslogg) {
        val aktiveVedtaksperioder = person.nåværendeVedtaksperioder().map {
            Aktivitetslogg.Aktivitet.AktivVedtaksperiode(
                it.arbeidsgiver.organisasjonsnummer(),
                it.id,
                it.periodetype()
            )
        }
        utbetaling().godkjenning(hendelse, this, skjæringstidspunkt, aktiveVedtaksperioder, inntektsmeldingInfo?.arbeidsforholdId, person.aktivitetslogg)
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

    internal object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt
                .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerGodkjenning(påminnelse)
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
                    vedtaksperiode.utbetaling()
                        .erAvvist() -> RevurderingFeilet.also { utbetalingsgodkjenning.warn("Utbetaling av revurdert periode ble avvist av saksbehandler. Utbetalingen må annulleres") }
                    vedtaksperiode.utbetaling().harUtbetalinger() -> TilUtbetaling
                    vedtaksperiode.utbetalingstidslinje.kunArbeidsgiverdager() -> AvsluttetUtenUtbetaling
                    else -> Avsluttet
                }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            if (Toggles.RevurderUtkastTilRevurdering.enabled) {
                hendelse.info("Oppdaterer tidslinjen til den overstyre perioden")
                vedtaksperiode.arbeidsgiver.låsOpp(vedtaksperiode.periode)
                vedtaksperiode.oppdaterHistorikkRevurdering(hendelse)
                vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.periode)
                vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
            }
        }
    }

    internal object AvventerArbeidsgivereRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_ARBEIDSGIVERE_REVURDERING
        // Skal denne trigge polling i Speil? Se VenterPåKiling
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val skalForkastesVedOverlapp = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som har gått til utbetaling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            val utbetaling = vedtaksperiode.utbetaling()
            when {
                utbetaling.harFeilet() -> vedtaksperiode.tilstand(hendelse, UtbetalingFeilet) {
                    hendelse.error("Utbetaling ble ikke gjennomført")
                }
                utbetaling.erUtbetalt() -> vedtaksperiode.tilstand(hendelse, Avsluttet) {
                    hendelse.info("OK fra Oppdragssystemet")
                }
                else -> hendelse.warn("Utbetalingen er ikke gjennomført. Prøver automatisk igjen senere")
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
            val utbetaling = vedtaksperiode.utbetaling()
            when {
                utbetaling.erUbetalt() -> vedtaksperiode.tilstand(påminnelse, AvventerHistorikk)
                utbetaling.erUtbetalt() -> vedtaksperiode.tilstand(påminnelse, Avsluttet)
                utbetaling.harFeilet() -> vedtaksperiode.tilstand(påminnelse, UtbetalingFeilet)
            }
        }
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET
        override val skalForkastesVedOverlapp = false

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
            sjekkUtbetalingstatus(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode med utbetaling som har feilet")
        }

        private fun sjekkUtbetalingstatus(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (!vedtaksperiode.utbetaling().erUtbetalt()) return
            vedtaksperiode.tilstand(hendelse, Avsluttet) {
                hendelse.info("OK fra Oppdragssystemet")
            }
        }
    }

    internal object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING
        override val erFerdigbehandlet = true
        override val skalForkastesVedOverlapp = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.vedtakFattet(hendelse)
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun uferdigPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun uferdigPeriodeRettFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) {
                vedtaksperiode.emitVedtaksperiodeEndret(inntektsmelding)
                vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet uten utbetaling")
        }
    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override val erFerdigbehandlet = true
        override val skalForkastesVedOverlapp = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.periode)
            vedtaksperiode.sendUtbetaltEvent(hendelse) // TODO: Fjerne når konsumentene lytter på vedtak fattet
            vedtaksperiode.vedtakFattet(hendelse)
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
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
            vedtaksperiode.arbeidsgiver.revurderSisteUtbetalte(hendelse, vedtaksperiode)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje, other: Vedtaksperiode) {
            hendelse.info("Oppdaterer tidslinjen til den overstyre perioden")
            vedtaksperiode.arbeidsgiver.låsOpp(other.periode)
            other.oppdaterHistorikkRevurdering(hendelse)
            vedtaksperiode.arbeidsgiver.lås(other.periode)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object RevurderingFeilet: Vedtaksperiodetilstand {
        override val type: TilstandType = REVURDERING_FEILET
        override val erFerdigbehandlet = true
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val erFerdigbehandlet = true

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            vedtaksperiode.person.søppelbøtte(
                hendelse,
                vedtaksperiode.periode
            )
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
    }

    internal companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

        internal fun finnForrigeAvsluttaPeriode(
            perioder: List<Vedtaksperiode>,
            vedtaksperiode: Vedtaksperiode
        ) = perioder
            .filter { it < vedtaksperiode }
            .filter { it.erAvsluttet() }
            .lastOrNull { it.skjæringstidspunkt == vedtaksperiode.skjæringstidspunkt }

        internal fun aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode: Vedtaksperiode): Aktivitetslogg {
            val tidligereUbetalt = vedtaksperiode.arbeidsgiver.finnSykeperioderAvsluttetUtenUtbetalingRettFør(vedtaksperiode)
            val aktivitetskontekster = listOf(vedtaksperiode) + tidligereUbetalt
            return vedtaksperiode.person.aktivitetslogg.logg(*aktivitetskontekster.toTypedArray())
        }

        internal fun hentVilkårsgrunnlagAktiviteter(vedtaksperiode: Vedtaksperiode): Aktivitetslogg {
            return vedtaksperiode.person.aktivitetslogg.logg("Vilkårsgrunnlag")
        }

        internal fun tidligerePerioderFerdigBehandlet(perioder: List<Vedtaksperiode>, vedtaksperiode: Vedtaksperiode) =
            perioder
                .filter { it < vedtaksperiode }
                .all { it.tilstand.erFerdigbehandlet }

        internal fun Iterable<Vedtaksperiode>.nåværendeVedtaksperiode() = firstOrNull { !it.tilstand.erFerdigbehandlet }

        internal fun List<Vedtaksperiode>.medSkjæringstidspunkt(skjæringstidspunkt: LocalDate) =
            this.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        internal fun List<Vedtaksperiode>.harInntekt() =
            this.takeIf { it.isNotEmpty() }
                ?.any { it.harInntekt() } ?: true

        internal fun overlapperMedForkastet(forkastede: Iterable<Vedtaksperiode>, hendelse: SykdomstidslinjeHendelse) {
            forkastede
                .filter { it.periode.overlapperMed(hendelse.periode()) }
                .forEach {
                    hendelse.error("${hendelse.kilde} overlapper med forkastet vedtaksperiode")
                    hendelse.info("${hendelse.kilde} overlapper med forkastet vedtaksperiode ${it.id}, hendelse sykmeldingsperiode: ${hendelse.periode()}, vedtaksperiode sykmeldingsperiode: ${it.periode}")
                }
        }

        internal fun overlapperMedForkastet(aktive: Iterable<Vedtaksperiode>, forkastede: Iterable<Vedtaksperiode>, inntektsmelding: Inntektsmelding) {
            forkastede
                .map { it.periode }
                .filterNot { forkastet -> aktive.any { aktiv -> forkastet.overlapperMed(aktiv.periode) } }
                .filter { it.overlapperMed(inntektsmelding.periode()) }
                .forEach { inntektsmelding.trimLeft(it.endInclusive) }
        }

        internal fun gjentaHistorikk(hendelse: ArbeidstakerHendelse, person: Person, nåværendeTilstand: Vedtaksperiodetilstand, nesteTilstand: Vedtaksperiodetilstand) {
            val nåværende = person.nåværendeVedtaksperioder()
            val første = nåværende.firstOrNull() ?: return
            if (nåværende
                    .filter { første.periode.overlapperMed(it.periode) }
                    .all { it.tilstand == nåværendeTilstand }
            ) {
                første.gjentaHistorikk(hendelse, nesteTilstand)
            }
        }

        internal fun Iterable<Vedtaksperiode>.harOverlappendeUtbetaltePerioder(periode: Periode) =
            any { it.periode().overlapperMed(periode) && it.harPassertPunktetHvorViOppdagerFlereArbeidsgivere() }
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

data class InntektsmeldingInfo(
    internal val id: UUID,
    internal val arbeidsforholdId: String?
)
