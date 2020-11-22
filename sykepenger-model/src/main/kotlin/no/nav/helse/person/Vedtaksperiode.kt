package no.nav.helse.person

import no.nav.helse.Toggles.replayEnabled
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dødsinformasjon
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetaling
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.Arbeidsgiver.TilbakestillBehandling
import no.nav.helse.person.ForlengelseFraInfotrygd.JA
import no.nav.helse.person.Kildesystem.INFOTRYGD
import no.nav.helse.person.Kildesystem.SPLEIS
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Fagområde
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.*
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
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
    private var maksdato: LocalDate,
    private var gjenståendeSykedager: Int?,
    private var forbrukteSykedager: Int?,
    private var godkjentAv: String?,
    private var godkjenttidspunkt: LocalDateTime?,
    private var automatiskBehandling: Boolean?,
    private var skjæringstidspunktFraInfotrygd: LocalDate?,
    private var dataForVilkårsvurdering: Vilkårsgrunnlag.Grunnlagsdata?,
    private var dataForSimulering: Simulering.SimuleringResultat?,
    private val sykdomshistorikk: Sykdomshistorikk,
    private var sykdomstidslinje: Sykdomstidslinje,
    private val hendelseIder: MutableList<UUID>,
    private var inntektsmeldingId: UUID?,
    private var periode: Periode,
    private var sykmeldingsperiode: Periode,
    private var utbetaling: Utbetaling?,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
    private var personFagsystemId: String?,
    private var personNettoBeløp: Int,
    private var arbeidsgiverFagsystemId: String?,
    private var arbeidsgiverNettoBeløp: Int,
    private var forlengelseFraInfotrygd: ForlengelseFraInfotrygd = ForlengelseFraInfotrygd.IKKE_ETTERSPURT
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val skjæringstidspunkt
        get() =
            skjæringstidspunktFraInfotrygd
                ?: Historie(person).skjæringstidspunkt(periode)
                ?: periode.start

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
        maksdato = LocalDate.MAX,
        gjenståendeSykedager = null,
        forbrukteSykedager = null,
        godkjentAv = null,
        godkjenttidspunkt = null,
        automatiskBehandling = null,
        skjæringstidspunktFraInfotrygd = null,
        dataForVilkårsvurdering = null,
        dataForSimulering = null,
        sykdomshistorikk = Sykdomshistorikk(),
        sykdomstidslinje = Sykdomstidslinje(),
        hendelseIder = mutableListOf(),
        inntektsmeldingId = null,
        periode = Periode(LocalDate.MIN, LocalDate.MAX),
        sykmeldingsperiode = Periode(LocalDate.MIN, LocalDate.MAX),
        utbetaling = null,
        utbetalingstidslinje = Utbetalingstidslinje(),
        personFagsystemId = null,
        personNettoBeløp = 0,
        arbeidsgiverFagsystemId = null,
        arbeidsgiverNettoBeløp = 0
    )

    internal fun accept(visitor: VedtaksperiodeVisitor) {
        visitor.preVisitVedtaksperiode(
            this,
            id,
            arbeidsgiverNettoBeløp,
            personNettoBeløp,
            periode,
            sykmeldingsperiode,
            hendelseIder
        )
        sykdomstidslinje.accept(visitor)
        sykdomshistorikk.accept(visitor)
        utbetalingstidslinje.accept(visitor)
        visitor.visitTilstand(tilstand)
        visitor.visitMaksdato(maksdato)
        visitor.visitForlengelseFraInfotrygd(forlengelseFraInfotrygd)
        visitor.visitGjenståendeSykedager(gjenståendeSykedager)
        visitor.visitForbrukteSykedager(forbrukteSykedager)
        visitor.visitArbeidsgiverFagsystemId(arbeidsgiverFagsystemId)
        visitor.visitPersonFagsystemId(personFagsystemId)
        visitor.visitGodkjentAv(godkjentAv)
        visitor.visitSkjæringstidspunkt(skjæringstidspunkt)
        visitor.visitDataForVilkårsvurdering(dataForVilkårsvurdering)
        visitor.visitDataForSimulering(dataForSimulering)
        visitor.postVisitVedtaksperiode(this, id, arbeidsgiverNettoBeløp, personNettoBeløp, periode, sykmeldingsperiode)
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

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMedInntektsmelding(inntektsmelding).also {
        if (!it) {
            inntektsmelding.trimLeft(periode.endInclusive)
            return it
        }
        kontekst(inntektsmelding)
        tilstand.håndter(this, inntektsmelding)
    }

    internal fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        if (id.toString() != utbetalingshistorikk.vedtaksperiodeId) return
        kontekst(utbetalingshistorikk)
        tilstand.håndter(person, arbeidsgiver, this, utbetalingshistorikk)
    }

    internal fun håndter(ytelser: Ytelser) {
        if (id.toString() != ytelser.vedtaksperiodeId) return
        kontekst(ytelser)
        tilstand.håndter(person, arbeidsgiver, this, ytelser)
    }

    internal fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        if (id.toString() != utbetalingsgodkjenning.vedtaksperiodeId()) return
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

    internal fun håndter(utbetaling: UtbetalingOverført) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        kontekst(utbetaling)
        tilstand.håndter(this, utbetaling)
    }

    internal fun håndter(utbetaling: UtbetalingHendelse) {
        if (id.toString() != utbetaling.vedtaksperiodeId) return
        kontekst(utbetaling)
        tilstand.håndter(this, utbetaling)
    }

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (id.toString() != påminnelse.vedtaksperiodeId) return false
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

    internal fun håndter(hendelse: OverstyrTidslinje) = overlapperMed(hendelse).also {
        if (!it) return it
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
    }

    internal fun håndter(grunnbeløpsregulering: Grunnbeløpsregulering) {
        if (!grunnbeløpsregulering.erRelevant(arbeidsgiverFagsystemId, personFagsystemId, skjæringstidspunkt)) return
        kontekst(grunnbeløpsregulering)
        tilstand.håndter(this, grunnbeløpsregulering)
    }

    override fun compareTo(other: Vedtaksperiode) = this.periode.endInclusive.compareTo(other.periode.endInclusive)

    internal fun erSykeperiodeRettFør(other: Vedtaksperiode) =
        this.periode.erRettFør(other.periode) && !this.sykdomstidslinje.erSisteDagArbeidsdag()

    internal fun starterEtter(other: Vedtaksperiode) = this.sykmeldingsperiode.start > other.sykmeldingsperiode.start

    internal fun periodetype() = when {
        forlengelseFraInfotrygd == JA ->
            arbeidsgiver.finnSykeperiodeRettFør(this)?.run { INFOTRYGDFORLENGELSE }
                ?: OVERGANG_FRA_IT
        harForegåendeSomErBehandletOgUtbetalt(this) -> FORLENGELSE
        else -> FØRSTEGANGSBEHANDLING
    }

    internal fun ferdig(hendelse: ArbeidstakerHendelse, sendTilInfotrygd: Boolean) {
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        if (sendTilInfotrygd && skalBytteTilstandVedForkastelse()) tilstand(hendelse, TilInfotrygd)
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

    private fun skalBytteTilstandVedForkastelse() =
        this.tilstand !in listOf(
            TilInfotrygd,
            Avsluttet,
            AvsluttetUtenUtbetalingMedInntektsmelding,
            AvsluttetUtenUtbetaling,
            AvventerVilkårsprøvingArbeidsgiversøknad,
            UtbetalingFeilet
        )

    private fun skalForkastesVedOverlapp() =
        this.tilstand !in listOf(
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

    internal fun erFerdigBehandlet() =
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

    private fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Invaliderer vedtaksperiode: %s", this.id.toString())
        tilstand(hendelse, TilInfotrygd)
    }

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
        block()

        event.kontekst(tilstand)
        emitVedtaksperiodeEndret(tilstand, event.aktivitetslogg, person.aktivitetslogg.logg(this), previousState)
        tilstand.entering(this, event)
    }

    private fun håndter(hendelse: Inntektsmelding, nesteTilstand: Vedtaksperiodetilstand) {
        arbeidsgiver.addInntekt(hendelse, skjæringstidspunkt)
        arbeidsgiver.addInntektVol2(hendelse, skjæringstidspunkt)
        hendelse.padLeft(periode.start)
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        inntektsmeldingId = hendelse.meldingsreferanseId()

        val tilstøtende = arbeidsgiver.finnSykeperiodeRettFør(this)
        hendelse.førsteFraværsdag?.also {
            when {
                tilstøtende == null -> if (it != skjæringstidspunkt)
                    hendelse.warn("Første fraværsdag oppgitt i inntektsmeldingen er ulik den systemet har beregnet. Vurder hvilken inntektsmelding som skal legges til grunn, og utbetal kun hvis dagsatsen er korrekt i forhold til denne.")
                tilstøtende.skjæringstidspunkt == skjæringstidspunkt && skjæringstidspunkt != hendelse.førsteFraværsdag ->
                    hendelse.warn("Første fraværsdag i inntektsmeldingen er forskjellig fra foregående tilstøtende periode")
            }
        }
        hendelse.valider(periode)
        if (hendelse.hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand)
        hendelse.info("Fullført behandling av inntektsmelding")
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        sykdomshistorikk.håndter(hendelse)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
        hendelseIder.add(hendelse.meldingsreferanseId())
    }

    private fun håndter(hendelse: Sykmelding, nesteTilstand: () -> Vedtaksperiodetilstand) {
        periode = hendelse.periode()
        sykmeldingsperiode = hendelse.periode()
        oppdaterHistorikk(hendelse)
        if (hendelse.valider(periode).hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand())
    }

    private fun håndter(hendelse: Søknad, nesteTilstand: Vedtaksperiodetilstand) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (hendelse.valider(periode).hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand)
    }

    private fun håndter(hendelse: SøknadArbeidsgiver, nesteTilstand: Vedtaksperiodetilstand) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (hendelse.valider(periode).hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand)
    }

    private fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand) {
        vilkårsgrunnlag.lagreInntekter(person, skjæringstidspunkt)
        val beregnetInntekt = arbeidsgiver.inntekt(skjæringstidspunkt) ?: vilkårsgrunnlag.severe(
            "Finner ikke inntekt for perioden $skjæringstidspunkt"
        )
        if (vilkårsgrunnlag.valider(beregnetInntekt, skjæringstidspunkt, periodetype()).hasErrorsOrWorse().also {
                mottaVilkårsvurdering(vilkårsgrunnlag.grunnlagsdata())
            }) {
            vilkårsgrunnlag.info("Feil i vilkårsgrunnlag i %s", tilstand.type)
            return tilstand(vilkårsgrunnlag, TilInfotrygd)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag verifisert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
    }

    private fun mottaVilkårsvurdering(grunnlagsdata: Vilkårsgrunnlag.Grunnlagsdata) {
        dataForVilkårsvurdering = grunnlagsdata
        arbeidsgiver.finnSykeperiodeRettEtter(this)?.mottaVilkårsvurdering(grunnlagsdata)
    }

    private fun trengerYtelser(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(
            hendelse,
            Periode(arbeidsgiver.sykdomstidslinje().førsteDag().minusYears(4), periode.endInclusive)
        )
        foreldrepenger(hendelse)
        pleiepenger(hendelse, periode)
        omsorgspenger(hendelse, periode)
        opplæringspenger(hendelse, periode)
        institusjonsopphold(hendelse, periode)
    }

    private fun trengerPersoninfo(hendelse: ArbeidstakerHendelse) {
        dødsinformasjon(hendelse)
    }

    private fun trengerKortHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(hendelse, periode)
    }

    private fun trengerGapHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(hendelse, periode.start.minusYears(4) til periode.endInclusive)
    }

    private fun trengerVilkårsgrunnlag(hendelse: ArbeidstakerHendelse) {
        val beregningSlutt = YearMonth.from(skjæringstidspunkt).minusMonths(1)
        inntektsberegning(hendelse, beregningSlutt.minusMonths(11), beregningSlutt)
        opptjening(hendelse)
        dagpenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
        arbeidsavklaringspenger(hendelse, periode.start.minusMonths(6), periode.endInclusive)
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

    private fun trengerUtbetaling(hendelse: ArbeidstakerHendelse, epost: String) {
        utbetaling(
            aktivitetslogg = hendelse,
            oppdrag = utbetaling().arbeidsgiverOppdrag(),
            maksdato = maksdato,
            saksbehandler = godkjentAv!!,
            saksbehandlerEpost = epost,
            godkjenttidspunkt = godkjenttidspunkt!!,
            annullering = false
        )
    }

    private fun replayHendelser() {
        person.vedtaksperiodeReplay(
            PersonObserver.VedtaksperiodeReplayEvent(
                vedtaksperiodeId = id,
                aktørId = aktørId,
                fødselsnummer = fødselsnummer,
                organisasjonsnummer = organisasjonsnummer,
                hendelseIder = hendelseIder
            )
        )
    }

    private fun regulerGrunnbeløp(grunnbeløpsregulering: Grunnbeløpsregulering) {
        tilstand(grunnbeløpsregulering, AvventerHistorikk)
    }

    private fun emitVedtaksperiodeEndret(
        currentState: Vedtaksperiodetilstand,
        hendelseaktivitetslogg: Aktivitetslogg,
        vedtaksperiodeaktivitetslogg: Aktivitetslogg,
        previousState: Vedtaksperiodetilstand
    ) {
        val event = PersonObserver.VedtaksperiodeEndretTilstandEvent(
            vedtaksperiodeId = id,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            gjeldendeTilstand = currentState.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = hendelseaktivitetslogg,
            vedtaksperiodeaktivitetslogg = vedtaksperiodeaktivitetslogg,
            hendelser = hendelseIder,
            makstid = currentState.makstid(this, LocalDateTime.now())
        )

        person.vedtaksperiodeEndret(event)
    }

    private fun tickleForArbeidsgiveravhengighet(påminnelse: Påminnelse) {
        person.nåværendeVedtaksperioder()
            .first()
            .takeIf { it.tilstand == AvventerArbeidsgivere }
            ?.also { it.tilstand(påminnelse, AvventerHistorikk) }
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
        if (første.tilstand == AvventerArbeidsgivere) {
            this.tilstand(hendelse, AvventerArbeidsgivere)
            første.tilstand(hendelse, AvventerHistorikk)
        }
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
            høstingsresultater(engineForTimeline, hendelse)
        else tilstand(hendelse, AvventerArbeidsgivere)
    }

    //Hent resultat fra beregning (harvest results). Savner Fred 😢
    private fun høstingsresultater(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        utbetaling = arbeidsgiver.lagUtbetaling(hendelse, fødselsnummer, engineForTimeline, this.periode)
        maksdato = engineForTimeline.maksdato()
        gjenståendeSykedager = engineForTimeline.gjenståendeSykedager()
        forbrukteSykedager = engineForTimeline.forbrukteSykedager()
        personFagsystemId = arbeidsgiver.utbetaling()?.personOppdrag()?.fagsystemId()
        personNettoBeløp = arbeidsgiver.utbetaling()?.personOppdrag()?.nettoBeløp() ?: 0
        arbeidsgiverFagsystemId = arbeidsgiver.utbetaling()?.arbeidsgiverOppdrag()?.fagsystemId()
        arbeidsgiverNettoBeløp = arbeidsgiver.utbetaling()?.arbeidsgiverOppdrag()?.nettoBeløp() ?: 0
        utbetalingstidslinje = arbeidsgiver.nåværendeTidslinje().subset(periode)

        when {
            utbetalingstidslinje.kunArbeidsgiverdager() && !person.aktivitetslogg.logg(this).hasWarningsOrWorse() -> {
                tilstand(hendelse, AvsluttetUtenUtbetalingMedInntektsmelding) {
                    hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav og avluttes""")
                }
            }
            !utbetalingstidslinje.harUtbetalinger() -> {
                loggHvisForlengelse(hendelse)
                tilstand(hendelse, AvventerGodkjenning) {
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

    private fun loggHvisForlengelse(logg: IAktivitetslogg) {
        periodetype().also { periodetype ->
            if (periodetype != FØRSTEGANGSBEHANDLING) {
                logg.info("Perioden er en forlengelse, av type $periodetype")
            }
        }
    }

    private fun utbetaling() =
        arbeidsgiver.utbetaling() ?: throw IllegalStateException("mangler utbetalinger")

    private fun sendUtbetaltEvent() {
        val sykepengegrunnlag =
            requireNotNull(
                arbeidsgiver.sykepengegrunnlag(
                    skjæringstidspunkt,
                    periode.endInclusive
                )
            ) { "Forventet sykepengegrunnlag ved opprettelse av utbetalt-event" }
        val inntekt =
            requireNotNull(arbeidsgiver.inntekt(skjæringstidspunkt)) { "Forventet inntekt ved opprettelse av utbetalt-event" }
        person.vedtaksperiodeUtbetalt(
            tilUtbetaltEvent(
                aktørId = aktørId,
                fødselnummer = fødselsnummer,
                orgnummer = organisasjonsnummer,
                utbetaling = utbetaling(),
                utbetalingstidslinje = utbetalingstidslinje,
                sykepengegrunnlag = sykepengegrunnlag,
                inntekt = inntekt,
                forbrukteSykedager = requireNotNull(forbrukteSykedager),
                gjenståendeSykedager = requireNotNull(gjenståendeSykedager),
                godkjentAv = requireNotNull(godkjentAv),
                automatiskBehandling = requireNotNull(automatiskBehandling),
                hendelseIder = hendelseIder,
                periode = periode,
                maksdato = maksdato
            )
        )
    }

    internal fun gjentaHistorikk(hendelse: ArbeidstakerHendelse) {
        if (tilstand == AvventerArbeidsgivere) tilstand(hendelse, AvventerHistorikk)
    }

    internal fun validerSykdomstidslinje(arbeidsgiverSykdomstidslinje: Sykdomstidslinje) {
        if (sykdomshistorikk.sykdomstidslinje().toShortString() != arbeidsgiverSykdomstidslinje.subset(periode())
                .toShortString()
        ) {
            log.warn("Sykdomstidslinje på vedtaksperiode er ikke lik arbeidsgiver sin avgrensede sykdomstidslinje")
            sikkerLogg.warn(
                "Sykdomstidslinje på vedtaksperiode er ikke lik arbeidsgiver sin avgrensede sykdomstidslinje."
                    + "vedtaksperiodeId=$id, aktørId=$aktørId, fødselsnummer=$fødselsnummer, " +
                    "arbeidsgivertidslinje=[${arbeidsgiverSykdomstidslinje.subset(periode())}], vedtaksperiodetidslinje=[${sykdomshistorikk.sykdomstidslinje()}], " +
                    "periode=${periode()}"
            )
        }
    }

    private fun håndterMuligForlengelse(
        hendelse: ArbeidstakerHendelse,
        tilstandHvisForlengelse: Vedtaksperiodetilstand,
        tilstandHvisGap: Vedtaksperiodetilstand
    ) {
        arbeidsgiver.finnSykeperiodeRettFør(this)?.also { tilstøtende ->
            tilstand(hendelse, tilstandHvisForlengelse) {
                håndterFortsattForlengelse(tilstøtende)
            }
        } ?: tilstand(hendelse, tilstandHvisGap) { håndterGapVarForlengelse() }
    }

    // oppdaget gap, var forlengelse
    private fun håndterGapVarForlengelse() {
        dataForVilkårsvurdering = null
        tilbakestillInntektsmeldingId()
        håndterGap()
    }

    // oppdaget/bekreftet gap
    private fun håndterGap() {
        forlengelseFraInfotrygd = ForlengelseFraInfotrygd.NEI
        skjæringstidspunktFraInfotrygd = null
    }

    // er forlengelse
    private fun håndterForlengelse(tilstøtende: Vedtaksperiode) {
        dataForVilkårsvurdering = tilstøtende.dataForVilkårsvurdering
        inntektsmeldingId = tilstøtende.inntektsmeldingId?.also {
            hendelseIder.add(it)
        }
        forlengelseFraInfotrygd = tilstøtende.forlengelseFraInfotrygd
        skjæringstidspunktFraInfotrygd =
            if (forlengelseFraInfotrygd == JA) tilstøtende.skjæringstidspunktFraInfotrygd else null
    }

    // er fortsatt forlengelse
    private fun håndterFortsattForlengelse(tilstøtende: Vedtaksperiode) {
        tilbakestillInntektsmeldingId()
        håndterForlengelse(tilstøtende)
    }

    // oppdaget forlengelse fra IT, har ikke tilstøtende
    private fun håndterForlengelseIT(historie: Historie) {
        tilbakestillInntektsmeldingId()
        forlengelseFraInfotrygd = JA
        skjæringstidspunktFraInfotrygd = historie.skjæringstidspunkt(periode)
    }

    private fun tilbakestillInntektsmeldingId() {
        inntektsmeldingId?.also {
            hendelseIder.remove(it)
            inntektsmeldingId = null
        }
    }

    override fun toString() = "${this.periode.start} - ${this.periode.endInclusive}"
    fun tillatAnullering() =
        this.tilstand != TilUtbetaling

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

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.warn("Avslutter perioden på grunn av tilbakestilling")
            vedtaksperiode.tilstand(hendelse, TilInfotrygd)
        }

        fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(35)

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
            søknad.trimLeft(vedtaksperiode.periode.endInclusive)
            søknad.error("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
            if (!vedtaksperiode.skalForkastesVedOverlapp()) return
            vedtaksperiode.tilstand(søknad, TilInfotrygd)
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

        fun håndter(vedtaksperiode: Vedtaksperiode, grunnbeløpsregulering: Grunnbeløpsregulering) {
        }

        fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
            if (utbetalingshistorikk.valider(vedtaksperiode.periode, vedtaksperiode.periodetype()).hasErrorsOrWorse())
                return vedtaksperiode.tilstand(utbetalingshistorikk, TilInfotrygd)
            utbetalingshistorikk.info("Utbetalingshistorikk sjekket; fant ingen feil.")
        }

        fun håndter(person: Person, arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode, ytelser: Ytelser) {
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

        fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingOverført) {
            utbetaling.error("Forventet ikke utbetaling overført i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingHendelse) {
            utbetaling.error("Forventet ikke utbetaling i %s", type.name)
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

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {}

        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
    }

    internal object Start : Vedtaksperiodetilstand {
        override val type = START

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            var replays: List<Vedtaksperiode> = emptyList()

            vedtaksperiode.håndter(sykmelding) returnPoint@{
                if (replayEnabled) {
                    if (!vedtaksperiode.arbeidsgiver.støtterReplayFor(vedtaksperiode)) {
                        return@returnPoint TilInfotrygd
                    }
                    replays = vedtaksperiode.arbeidsgiver.søppelbøtte(
                        sykmelding,
                        Arbeidsgiver.SENERE_EXCLUSIVE(vedtaksperiode),
                        false
                    )
                } else if (vedtaksperiode.arbeidsgiver.harPeriodeEtter(vedtaksperiode)) {
                    return@returnPoint TilInfotrygd
                }

                val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.also {
                    vedtaksperiode.håndterForlengelse(it)
                }
                val forlengelse = periodeRettFør != null
                val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode) && periodeRettFør?.let { it.tilstand != AvsluttetUtenUtbetaling } ?: true
                when {
                    forlengelse && ferdig -> MottattSykmeldingFerdigForlengelse
                    forlengelse && !ferdig -> MottattSykmeldingUferdigForlengelse
                    !forlengelse && ferdig -> MottattSykmeldingFerdigGap
                    !forlengelse && !ferdig -> MottattSykmeldingUferdigGap
                    else -> sykmelding.severe("Klarer ikke bestemme hvilken sykmeldingmottattilstand vi skal til")
                }
            }
            sykmelding.info("Fullført behandling av sykmelding")
            if (replayEnabled) {
                replays.forEach { periode ->
                    periode.replayHendelser()
                }
            }
        }
    }

    internal object MottattSykmeldingFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object MottattSykmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerInntektsmeldingUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            gjenopptaBehandling: GjenopptaBehandling
        ) {
            vedtaksperiode.håndterMuligForlengelse(
                gjenopptaBehandling,
                MottattSykmeldingFerdigForlengelse,
                MottattSykmeldingFerdigGap
            )
        }
    }

    internal object MottattSykmeldingFerdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_FERDIG_GAP

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
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
            vedtaksperiode.håndter(søknad, AvsluttetUtenUtbetaling)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndter(inntektsmelding, AvventerSøknadUferdigGap)
        }
    }

    internal object AvventerSøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (søknad.sykdomstidslinje().starterFør(vedtaksperiode.sykdomstidslinje)) {
                søknad.warn("Søknaden inneholder egenmeldingsdager som ikke er oppgitt i inntektsmeldingen. Vurder om arbeidsgiverperioden beregnes riktig")
                søknad.trimLeft(vedtaksperiode.sykdomstidslinje.førsteDag())
            }
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            vedtaksperiode.håndter(søknad, AvventerVilkårsprøvingArbeidsgiversøknad)
            søknad.info("Fullført behandling av søknad til arbeidsgiver")
        }
    }

    internal object AvventerGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerGapHistorikkFraInfotrygd(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerGapHistorikkFraInfotrygd(påminnelse)
            påminnelse.info("Forespør sykdoms- og inntektshistorikk (Påminnet)")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
            val historie = Historie(person, utbetalingshistorikk)
            validation(utbetalingshistorikk) {
                onError {
                    vedtaksperiode.tilstand(utbetalingshistorikk, TilInfotrygd)
                }
                validerUtbetalingshistorikk(
                    historie.avgrensetPeriode(
                        vedtaksperiode.organisasjonsnummer,
                        vedtaksperiode.periode
                    ), utbetalingshistorikk, vedtaksperiode.periodetype()
                )
                lateinit var nesteTilstand: Vedtaksperiodetilstand
                onSuccess {
                    nesteTilstand =
                        if (historie.erForlengelse(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)) {
                            utbetalingshistorikk.info("Oppdaget at perioden er en forlengelse")
                            AvventerHistorikk
                        } else {
                            AvventerInntektsmeldingFerdigGap
                        }
                }
                onSuccess {
                    vedtaksperiode.tilstand(utbetalingshistorikk, nesteTilstand)
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
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerGap)
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
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerVilkårsprøvingGap)
        }
    }

    internal object AvventerInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(
                gjenopptaBehandling,
                AvventerHistorikk,
                AvventerInntektsmeldingFerdigGap
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigForlengelse)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding()
        }
    }

    internal object AvventerUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(gjenopptaBehandling, AvventerHistorikk, AvventerVilkårsprøvingGap)
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(
                gjenopptaBehandling,
                MottattSykmeldingFerdigForlengelse,
                AvventerSøknadFerdigGap
            )
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

    internal object AvventerInntektsmeldingFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_FERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding()
        }

    }

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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vedtaksperiode.håndter(vilkårsgrunnlag, AvventerHistorikk)
        }
    }

    internal object AvventerHistorikk : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.trengerPersoninfo(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
            vedtaksperiode.trengerPersoninfo(påminnelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            val historie = Historie(person, ytelser.utbetalingshistorikk())
            validation(ytelser) {
                onError { vedtaksperiode.tilstand(ytelser, TilInfotrygd) }
                validerYtelser(
                    historie.avgrensetPeriode(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode),
                    ytelser,
                    vedtaksperiode.periodetype()
                )
                onSuccess { ytelser.addInntekter(person) }
                overlappende(vedtaksperiode.periode, ytelser.foreldrepenger())
                overlappende(vedtaksperiode.periode, ytelser.pleiepenger())
                overlappende(vedtaksperiode.periode, ytelser.omsorgspenger())
                overlappende(vedtaksperiode.periode, ytelser.opplæringspenger())
                overlappende(vedtaksperiode.periode, ytelser.institusjonsopphold())
                onSuccess {
                    val tilstøtende = arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
                    val periodetype = historie.periodetype(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)
                    val opphav = if (periodetype.opphav() == SPLEIS) "ny løsning" else "Infotrygd"

                    if (historie.erPingPong(vedtaksperiode.organisasjonsnummer, vedtaksperiode.periode)) {
                        ytelser.warn("Perioden forlenger en behandling i $opphav, og har historikk i ${if (periodetype.opphav() == SPLEIS) "Infotrygd" else "ny løsning"} også: Undersøk at antall dager igjen er beregnet riktig.")
                    }

                    when {
                        periodetype == FØRSTEGANGSBEHANDLING -> {
                            vedtaksperiode.håndterGap()
                            ytelser.info("Perioden er en førstegangsbehandling")
                        }
                        periodetype == OVERGANG_FRA_IT -> {
                            vedtaksperiode.håndterForlengelseIT(historie)
                            arbeidsgiver.addInntekt(ytelser)
                            ytelser.info("Perioden er en direkte overgang fra periode med opphav i Infotrygd")
                            if (ytelser.statslønn())
                                ytelser.warn("Det er lagt inn statslønn i Infotrygd, undersøk at utbetalingen blir riktig.")
                        }
                        // vi er en forlengelse av et slag, men har ingen tilstøtende (Infotrygd-periode foran)
                        tilstøtende == null -> {
                            vedtaksperiode.håndterForlengelseIT(historie)
                            ytelser.info("Perioden er en forlengelse av en periode med opphav i $opphav, dog uten tilstøtende")
                        }
                        else -> {
                            ytelser.info("Perioden er en forlengelse av en periode med opphav i $opphav")
                            vedtaksperiode.håndterFortsattForlengelse(tilstøtende)
                        }
                    }
                }
                harInntektshistorikk(arbeidsgiver, vedtaksperiode.periode.start)
                lateinit var engineForTimeline: ArbeidsgiverUtbetalinger
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    engineForTimeline = ArbeidsgiverUtbetalinger(
                        tidslinjer = person.utbetalingstidslinjer(vedtaksperiode.periode, historie, ytelser),
                        personTidslinje = historie.utbetalingstidslinje(vedtaksperiode.periode),
                        periode = vedtaksperiode.periode,
                        skjæringstidspunkter = historie.skjæringstidspunkter(vedtaksperiode.periode),
                        alder = Alder(vedtaksperiode.fødselsnummer),
                        arbeidsgiverRegler = NormalArbeidstaker,
                        aktivitetslogg = ytelser.aktivitetslogg,
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        fødselsnummer = vedtaksperiode.fødselsnummer,
                        dødsdato = ytelser.dødsinfo().dødsdato
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (simulering.valider(vedtaksperiode.utbetaling().arbeidsgiverOppdrag().utenUendretLinjer()).hasErrorsOrWorse()) {
                return vedtaksperiode.tilstand(simulering, TilInfotrygd)
            }
            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            simulering(
                aktivitetslogg = hendelse,
                oppdrag = Fagområde.SykepengerRefusjon.utbetalingslinjer(vedtaksperiode.utbetaling()).utenUendretLinjer(),
                maksdato = vedtaksperiode.maksdato,
                saksbehandler = "Spleis"
            )
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt
                .plusDays(4)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            trengerGodkjenning(hendelse, vedtaksperiode)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingsgodkjenning: Utbetalingsgodkjenning
        ) {
            if (utbetalingsgodkjenning.valider().hasErrorsOrWorse()) return vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                TilInfotrygd
            )

            vedtaksperiode.tilstand(
                utbetalingsgodkjenning,
                if (!vedtaksperiode.utbetalingstidslinje.harUtbetalinger()) Avsluttet else TilUtbetaling
            ) {
                vedtaksperiode.godkjenttidspunkt = utbetalingsgodkjenning.godkjenttidspunkt()
                vedtaksperiode.godkjentAv = utbetalingsgodkjenning.saksbehandler()
                vedtaksperiode.automatiskBehandling = utbetalingsgodkjenning.automatiskBehandling().also {
                    if (it) {
                        utbetalingsgodkjenning.info("Utbetaling markert som godkjent automatisk ${vedtaksperiode.godkjenttidspunkt}")
                    } else {
                        utbetalingsgodkjenning.info("Utbetaling markert som godkjent av saksbehandler ${vedtaksperiode.godkjentAv} ${vedtaksperiode.godkjenttidspunkt}")
                    }
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerGodkjenning(påminnelse, vedtaksperiode)
        }

        private fun trengerGodkjenning(
            hendelse: ArbeidstakerHendelse,
            vedtaksperiode: Vedtaksperiode
        ) {
            godkjenning(
                aktivitetslogg = hendelse,
                periodeFom = vedtaksperiode.periode.start,
                periodeTom = vedtaksperiode.periode.endInclusive,
                sykepengegrunnlag = vedtaksperiode.arbeidsgiver.inntekt(vedtaksperiode.periode.start)!!,
                vedtaksperiodeaktivitetslogg = vedtaksperiode.person.aktivitetslogg.logg(vedtaksperiode),
                periodetype = vedtaksperiode.periodetype()
            )
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.info("Tilbakestiller ikke en vedtaksperiode som har gått til utbetaling")
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            tilstandsendringstidspunkt.plusDays(7)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            val epost = if (hendelse is Utbetalingsgodkjenning) hendelse.saksbehandlerEpost() else "tbd@nav.no"
            vedtaksperiode.trengerUtbetaling(hendelse, epost)
        }

        override fun håndterMakstid(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, UtbetalingFeilet) {
                påminnelse.error("Gir opp å forsøke utbetaling")
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingOverført) {
            utbetaling.info(
                "Utbetalingen ble overført til Oppdrag/UR ${utbetaling.overføringstidspunkt}, " +
                    "og har fått avstemmingsnøkkel ${utbetaling.avstemmingsnøkkel}"
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som har gått til utbetaling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingHendelse) {
            utbetaling.valider()
            if (utbetaling.skalForsøkesIgjen()) return utbetaling.warn("Utbetalingen er ikke gjennomført. Prøver automatisk igjen senere")
            vedtaksperiode.utbetaling().håndter(utbetaling)

            if (utbetaling.hasErrorsOrWorse()) return vedtaksperiode.tilstand(utbetaling, UtbetalingFeilet) {
                utbetaling.error("Utbetaling ble ikke gjennomført")
            }

            vedtaksperiode.tilstand(utbetaling, Avsluttet) {
                utbetaling.info("OK fra Oppdragssystemet")
            }
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerUtbetaling(påminnelse, "epost@nav.no")
        }
    }

    internal object TilAnnullering : Vedtaksperiodetilstand {
        override val type = TIL_ANNULLERING

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.info("Tilbakestiller ikke en vedtaksperiode som har gått til annullering")
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingOverført) {
            utbetaling.info(
                "Annulleringen ble overført til Oppdrag/UR ${utbetaling.overføringstidspunkt}, " +
                    "og har fått avstemmingsnøkkel ${utbetaling.avstemmingsnøkkel}"
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som har gått til annullering")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingHendelse) {
            if (utbetaling.valider().hasErrorsOrWorse()) {
                utbetaling.warn("Annullering ble ikke gjennomført")
            } else {
                vedtaksperiode.arbeidsgiver.annullerUtbetaling(
                    utbetaling,
                    vedtaksperiode.arbeidsgiverFagsystemId!!,
                    utbetaling.godkjenttidspunkt,
                    saksbehandlerEpost = utbetaling.saksbehandlerEpost
                )
                utbetaling.info("Behandler annullering for vedtaksperiode: %s", vedtaksperiode.id.toString())
                vedtaksperiode.arbeidsgiver.søppelbøtte(utbetaling, Arbeidsgiver.ALLE)
                vedtaksperiode.invaliderPeriode(utbetaling)
            }

        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object UtbetalingFeilet : Vedtaksperiodetilstand {
        override val type = UTBETALING_FEILET

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.error("Feilrespons fra oppdrag")
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tilstand(påminnelse, TilUtbetaling) {
                påminnelse.info("Forsøker utbetaling på nytt")
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode med utbetaling som har feilet")
        }
    }

    internal object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(
                inntektsmelding,
                if (inntektsmelding.inntektenGjelderFor(vedtaksperiode.periode) && vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(
                        vedtaksperiode
                    ) == null
                ) {
                    AvventerVilkårsprøvingArbeidsgiversøknad
                } else
                    AvsluttetUtenUtbetalingMedInntektsmelding
            )
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet uten utbetaling")
        }
    }

    internal object AvsluttetUtenUtbetalingMedInntektsmelding : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING_MED_INNTEKTSMELDING

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.info("Tilbakestiller ikke en vedtaksperiode som er avsluttet med inntektsmelding")
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(hendelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet med inntektsmelding")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.info("Tilbakestiller ikke en vedtaksperiode som har gått til utbetalinger")
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.sykmeldingsperiode)
            vedtaksperiode.sendUtbetaltEvent()
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, grunnbeløpsregulering: Grunnbeløpsregulering) {
            if (grunnbeløpsregulering.håndtert()) return grunnbeløpsregulering.info("Grunnbeløpsreguleringen er håndtert av en annen periode")
            grunnbeløpsregulering.info("Foretar grunnbeløpsregulering")
            vedtaksperiode.regulerGrunnbeløp(grunnbeløpsregulering)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.severe("Overstyrer ikke en vedtaksperiode som er avsluttet")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, utbetaling: UtbetalingHendelse) {
            if (utbetaling.annullert) {
                vedtaksperiode.tilstand(utbetaling, TilAnnullering)
            }
        }
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            vedtaksperiode.arbeidsgiver.søppelbøtte(
                hendelse,
                vedtaksperiode.arbeidsgiver.tidligereOgEttergølgende2(vedtaksperiode)
            )
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            utbetalingshistorikk: Utbetalingshistorikk
        ) {
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er sendt til Infotrygd")
        }
    }

    internal companion object {
        private val log = LoggerFactory.getLogger(Vedtaksperiode::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")

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
    }
}

enum class ForlengelseFraInfotrygd {
    IKKE_ETTERSPURT,
    JA,
    NEI
}

enum class Kildesystem { SPLEIS, INFOTRYGD }
enum class Periodetype(private val kildesystem: Kildesystem) {
    /** Perioden er første periode i et sykdomstilfelle */
    FØRSTEGANGSBEHANDLING(SPLEIS),

    /** Perioden en en forlengelse av en Spleis-periode */
    FORLENGELSE(SPLEIS),

    /** Perioden en en umiddelbar forlengelse av en periode som er utbetalt i Infotrygd */
    OVERGANG_FRA_IT(INFOTRYGD),

    /** Perioden er en direkte eller indirekte forlengelse av en OVERGANG_FRA_IT-periode */
    INFOTRYGDFORLENGELSE(INFOTRYGD);

    fun opphav() = kildesystem
}
