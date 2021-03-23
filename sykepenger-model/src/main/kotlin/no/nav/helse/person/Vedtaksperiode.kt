package no.nav.helse.person

import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dødsinformasjon
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.Arbeidsgiver.Revurdering
import no.nav.helse.person.ForkastetÅrsak.ERSTATTES
import no.nav.helse.person.ForlengelseFraInfotrygd.*
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.økonomi.Inntekt
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
    private var inntektsmeldingId: UUID?,
    private var periode: Periode,
    private var sykmeldingsperiode: Periode,
    private val utbetalinger: MutableList<Utbetaling>,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
    private var forlengelseFraInfotrygd: ForlengelseFraInfotrygd = IKKE_ETTERSPURT,
    private var inntektskilde: Inntektskilde,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val regler = NormalArbeidstaker
    private val skjæringstidspunkt
        get() =
            skjæringstidspunktFraInfotrygd
                ?: person.historie().skjæringstidspunkt(periode)
                ?: periode.start

    private val utbetaling get() = utbetalinger.lastOrNull()

    internal constructor(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        id: UUID,
        aktørId: String,
        fødselsnummer: String,
        organisasjonsnummer: String,
        tilstand: Vedtaksperiodetilstand = Start
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = id,
        aktørId = aktørId,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        tilstand = tilstand,
        skjæringstidspunktFraInfotrygd = null,
        dataForSimulering = null,
        sykdomstidslinje = Sykdomstidslinje(),
        hendelseIder = mutableListOf(),
        inntektsmeldingId = null,
        periode = Periode(LocalDate.MIN, LocalDate.MAX),
        sykmeldingsperiode = Periode(LocalDate.MIN, LocalDate.MAX),
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
            inntektsmeldingId,
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
            inntektsmeldingId,
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

    internal fun håndter(inntektsmelding: Inntektsmelding): Boolean {
        if (arbeidsgiver.harRefusjonOpphørt(periode.endInclusive) && !erAvsluttet()) {
            inntektsmelding.error("Refusjon opphører i perioden")
            tilstand(inntektsmelding, TilInfotrygd)
        }

        return overlapperMedInntektsmelding(inntektsmelding).also {
            if (!it) {
                inntektsmelding.trimLeft(periode.endInclusive)
                return it
            }
            kontekst(inntektsmelding)
            tilstand.håndter(this, inntektsmelding)
        }
    }

    internal fun håndter(inntektsmelding: InntektsmeldingReplay): Boolean {
        return overlapperMedInntektsmelding(inntektsmelding.wrapped).also {
            if (!it) {
                inntektsmelding.trimLeft(periode.endInclusive)
                return it
            }
            if (arbeidsgiver.harRefusjonOpphørt(periode.endInclusive) && !erAvsluttet()) {
                inntektsmelding.wrapped.error("Refusjon opphører i perioden")
                tilstand(inntektsmelding.wrapped, TilInfotrygd)
            }
            kontekst(inntektsmelding.wrapped)
            tilstand.håndter(this, inntektsmelding.wrapped)
        }
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk, infotrygdhistorikk: Infotrygdhistorikk) {
        if (!utbetalingshistorikk.erRelevant(id)) return
        kontekst(utbetalingshistorikk)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingshistorikk, infotrygdhistorikk)
    }

    internal fun håndter(ytelser: Ytelser, infotrygdhistorikk: Infotrygdhistorikk, dødsdato: LocalDate?) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        kontekst(ytelser)
        tilstand.håndter(person, arbeidsgiver, this, ytelser, infotrygdhistorikk, dødsdato)
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        if (!utbetalingsgodkjenning.erRelevant(id.toString())) return
        kontekst(utbetalingsgodkjenning)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingsgodkjenning)
    }

    internal fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        if (id.toString() != vilkårsgrunnlag.vedtaksperiodeId) return
        kontekst(vilkårsgrunnlag)
        tilstand.håndter(this, vilkårsgrunnlag)
    }

    internal fun håndter(simulering: Simulering) {
        if (id.toString() != simulering.vedtaksperiodeId) return
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
        if (!skalGjenopptaBehandling()) return false
        val forrige = arbeidsgiver.finnSykeperiodeRettFør(this)
        if (forrige != null && !forrige.erFerdigBehandlet()) return true
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
        return true
    }

    internal fun håndter(hendelse: OverstyrTidslinje) = hendelse.erRelevant(periode).also {
        if (!it) return it
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
    }

    override fun compareTo(other: Vedtaksperiode) = this.periode.endInclusive.compareTo(other.periode.endInclusive)

    // TODO: burde sammenligne at skjæringstidspunkt er lik for *this* og *other*, men det vil
    // ikke fungere optimalt med perioder hvor vi trenger IT-historikk for å avdekke riktig skjæringstidspunkt
    internal fun erSykeperiodeRettFør(other: Vedtaksperiode) =
        this.periode.erRettFør(other.periode) && !this.sykdomstidslinje.erSisteDagArbeidsdag() && !other.sykdomstidslinje.erFørsteDagArbeidsdag()

    internal fun starterEtter(other: Vedtaksperiode) = this.sykmeldingsperiode.start > other.sykmeldingsperiode.start

    internal fun periodetype() = when {
        forlengelseFraInfotrygd == JA ->
            arbeidsgiver.finnSykeperiodeRettFør(this)?.run { INFOTRYGDFORLENGELSE }
                ?: OVERGANG_FRA_IT
        harForegåendeSomErBehandletOgUtbetalt(this) -> FORLENGELSE
        else -> FØRSTEGANGSBEHANDLING
    }

    internal fun inntektskilde() = inntektskilde

    internal fun ferdig(hendelse: ArbeidstakerHendelse, årsak: ForkastetÅrsak) {
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        if (årsak !== ERSTATTES && !erAvsluttet()) tilstand(hendelse, TilInfotrygd)
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

    internal fun blokkererRevurdering(other: Vedtaksperiode) =
        this > other && !(utbetaling?.hørerSammen(other.utbetaling()) ?: true)

    private fun erAvsluttet() =
        utbetaling?.erAvsluttet() == true || tilstand == AvsluttetUtenUtbetalingMedInntektsmelding

    private fun skalForkastesVedOverlapp() =
        this.tilstand !in listOf(
            TilUtbetaling,
            Avsluttet,
            AvsluttetUtenUtbetalingMedInntektsmelding,
            AvsluttetUtenUtbetaling,
            UtbetalingFeilet
        )

    private fun skalGjenopptaBehandling() =
        this.tilstand !in listOf(
            TilInfotrygd,
            Avsluttet,
            AvsluttetUtenUtbetalingMedInntektsmelding,
            AvsluttetUtenUtbetaling
        )

    private fun erFerdigBehandlet() =
        this.tilstand in listOf(
            TilInfotrygd,
            Avsluttet,
            AvsluttetUtenUtbetalingMedInntektsmelding
        )

    internal fun måFerdigstilles() =
        this.tilstand !in listOf(
            TilInfotrygd,
            Avsluttet,
            AvsluttetUtenUtbetalingMedInntektsmelding,
            AvsluttetUtenUtbetaling,
            AvventerVilkårsprøvingArbeidsgiversøknad
        )

    private fun harForegåendeSomErBehandletOgUtbetalt(vedtaksperiode: Vedtaksperiode) =
        arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.erUtbetalt() ?: false

    private fun erUtbetalt() = tilstand == Avsluttet && utbetalingstidslinje.harUtbetalinger()

    private fun sammeArbeidsgiverperiode(other: Vedtaksperiode): Boolean {
        val fagsystemId = utbetaling?.arbeidsgiverOppdrag()?.fagsystemId()
        return fagsystemId != null && fagsystemId == other.utbetaling?.arbeidsgiverOppdrag()?.fagsystemId()
    }

    internal fun revurder(hendelse: ArbeidstakerHendelse) {
        require(tilstand == Avsluttet)
        kontekst(hendelse)
        tilstand(hendelse, AvventerRevurdering)
    }

    internal fun sammeArbeidsgiverPeriodeOgUtbetalt(other: Vedtaksperiode) =
        sammeArbeidsgiverperiode(other) && erUtbetalt()

    internal fun periode() = periode
    internal fun opprinneligPeriode() = sykmeldingsperiode

    private fun kontekst(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) = hendelse.erRelevant(this.sykmeldingsperiode)

    private fun overlapperMedInntektsmelding(inntektsmelding: Inntektsmelding) =
        inntektsmelding.erRelevant(this.sykmeldingsperiode)

    private fun tilstand(
        event: ArbeidstakerHendelse,
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
        emitVedtaksperiodeEndret(tilstand, event, previousState)
        tilstand.entering(this, event)
    }

    private fun håndter(hendelse: Inntektsmelding, nesteTilstand: () -> Vedtaksperiodetilstand) {
        arbeidsgiver.addInntekt(hendelse, skjæringstidspunkt)
        arbeidsgiver.trimTidligereBehandletDager(hendelse)
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        inntektsmeldingId = hendelse.meldingsreferanseId()

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
        if (hendelse.hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand())
        hendelse.info("Fullført behandling av inntektsmelding")
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        hendelse.padLeft(periode.start)
        hendelse.trimLeft(periode.endInclusive)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
        hendelseIder.add(hendelse.meldingsreferanseId())
    }

    private fun håndter(søknad: Søknad, nesteTilstand: Vedtaksperiodetilstand) {
        if (!person.harFlereArbeidsgivereMedSykdom() && søknad.harAndreInntektskilder()) return tilstand(søknad, TilInfotrygd)
        håndterSøknad(søknad) { nesteTilstand }
    }

    private fun håndter(søknad: SøknadArbeidsgiver, nesteTilstand: Vedtaksperiodetilstand) {
        håndterSøknad(søknad) { nesteTilstand }
    }

    private fun håndterSøknad(hendelse: SykdomstidslinjeHendelse, nesteTilstand: (Vedtaksperiode) -> Vedtaksperiodetilstand?) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (hendelse.valider(periode).hasErrorsOrWorse()) return person.invaliderAllePerioder(
            hendelse,
            "Invaliderer alle perioder pga flere arbeidsgivere og feil i søknad"
        )
        nesteTilstand(this)?.also { tilstand(hendelse, it) }
    }

    private fun overlappendeSøknadIkkeStøttet(søknad: Søknad, egendefinertFeiltekst: String? = null) {
        søknad.trimLeft(periode.endInclusive)
        søknad.error(egendefinertFeiltekst ?: "Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
        if (!skalForkastesVedOverlapp()) return
        tilstand(søknad, TilInfotrygd)
    }

    private fun håndterOverlappendeSøknad(søknad: Søknad, nesteTilstand: (Vedtaksperiode) -> Vedtaksperiodetilstand? = { null }) {
        if (søknad.sykdomstidslinje().erSisteDagArbeidsdag()) return overlappendeSøknadIkkeStøttet(
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

        if (grunnlagForSykepengegrunnlag == null) return tilstand(vilkårsgrunnlag, TilInfotrygd) {
            vilkårsgrunnlag.error("Har ikke inntekt på skjæringstidspunkt $skjæringstidspunkt ved vilkårsvurdering")
        }

        if (vilkårsgrunnlag.valider(grunnlagForSykepengegrunnlag, sammenligningsgrunnlag ?: Inntekt.INGEN, skjæringstidspunkt, periodetype()).hasErrorsOrWorse()
                .also {
                    mottaVilkårsvurdering(vilkårsgrunnlag.grunnlagsdata())
                    person.vilkårsgrunnlagHistorikk.lagre(vilkårsgrunnlag, skjæringstidspunkt)
                }
        ) {
            vilkårsgrunnlag.info("Feil i vilkårsgrunnlag i %s", tilstand.type)
            return tilstand(vilkårsgrunnlag, TilInfotrygd)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag verifisert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun mottaVilkårsvurdering(grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        arbeidsgiver.finnSykeperiodeRettEtter(this)?.mottaVilkårsvurdering(grunnlagsdata)
    }

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        trengerGapHistorikkFraInfotrygd(hendelse)
        foreldrepenger(hendelse)
        pleiepenger(hendelse, periode)
        omsorgspenger(hendelse, periode)
        opplæringspenger(hendelse, periode)
        institusjonsopphold(hendelse, periode)
        arbeidsavklaringspenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
        dagpenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
    }

    private fun trengerPersoninfo(hendelse: ArbeidstakerHendelse) {
        dødsinformasjon(hendelse)
    }

    private fun trengerKortHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(hendelse, periode)
    }

    private fun trengerGapHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(hendelse, arbeidsgiver.sykdomstidslinje().førsteDag().minusYears(4) til periode.endInclusive)
    }

    private fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntektsberegning(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
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
        currentState: Vedtaksperiodetilstand,
        hendelse: ArbeidstakerHendelse,
        previousState: Vedtaksperiodetilstand
    ) {
        val event = PersonObserver.VedtaksperiodeEndretTilstandEvent(
            vedtaksperiodeId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            gjeldendeTilstand = currentState.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = hendelse.aktivitetsloggMap(),
            hendelser = hendelseIder,
            makstid = currentState.makstid(this, LocalDateTime.now())
        )

        person.vedtaksperiodeEndret(event)
    }

    private fun vedtakFattet(hendelse: ArbeidstakerHendelse) {
        val sykepengegrunnlag = person.sykepengegrunnlag(skjæringstidspunkt, periode.start) ?: Inntekt.INGEN
        val inntekt = arbeidsgiver.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start) ?: Inntekt.INGEN
        Utbetaling.vedtakFattet(utbetaling, hendelse, person, id, periode, hendelseIder, skjæringstidspunkt, sykepengegrunnlag, inntekt)
    }

    private fun tickleForArbeidsgiveravhengighet(påminnelse: Påminnelse) {
        gjentaHistorikk(påminnelse, person)
    }

    private fun gjentaHistorikk(hendelse: ArbeidstakerHendelse) {
        hendelse.kontekst(arbeidsgiver)
        kontekst(hendelse)
        tilstand(hendelse, AvventerHistorikk)
    }

    /**
     * Skedulering av utbetaling opp mot andre arbeidsgivere
     */
    private fun forsøkUtbetaling(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder()
        val første = vedtaksperioder.first()
        if (første == this) return første.forsøkUtbetalingSteg2(vedtaksperioder.drop(1), engineForTimeline, hendelse)

        vedtaksperioder
            .filter { this.periode.overlapperMed(it.periode) }
            .forEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }

        this.tilstand(hendelse, AvventerArbeidsgivere)
        gjentaHistorikk(hendelse, person)
    }

    private fun forsøkRevurdering(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder()
        val første = vedtaksperioder.first()
        if (første == this) return første.forsøkRevurderingSteg2(vedtaksperioder.drop(1), engineForTimeline, hendelse)

        this.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        if (første.tilstand == AvventerArbeidsgivere) {
            første.tilstand(hendelse, AvventerRevurdering)
        }
    }

    private fun forsøkRevurderingSteg2(
        andreVedtaksperioder: List<Vedtaksperiode>,
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        if (andreVedtaksperioder
                .filter { this.periode.overlapperMed(it.periode) }
                .all { it.tilstand == AvventerArbeidsgivereRevurdering }
        )
            høstingsresultaterRevurdering(engineForTimeline, hendelse)
        else tilstand(hendelse, AvventerArbeidsgivereRevurdering)
    }

    private fun forsøkUtbetalingSteg2(
        andreVedtaksperioder: List<Vedtaksperiode>,
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        if (andreVedtaksperioder
                .filter { this.periode.overlapperMed(it.periode) }
                .all { it.tilstand == AvventerArbeidsgivere }
        )
            høstingsresultater(engineForTimeline, hendelse, andreVedtaksperioder)
        else tilstand(hendelse, AvventerArbeidsgivere)
    }

    private fun høstingsresultater(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse,
        andreVedtaksperioder: List<Vedtaksperiode>
    ) {
        engineForTimeline.beregnGrenser(periode.endInclusive)
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
        andreVedtaksperioder.forEach { it.utbetalingstidslinje = it.arbeidsgiver.nåværendeTidslinje().subset(it.periode) }

        val ingenUtbetaling = !utbetaling().harUtbetalinger()
        val kunArbeidsgiverdager = utbetalingstidslinje.kunArbeidsgiverdager()
        val ingenWarnings = !person.aktivitetslogg.logg(this).hasWarningsOrWorse()

        when {
            ingenUtbetaling && kunArbeidsgiverdager && ingenWarnings -> {
                tilstand(hendelse, AvsluttetUtenUtbetalingMedInntektsmelding) {
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

    private fun høstingsresultaterRevurdering(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        engineForTimeline.beregnGrenser(periode.endInclusive)
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

        when {
            !utbetaling().harUtbetalinger() && utbetalingstidslinje.kunArbeidsgiverdager() && !person.aktivitetslogg.logg(this).hasWarningsOrWorse() -> {
                tilstand(hendelse, AvsluttetUtenUtbetalingMedInntektsmelding) {
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

    private fun erForlengelseAvAvsluttetUtenUtbetalingMedInntektsmelding(): Boolean {
        val sykeperiodeRettFør = arbeidsgiver.finnSykeperiodeRettFør(this) ?: return false
        if (sykeperiodeRettFør.tilstand == AvsluttetUtenUtbetalingMedInntektsmelding) return true
        return sykeperiodeRettFør.erForlengelseAvAvsluttetUtenUtbetalingMedInntektsmelding()
    }

    private fun loggHvisForlengelse(logg: IAktivitetslogg) {
        periodetype().also { periodetype ->
            if (periodetype != FØRSTEGANGSBEHANDLING) {
                logg.info("Perioden er en forlengelse, av type $periodetype")
            }
        }
    }

    private fun utbetaling() = checkNotNull(utbetaling) { "mangler utbetalinger" }

    private fun sendUtbetaltEvent(hendelse: ArbeidstakerHendelse) {
        val sykepengegrunnlag = requireNotNull(person.sykepengegrunnlag(skjæringstidspunkt, periode.start)) {
            "Forventet sykepengegrunnlag ved opprettelse av utbetalt-event"
        }
        val inntekt = requireNotNull(arbeidsgiver.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start)) {
            "Forventet inntekt ved opprettelse av utbetalt-event"
        }
        utbetaling().ferdigstill(hendelse, person, periode, sykepengegrunnlag, inntekt, hendelseIder)
    }

    private fun håndterMuligForlengelse(
        hendelse: ArbeidstakerHendelse,
        tilstandHvisForlengelse: Vedtaksperiodetilstand,
        tilstandHvisGap: Vedtaksperiodetilstand
    ) {
        tilstand(hendelse, arbeidsgiver.finnSykeperiodeRettFør(this)?.let { tilstandHvisForlengelse } ?: tilstandHvisGap)
    }

    internal fun avventerRevurdering(other: Vedtaksperiode, revurdering: Revurdering): Boolean {
        if (this <= other) return false
        tilstand.håndter(this, revurdering)
        return true
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

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.error("Mottatt overlappende sykmeldinger - det støttes ikke før replay av hendelser er på plass")
            if (!vedtaksperiode.skalForkastesVedOverlapp()) return
            vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.overlappendeSøknadIkkeStøttet(søknad)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            søknad.trimLeft(vedtaksperiode.periode.endInclusive)
            søknad.error("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
            if (!vedtaksperiode.skalForkastesVedOverlapp()) return
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
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            val historie = person.historie()
            validation(utbetalingshistorikk) {
                onError {
                    vedtaksperiode.tilstand(utbetalingshistorikk, TilInfotrygd)
                }
                valider {
                    infotrygdhistorikk.validerOverlappende(this, historie, vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)
                }
                onSuccess {
                    utbetalingshistorikk.info("Utbetalingshistorikk sjekket; fant ingen feil.")
                }
            }
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            dødsdato: LocalDate?
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
            vedtaksperiode.trengerKortHistorikkFraInfotrygd(påminnelse)
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

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {}

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}

        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            vedtaksperiode.periode = sykmelding.periode()
            vedtaksperiode.sykmeldingsperiode = sykmelding.periode()
            vedtaksperiode.oppdaterHistorikk(sykmelding)

            if (sykmelding.valider(vedtaksperiode.periode).hasErrorsOrWorse())
                return vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
            if (!vedtaksperiode.person.forlengerIkkeBareAnnenArbeidsgiver(vedtaksperiode.arbeidsgiver, vedtaksperiode))
                return vedtaksperiode.person.invaliderAllePerioder(sykmelding, "Forlenger annen arbeidsgiver, men ikke seg selv")
            if (vedtaksperiode.arbeidsgiver.harPeriodeEtter(vedtaksperiode))
                return vedtaksperiode.tilstand(sykmelding, TilInfotrygd)

            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val forlengelse = periodeRettFør != null
            val ferdig =
                vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode) && periodeRettFør?.let { it.tilstand != AvsluttetUtenUtbetaling } ?: true
            val refusjonOpphørt = vedtaksperiode.arbeidsgiver.harRefusjonOpphørt(vedtaksperiode.periode.endInclusive)
            vedtaksperiode.tilstand(
                sykmelding, when {
                    forlengelse && refusjonOpphørt -> TilInfotrygd
                    forlengelse && ferdig -> MottattSykmeldingFerdigForlengelse
                    forlengelse && !ferdig -> MottattSykmeldingUferdigForlengelse
                    !forlengelse && ferdig -> MottattSykmeldingFerdigGap
                    !forlengelse && !ferdig -> MottattSykmeldingUferdigGap
                    else -> sykmelding.severe("Klarer ikke bestemme hvilken sykmeldingmottattilstand vi skal til")
                }
            )
            sykmelding.info("Fullført behandling av sykmelding")
        }
    }

    internal object MottattSykmeldingFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) return vedtaksperiode.person.invaliderAllePerioder(
                søknad, "Invaliderer alle perioder for flere arbeidsgivere fordi forlengelser hos alle arbeidsgivere ikke gjelder samme periode"
            )
            vedtaksperiode.håndter(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }
    }

    internal object MottattSykmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) return vedtaksperiode.person.invaliderAllePerioder(
                søknad,
                "Invaliderer alle perioder for flere arbeidsgivere fordi infotrygdforlengelser hos alle arbeidsgivere ikke gjelder samme periode"
            )
            vedtaksperiode.håndter(søknad, AvventerInntektsmeldingUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding) { AvventerSøknadUferdigForlengelse }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
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

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding) {
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.skjæringstidspunkt til vedtaksperiode.periode.endInclusive)) {
                    AvventerSøknadFerdigGap
                } else {
                    AvventerArbeidsgiversøknadFerdigGap
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (!vedtaksperiode.person.forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode)) return vedtaksperiode.person.invaliderAllePerioder(
                søknad,
                "Invaliderer alle perioder for flere arbeidsgivere fordi infotrygdforlengelser hos alle arbeidsgivere ikke gjelder samme periode"
            )
            vedtaksperiode.håndter(søknad, AvventerInntektsmeldingEllerHistorikkFerdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            if (!Toggles.FlereArbeidsgivereFørstegangsbehandling.enabled) {
                if (vedtaksperiode.person.harOverlappendePeriodeHosAnnenArbeidsgiver(vedtaksperiode))
                    return vedtaksperiode.person.invaliderAllePerioder(
                        søknad,
                        "Invaliderer alle perioder for flere arbeidsgivere fordi førstegangsbehandling ikke støttes"
                    )
            }
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigGap)
        }
    }

    internal object MottattSykmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_GAP

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling, MottattSykmeldingFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerInntektsmeldingUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            if (!Toggles.FlereArbeidsgivereFørstegangsbehandling.enabled) {
                if (vedtaksperiode.person.harOverlappendePeriodeHosAnnenArbeidsgiver(vedtaksperiode))
                    return vedtaksperiode.person.invaliderAllePerioder(
                        søknad,
                        "Invaliderer alle perioder for flere arbeidsgivere fordi førstegangsbehandling ikke støttes"
                    )
            }
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding) {
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.skjæringstidspunkt til vedtaksperiode.periode.endInclusive)) {
                    AvventerSøknadUferdigGap
                } else {
                    AvventerArbeidsgiversøknadUferdigGap
                }
            }
        }
    }

    internal object AvventerSøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (søknad.sykdomstidslinje().starterFør(vedtaksperiode.sykdomstidslinje)) {
                søknad.warn("Søknaden inneholder egenmeldingsdager som ikke er oppgitt i inntektsmeldingen. Vurder om arbeidsgiverperioden beregnes riktig")
                søknad.trimLeft(vedtaksperiode.sykdomstidslinje.førsteDag())
            }
            vedtaksperiode.håndter(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingArbeidsgiversøknad)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerArbeidsgiversøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_ARBEIDSGIVERSØKNAD_FERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetalingMedInntektsmelding)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetalingMedInntektsmelding)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgiversøknadUferdigGap)
        }
    }

    internal object AvventerArbeidsgiversøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_ARBEIDSGIVERSØKNAD_UFERDIG_GAP

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerArbeidsgiversøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetalingMedInntektsmelding)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetalingMedInntektsmelding)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_REVURDERING

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.revurdering(vedtaksperiode, hendelse)
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.trengerPersoninfo(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            dødsdato: LocalDate?
        ) {
            val historie = person.historie()
            val periodetype = historie.periodetype(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)
            validation(ytelser) {
                onError { vedtaksperiode.tilstand(ytelser, AvsluttetIngenEndring) }
                valider {
                    infotrygdhistorikk.valider(this, historie, vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt)
                }
                valider {
                    ytelser.valider(
                        vedtaksperiode.periode,
                        historie.avgrensetPeriode(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode),
                        periodetype,
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                lateinit var engineForTimeline: ArbeidsgiverUtbetalinger
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    val utbetalingstidslinjer = try {
                        person.utbetalingstidslinjer(vedtaksperiode.periode, historie)
                    } catch (e: IllegalArgumentException) {
                        sikkerLogg.warn("Feilet i builder for vedtaksperiode ${vedtaksperiode.id} - ${e.message}")
                        return@valider false
                    }
                    engineForTimeline = ArbeidsgiverUtbetalinger(
                        tidslinjer = utbetalingstidslinjer,
                        personTidslinje = historie.utbetalingstidslinjeFraInfotrygd(vedtaksperiode.periode),
                        periode = vedtaksperiode.periode,
                        alder = Alder(vedtaksperiode.fødselsnummer),
                        arbeidsgiverRegler = NormalArbeidstaker,
                        aktivitetslogg = ytelser,
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        dødsdato = dødsdato
                    ).also { engine ->
                        engine.beregn()
                    }
                    !ytelser.hasErrorsOrWorse()
                }
                onSuccess {
                    vedtaksperiode.forsøkRevurdering(engineForTimeline.tidslinjeEngine, ytelser)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding) {
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.skjæringstidspunkt til vedtaksperiode.periode.endInclusive)
                ) {
                    AvventerUferdigGap
                } else {
                    UtenUtbetalingMedInntektsmeldingUferdigGap
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerInntektsmeldingEllerHistorikkFerdigGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding()
        }
    }

    internal object AvventerUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }
    }

    internal object UtenUtbetalingMedInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvsluttetUtenUtbetalingMedInntektsmelding)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode))
                vedtaksperiode.tilstand(påminnelse, AvsluttetUtenUtbetalingMedInntektsmelding)
        }
    }

    internal object UtenUtbetalingMedInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = UTEN_UTBETALING_MED_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvsluttetUtenUtbetalingMedInntektsmelding)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            if (vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode))
                vedtaksperiode.tilstand(påminnelse, AvsluttetUtenUtbetalingMedInntektsmelding)
        }
    }

    internal object AvventerInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(gjenopptaBehandling, AvventerHistorikk, AvventerInntektsmeldingEllerHistorikkFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding) {
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.skjæringstidspunkt til vedtaksperiode.periode.endInclusive)) {
                    AvventerUferdigForlengelse
                } else {
                    UtenUtbetalingMedInntektsmeldingUferdigForlengelse
                }
            }
        }
    }

    internal object AvventerUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            val harVilkårsgrunnlag = vedtaksperiode.person.vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt) != null
            if (harVilkårsgrunnlag) return vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
            return vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(gjenopptaBehandling, MottattSykmeldingFerdigForlengelse, AvventerSøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingArbeidsgiversøknad)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerVilkårsprøvingArbeidsgiversøknad : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_ARBEIDSGIVERSØKNAD

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
            hendelse.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndter(vilkårsgrunnlag, AvsluttetUtenUtbetalingMedInntektsmelding)
        }
    }

    internal object AvventerSøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerSøknadFerdigGap)
        }
    }

    internal object AvventerInntektsmeldingEllerHistorikkFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.person.inntektsmeldingReplay(PersonObserver.InntektsmeldingReplayEvent(vedtaksperiode.fødselsnummer, vedtaksperiode.id))
            if (vedtaksperiode.arbeidsgiver.finnForkastetSykeperiodeRettFør(vedtaksperiode) == null) {
                vedtaksperiode.trengerInntektsmelding()
            }
            vedtaksperiode.trengerGapHistorikkFraInfotrygd(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            val vedtaksperioder = vedtaksperiode.person.nåværendeVedtaksperioder()
            val første = vedtaksperioder.first()
            val overlappendeVedtaksperioder = vedtaksperioder.filter { vedtaksperiode.periode.overlapperMed(it.periode) }

            vedtaksperiode.håndter(inntektsmelding) juice@{
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.skjæringstidspunkt til vedtaksperiode.periode.endInclusive)) {
                    if (første == vedtaksperiode && overlappendeVedtaksperioder.drop(1).all { it.tilstand == AvventerArbeidsgivere }) {
                        return@juice AvventerHistorikk
                    }
                    overlappendeVedtaksperioder.forEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }
                    AvventerArbeidsgivere
                } else {
                    AvsluttetUtenUtbetalingMedInntektsmelding
                }
            }.also {
                gjentaHistorikk(inntektsmelding, vedtaksperiode.person)
            }
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
            val historie = person.historie()
            validation(utbetalingshistorikk) {
                onError {
                    vedtaksperiode.tilstand(utbetalingshistorikk, TilInfotrygd)
                }
                valider {
                    infotrygdhistorikk.validerOverlappende(this, historie, vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)
                }
                onError {
                    person.invaliderAllePerioder(utbetalingshistorikk, null)
                }
                validerHvis("Er ikke overgang fra IT og har flere arbeidsgivere", !Toggles.FlereArbeidsgivereFørstegangsbehandling.enabled) {
                    historie.forlengerInfotrygd(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode) || !person.harFlereArbeidsgivereMedSykdom()
                }
                onSuccess {
                    if (historie.erForlengelse(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)) {
                        utbetalingshistorikk.info("Oppdaget at perioden er en forlengelse")
                        return@onSuccess vedtaksperiode.tilstand(utbetalingshistorikk, AvventerHistorikk)
                    }
                    if (!Toggles.PraksisendringEnabled.enabled) return@onSuccess

                    val førsteSykedag = vedtaksperiode.sykdomstidslinje.førsteSykedag() ?: return@onSuccess
                    val forrigeSkjæringstidspunkt = historie.forrigeSkjæringstidspunktInnenforArbeidsgiverperioden(
                        vedtaksperiode.regler,
                        vedtaksperiode.organisasjonsnummer,
                        førsteSykedag
                    ) ?: return@onSuccess

                    infotrygdhistorikk.addInntekter(person, this)
                    if (!arbeidsgiver.opprettReferanseTilInntekt(forrigeSkjæringstidspunkt, førsteSykedag)) return@onSuccess

                    vedtaksperiode.tilstand(utbetalingshistorikk, AvventerHistorikk)
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerGapHistorikkFraInfotrygd(påminnelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding()
        }

    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) = tilstandsendringstidspunkt.plusDays(1)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            val vedtaksperioder = vedtaksperiode.person.nåværendeVedtaksperioder()
            val første = vedtaksperioder.first()
            if (første == vedtaksperiode) {
                if (vedtaksperioder.drop(1)
                        .filter { vedtaksperiode.periode.overlapperMed(it.periode) }
                        .all { it.tilstand == AvventerArbeidsgivere }
                ) {
                    return vedtaksperiode.håndter(vilkårsgrunnlag, AvventerHistorikk)
                }
            }
            return vedtaksperiode.håndter(vilkårsgrunnlag, AvventerArbeidsgivere).also {
                vedtaksperioder
                    .filter { vedtaksperiode.periode.overlapperMed(it.periode) }
                    .onEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }

                gjentaHistorikk(vilkårsgrunnlag, vedtaksperiode.person)
            }
        }
    }

    @Deprecated(":)")
    internal object AvventerVilkårsprøvingGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
            hendelse.info("Forespør vilkårsgrunnlag")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            if (!Toggles.FlereArbeidsgivereFørstegangsbehandling.enabled) return vedtaksperiode.håndter(vilkårsgrunnlag, AvventerHistorikk)
            val vedtaksperioder = vedtaksperiode.person.nåværendeVedtaksperioder()
            val første = vedtaksperioder.first()
            if (første == vedtaksperiode) {
                if (vedtaksperioder.drop(1)
                        .filter { vedtaksperiode.periode.overlapperMed(it.periode) }
                        .all { it.tilstand == AvventerArbeidsgivere }
                ) {
                    return vedtaksperiode.håndter(vilkårsgrunnlag, AvventerHistorikk)
                }
            }
            return vedtaksperiode.håndter(vilkårsgrunnlag, AvventerArbeidsgivere).also {
                vedtaksperioder
                    .filter { vedtaksperiode.periode.overlapperMed(it.periode) }
                    .onEach { it.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE }

                gjentaHistorikk(vilkårsgrunnlag, vedtaksperiode.person)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, AvventerUferdigGap)
        }
    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (vedtaksperiode.arbeidsgiver.sykdomstidslinje().harDagUtenSøknad(vedtaksperiode.periode)) {
                hendelse.error("Tidslinjen inneholder minst én dag med kilde sykmelding")
                return vedtaksperiode.tilstand(hendelse, TilInfotrygd)
            }
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.trengerPersoninfo(hendelse)
            vedtaksperiode.utbetaling?.forkast(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
            vedtaksperiode.trengerPersoninfo(påminnelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser,
            infotrygdhistorikk: Infotrygdhistorikk,
            dødsdato: LocalDate?
        ) {
            vedtaksperiode.fjernArbeidsgiverperiodeVedOverlappMedIT(infotrygdhistorikk)
            val historie = person.historie()
            val periodetype = historie.periodetype(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)
            val skjæringstidspunkt = vedtaksperiode.skjæringstidspunkt
            validation(ytelser) {
                onError { vedtaksperiode.tilstand(ytelser, TilInfotrygd) }
                onSuccess {
                    vedtaksperiode.skjæringstidspunktFraInfotrygd = skjæringstidspunkt
                }
                valider {
                    infotrygdhistorikk.valider(this, historie, vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode, skjæringstidspunkt)
                }
                valider {
                    ytelser.valider(
                        vedtaksperiode.periode,
                        historie.avgrensetPeriode(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode),
                        periodetype,
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                validerHvis("Har ikke overgang for alle arbeidsgivere i Infotrygd", periodetype == OVERGANG_FRA_IT) {
                    person.harForlengelseForAlleArbeidsgivereIInfotrygdhistorikken(historie, vedtaksperiode)
                }
                onSuccess { infotrygdhistorikk.addInntekter(person, this) }
                onSuccess { infotrygdhistorikk.lagreVilkårsgrunnlag(skjæringstidspunkt, periodetype, person.vilkårsgrunnlagHistorikk) }

                valider {
                    person.vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)?.isOk()
                        ?: return@håndter vedtaksperiode.tilstand(ytelser, AvventerVilkårsprøving)
                }

                onSuccess {
                    when (periodetype) {
                        in listOf(OVERGANG_FRA_IT, INFOTRYGDFORLENGELSE) -> {
                            vedtaksperiode.forlengelseFraInfotrygd = JA
                        }
                        else -> {
                            vedtaksperiode.forlengelseFraInfotrygd = NEI
                            arbeidsgiver.forrigeAvsluttaPeriodeMedVilkårsvurdering(vedtaksperiode, historie)?.also { vedtaksperiode.kopierManglende(it) }
                        }
                    }
                }
                harNødvendigInntekt(person, skjæringstidspunkt)
                lateinit var engineForTimeline: ArbeidsgiverUtbetalinger
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    val utbetalingstidslinjer = try {
                        person.utbetalingstidslinjer(vedtaksperiode.periode, historie)
                    } catch (e: IllegalArgumentException) {
                        sikkerLogg.warn("Feilet i builder for vedtaksperiode ${vedtaksperiode.id} - ${e.message}")
                        return@valider false
                    }
                    engineForTimeline = ArbeidsgiverUtbetalinger(
                        tidslinjer = utbetalingstidslinjer,
                        personTidslinje = historie.utbetalingstidslinjeFraInfotrygd(vedtaksperiode.periode),
                        periode = vedtaksperiode.periode,
                        alder = Alder(vedtaksperiode.fødselsnummer),
                        arbeidsgiverRegler = vedtaksperiode.regler,
                        aktivitetslogg = ytelser,
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        dødsdato = dødsdato
                    ).also { engine ->
                        engine.beregn()
                    }
                    !ytelser.hasErrorsOrWorse()
                }
                onSuccess {
                    vedtaksperiode.forsøkUtbetaling(engineForTimeline.tidslinjeEngine, ytelser)
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
        }
    }

    private fun kopierManglende(other: Vedtaksperiode) {
        if (this.inntektsmeldingId == null)
            this.inntektsmeldingId = other.inntektsmeldingId?.also { this.hendelseIder.add(it) }
    }

    internal object AvventerSimulering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad) { AvventerHistorikk }
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.utbetaling().simuler(hendelse)
        }
    }

    internal object AvventerSimuleringRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_SIMULERING_REVURDERING

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt.plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.utbetaling().simuler(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.utbetaling().simuler(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetaling().valider(simulering).hasErrorsOrWorse())
                return vedtaksperiode.tilstand(simulering, AvsluttetIngenEndring)
            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenningRevurdering)
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt
                .plusDays(35)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad) {
                it.emitVedtaksperiodeReberegnet()
                AvventerHistorikk
            }
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
                    vedtaksperiode.utbetalingstidslinje.kunArbeidsgiverdager() -> AvsluttetUtenUtbetalingMedInntektsmelding
                    else -> Avsluttet
                }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Revurdering) {
            hendelse.info("Hopper tilbake i en ventetilstand fordi en betalt periode er gått til revurdering")
            if (vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode) != null)
                return vedtaksperiode.tilstand(hendelse, AvventerUferdigForlengelse)
            vedtaksperiode.tilstand(hendelse, AvventerUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerGodkjenning(påminnelse)
        }
    }

    private fun trengerGodkjenning(hendelse: ArbeidstakerHendelse) {
        val aktiveVedtaksperioder = person.nåværendeVedtaksperioder().map {
            Aktivitetslogg.Aktivitet.AktivVedtaksperiode(
                it.arbeidsgiver.organisasjonsnummer(),
                it.id,
                it.periodetype()
            )
        }
        utbetaling().godkjenning(hendelse, this, aktiveVedtaksperioder, person.aktivitetslogg)
    }

    internal object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt
                .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
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
                        .erAvvist() -> AvsluttetIngenEndring.also { utbetalingsgodkjenning.warn("Revurdering er avvist av saksbehandler") }
                    vedtaksperiode.utbetaling().harUtbetalinger() -> TilUtbetaling
                    vedtaksperiode.utbetalingstidslinje.kunArbeidsgiverdager() -> AvsluttetUtenUtbetalingMedInntektsmelding
                    else -> Avsluttet
                }
            )
        }
    }

    internal object AvventerArbeidsgivereRevurdering : Vedtaksperiodetilstand {
        override val type: TilstandType = AVVENTER_ARBEIDSGIVERE_REVURDERING
        // Skal denne trigge polling i Speil? Se VenterPåKiling
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}

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
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            val utbetaling = vedtaksperiode.utbetaling()
            when {
                utbetaling.erUtbetalt() -> vedtaksperiode.tilstand(påminnelse, Avsluttet)
                utbetaling.harFeilet() -> vedtaksperiode.tilstand(påminnelse, UtbetalingFeilet)
            }
        }
    }

    internal object TilAnnullering : Vedtaksperiodetilstand {
        override val type = TIL_ANNULLERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som har gått til annullering")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {}

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

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding) {
                val forrige = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.skjæringstidspunkt til vedtaksperiode.periode.endInclusive)
                    && (forrige == null || forrige.skjæringstidspunkt != vedtaksperiode.skjæringstidspunkt)
                ) {
                    AvventerVilkårsprøvingArbeidsgiversøknad
                } else {
                    AvsluttetUtenUtbetalingMedInntektsmelding
                }
            }
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet uten utbetaling")
        }
    }

    internal object AvsluttetUtenUtbetalingMedInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.vedtakFattet(hendelse)
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet med inntektsmelding")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.periode)
            vedtaksperiode.sendUtbetaltEvent(hendelse) // TODO: Fjerne når konsumentene lytter på vedtak fattet
            vedtaksperiode.vedtakFattet(hendelse)
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk,
            infotrygdhistorikk: Infotrygdhistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            if (Toggles.RevurderUtbetaltPeriode.enabled) {
                if (vedtaksperiode.arbeidsgiver.blokkeresRevurdering(vedtaksperiode)) hendelse.severe("Overstyrer ikke en vedtaksperiode som er avsluttet")
                vedtaksperiode.arbeidsgiver.låsOpp(vedtaksperiode.periode)
                vedtaksperiode.oppdaterHistorikk(hendelse)
                vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.periode)

                // Som en sideeffekt -> dytt perioden foran over i AVVENTER_UFERDIG_FORLENGELSE
                vedtaksperiode.arbeidsgiver.revurder(vedtaksperiode, hendelse)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object AvsluttetIngenEndring : Vedtaksperiodetilstand {
        override val type: TilstandType = AVSLUTTET_INGEN_ENDRING
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
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
            utbetalingshistorikk: Utbetalingshistorikk,
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
            vedtaksperiode: Vedtaksperiode,
            skjæringstidspunkt: LocalDate,
            historie: Historie
        ) = perioder
            .filter { it < vedtaksperiode }
            .filter { it.erAvsluttet() }
            .filter { historie.skjæringstidspunkt(it.periode()) == skjæringstidspunkt }
            .lastOrNull()

        internal fun aktivitetsloggMedForegåendeUtenUtbetaling(vedtaksperiode: Vedtaksperiode): Aktivitetslogg {
            val tidligereUbetalt = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.takeIf {
                it.tilstand in listOf(AvsluttetUtenUtbetaling, AvsluttetUtenUtbetalingMedInntektsmelding)
            }
            val aktivitetskontekster = listOfNotNull<Aktivitetskontekst>(vedtaksperiode, tidligereUbetalt)
            return vedtaksperiode.person.aktivitetslogg.logg(*aktivitetskontekster.toTypedArray())
        }

        internal fun tidligerePerioderFerdigBehandlet(perioder: List<Vedtaksperiode>, vedtaksperiode: Vedtaksperiode) =
            perioder
                .filter { it < vedtaksperiode }
                .none { it.måFerdigstilles() }

        internal fun List<Vedtaksperiode>.medSkjæringstidspunkt(skjæringstidspunkt: LocalDate) =
            this.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        internal fun List<Vedtaksperiode>.harInntekt() =
            this.takeIf { it.isNotEmpty() }
                ?.any { it.arbeidsgiver.grunnlagForSykepengegrunnlag(it.skjæringstidspunkt, it.periode.start) != null } ?: true

        internal fun overlapperMedForkastet(forkastede: Iterable<Vedtaksperiode>, sykmelding: Sykmelding) {
            forkastede
                .filter { it.sykmeldingsperiode.overlapperMed(sykmelding.periode()) }
                .forEach {
                    sykmelding.error("Sykmelding overlapper med forkastet vedtaksperiode")
                    sykmelding.info("Sykmelding overlapper med forkastet vedtaksperiode ${it.id}, hendelse sykmeldingsperiode: ${sykmelding.periode()}, vedtaksperiode sykmeldingsperiode: ${it.periode}")
                }
        }

        internal fun overlapperMedForkastet(forkastede: Iterable<Vedtaksperiode>, inntektsmelding: Inntektsmelding) {
            forkastede
                .forEach {
                    if (it.sykmeldingsperiode.overlapperMed(inntektsmelding.periode()) &&
                        it.inntektsmeldingId != inntektsmelding.meldingsreferanseId() // Pga replay :(
                    )
                        inntektsmelding.trimLeft(it.periode.endInclusive)
                }
        }

        internal fun List<Vedtaksperiode>.håndter(inntektsmelding: InntektsmeldingReplay) {
            this.dropWhile { it.id != inntektsmelding.vedtaksperiodeId }
                .forEach { it.håndter(inntektsmelding) }
        }

        internal fun harForlengelseForAlleArbeidsgivereIInfotrygdhistorikken(
            vedtaksperioder: List<Vedtaksperiode>,
            historie: Historie,
            vedtaksperiode: Vedtaksperiode
        ) = historie.harForlengelseForAlleArbeidsgivereIInfotrygdhistorikken(
            vedtaksperioder.filter { it.periode.overlapperMed(vedtaksperiode.periode) }.map { it.organisasjonsnummer },
            vedtaksperiode.skjæringstidspunkt
        )

        internal fun gjentaHistorikk(hendelse: ArbeidstakerHendelse, person: Person) {
            val nåværende = person.nåværendeVedtaksperioder()
            val første = nåværende.firstOrNull() ?: return
            if (nåværende
                    .filter { første.periode.overlapperMed(it.periode) }
                    .all { it.tilstand == AvventerArbeidsgivere }
            ) {
                første.gjentaHistorikk(hendelse)
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
