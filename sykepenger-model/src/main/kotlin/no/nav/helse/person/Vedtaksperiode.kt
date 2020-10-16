package no.nav.helse.person

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Toggles
import no.nav.helse.Toggles.replayEnabled
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.arbeidsavklaringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.dagpenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.foreldrepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.godkjenning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.inntektsberegning
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.institusjonsopphold
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.medlemskap
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.omsorgspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opplæringspenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.opptjening
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.pleiepenger
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.sendUtbetalingsbehov
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.simulering
import no.nav.helse.person.Aktivitetslogg.Aktivitet.Behov.Companion.utbetalingshistorikk
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.Arbeidsgiver.TilbakestillBehandling
import no.nav.helse.person.ForlengelseFraInfotrygd.JA
import no.nav.helse.person.TilstandType.*
import no.nav.helse.sykdomstidslinje.Sykdomshistorikk
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Fagområde
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
    private var periode: Periode,
    private var sykmeldingsperiode: Periode,
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
                ?: person.skjæringstidspunkt(periode.endInclusive)
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
        periode = Periode(LocalDate.MIN, LocalDate.MAX),
        sykmeldingsperiode = Periode(LocalDate.MIN, LocalDate.MAX),
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
        if (this.skalForkastesVedOverlapp()) {
            valider(sykmelding) { tilstand.håndter(this, sykmelding) }
        } else {
            tilstand.håndter(this, sykmelding)
        }
    }

    internal fun håndter(søknad: SøknadArbeidsgiver) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        if (this.skalForkastesVedOverlapp()) {
            valider(søknad) { tilstand.håndter(this, søknad) }
        } else {
            tilstand.håndter(this, søknad)
        }
    }

    internal fun håndter(søknad: Søknad) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        if (this.skalForkastesVedOverlapp()) {
            valider(søknad) { tilstand.håndter(this, søknad) }
        } else {
            tilstand.håndter(this, søknad)
        }
    }

    internal fun håndter(inntektsmelding: Inntektsmelding) = overlapperMed(inntektsmelding).also {
        if (!it) return it
        kontekst(inntektsmelding)
        if (Toggles.mottattInntektsmeldingEventEnabled) mottattInntektsmelding()
        if (this.skalForkastesVedOverlapp()) {
            valider(inntektsmelding) { tilstand.håndter(this, inntektsmelding) }
        } else {
            tilstand.håndter(this, inntektsmelding)
        }
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
        if (!påminnelse.gjelderTilstand(tilstand.type)) return true
        kontekst(påminnelse)
        person.vedtaksperiodePåminnet(påminnelse)
        if (LocalDateTime.now() >= tilstand.makstid(this, påminnelse.tilstandsendringstidspunkt())) {
            påminnelse.kontekst(person)
            påminnelse.error("Gir opp fordi tilstanden er nådd makstid")
            tilstand(påminnelse, TilInfotrygd)
        } else {
            tilstand.håndter(this, påminnelse)
        }
        return true
    }

    internal fun håndter(other: Vedtaksperiode, hendelse: GjenopptaBehandling) {
        val ferdig = arbeidsgiver.tidligerePerioderFerdigBehandlet(this)
        if (this.periode.start > other.periode.start && ferdig) {
            kontekst(hendelse.hendelse)
            tilstand.håndter(this, hendelse)
        }
    }

    internal fun håndter(hendelse: GjenopptaBehandling) {
        kontekst(hendelse.hendelse)
        tilstand.håndter(this, hendelse)
    }

    internal fun håndter(hendelse: OverstyrTidslinje) = overlapperMed(hendelse).also {
        if (!it) return it
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
    }

    fun håndter(annullering: Annullering) {
        if (this.periode.endInclusive < annullering.fom) {
            return
        }
        tilstand.håndter(this, annullering)
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
            arbeidsgiver.finnSykeperiodeRettFør(this)?.run { Periodetype.INFOTRYGDFORLENGELSE }
                ?: Periodetype.OVERGANG_FRA_IT
        harForegåendeSomErBehandletOgUtbetalt(this) -> Periodetype.FORLENGELSE
        else -> Periodetype.FØRSTEGANGSBEHANDLING
    }

    internal fun ferdig(hendelse: PersonHendelse, sendTilInfotrygd: Boolean) {
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

    internal fun skalGjenopptaBehandling() =
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

    internal fun erAvsluttet() =
        this.tilstand in listOf(
            Avsluttet,
            AvsluttetUtenUtbetalingMedInntektsmelding,
            AvsluttetUtenUtbetaling,
            AvventerVilkårsprøvingArbeidsgiversøknad,
            UtbetalingFeilet
        )

    internal fun foregåendeSomErBehandletUtenUtbetaling() =
        arbeidsgiver.finnSykeperiodeRettFør(this)?.takeIf {
            it.tilstand in listOf(AvsluttetUtenUtbetaling, AvsluttetUtenUtbetalingMedInntektsmelding)
        }?.id

    private fun harForegåendeSomErBehandletOgUtbetalt(vedtaksperiode: Vedtaksperiode) =
        arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.let {
            it.tilstand == Avsluttet && it.utbetalingstidslinje.harUtbetalinger()
        } == true

    private fun invaliderPeriode(hendelse: ArbeidstakerHendelse) {
        hendelse.info("Invaliderer vedtaksperiode: %s", this.id.toString())
        tilstand(hendelse, TilInfotrygd)
    }

    internal fun periode() = periode
    internal fun opprinneligPeriode() = sykmeldingsperiode

    private fun valider(hendelse: SykdomstidslinjeHendelse, block: () -> Unit) {
        if (hendelse.valider(periode).hasErrorsOrWorse())
            return tilstand(hendelse, TilInfotrygd)
        block()
        if (hendelse.hasErrorsOrWorse())
            return tilstand(hendelse, TilInfotrygd)
    }

    private fun kontekst(hendelse: PersonHendelse) {
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) = hendelse.erRelevant(this.sykmeldingsperiode)

    private fun tilstand(
        event: PersonHendelse,
        nyTilstand: Vedtaksperiodetilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) return  // Already in this state => ignore
        tilstand.leaving(event)

        val previousState = tilstand

        tilstand = nyTilstand
        block()

        event.kontekst(tilstand)
        emitVedtaksperiodeEndret(tilstand, event.aktivitetslogg, person.aktivitetslogg.logg(this), previousState)
        tilstand.entering(this, event)
    }

    private fun håndter(hendelse: Inntektsmelding, nesteTilstand: Vedtaksperiodetilstand) {
        arbeidsgiver.addInntekt(hendelse)
        hendelse.padLeft(periode.start)
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)

        val tilstøtende = arbeidsgiver.finnSykeperiodeRettFør(this)
        hendelse.førsteFraværsdag?.also {
            when {
                tilstøtende == null -> if (it != skjæringstidspunkt)
                    hendelse.warn("Første fraværsdag oppgitt i inntektsmeldingen er ulik den systemet har beregnet. Utbetal kun hvis dagsatsen er korrekt")
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
        if (hendelse.hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
        tilstand(hendelse, nesteTilstand())
    }

    private fun håndter(hendelse: Søknad, nesteTilstand: Vedtaksperiodetilstand) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (hendelse.hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
            .also { trengerInntektsmelding() }
        tilstand(hendelse, nesteTilstand)
    }

    private fun håndter(hendelse: SøknadArbeidsgiver, nesteTilstand: Vedtaksperiodetilstand) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (hendelse.hasErrorsOrWorse()) return tilstand(hendelse, TilInfotrygd)
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

    private fun trengerYtelser(hendelse: PersonHendelse) {
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

    private fun trengerKortHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse) {
        utbetalingshistorikk(hendelse, periode)
    }

    private fun trengerVilkårsgrunnlag(hendelse: PersonHendelse) {
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

    private fun mottattInntektsmelding() {
        this.person.mottattInntektsmelding(
            PersonObserver.MottattInntektsmeldingEvent(
                vedtaksperiodeId = this.id,
                organisasjonsnummer = this.organisasjonsnummer,
                fødselsnummer = this.fødselsnummer,
                fom = this.periode.start,
                tom = this.periode.endInclusive
            )
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

    private fun tickleForArbeidsgiveravhengighet(
        påminnelse: Påminnelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder()
        vedtaksperioder.removeAt(0).also {
            if (it.tilstand == AvventerArbeidsgivere) it.tilstand(påminnelse, AvventerHistorikk)
        }
    }

    /**
     * Skedulering av utbetaling opp mot andre arbeidsgivere
     */
    private fun forsøkUtbetaling(
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder()
        vedtaksperioder.removeAt(0).also {
            if (it == this) return it.forsøkUtbetalingSteg2(vedtaksperioder, engineForTimeline, hendelse)
            if (it.tilstand == AvventerArbeidsgivere) {
                this.tilstand(hendelse, AvventerArbeidsgivere)
                it.tilstand(hendelse, AvventerHistorikk)
            }
        }
    }

    private fun forsøkUtbetalingSteg2(
        vedtaksperioder: MutableList<Vedtaksperiode>,
        engineForTimeline: MaksimumSykepengedagerfilter,
        hendelse: ArbeidstakerHendelse
    ) {
        if (vedtaksperioder
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
        engineForTimeline.beregnGrenser(this.periode.endInclusive)
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
            if (periodetype != Periodetype.FØRSTEGANGSBEHANDLING) {
                logg.info("Perioden er en forlengelse, av type $periodetype")
            }
        }
    }

    private fun utbetaling() =
        arbeidsgiver.utbetaling() ?: throw IllegalStateException("mangler utbetalinger")

    private fun sendUtbetaltEvent() {
        val sykepengegrunnlag =
            requireNotNull(arbeidsgiver.sykepengegrunnlag(skjæringstidspunkt, periode.endInclusive)) { "Forventet sykepengegrunnlag ved opprettelse av utbetalt-event" }
        person.vedtaksperiodeUtbetalt(
            tilUtbetaltEvent(
                aktørId = aktørId,
                fødselnummer = fødselsnummer,
                orgnummer = organisasjonsnummer,
                utbetaling = utbetaling(),
                utbetalingstidslinje = utbetalingstidslinje,
                sykepengegrunnlag = sykepengegrunnlag,
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

    internal fun gjentaHistorikk(hendelse: PersonHendelse) {
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

    override fun toString() = "${this.periode.start} - ${this.periode.endInclusive}"
    fun tillatAnullering() =
        this.tilstand != TilUtbetaling


    // log occurence for quantitative data for analysis
    private fun loggUlikSkjæringstidspunkt(utbetalingshistorikk: Utbetalingshistorikk) {
        if (forlengelseFraInfotrygd != JA) return
        val beregnetSkjæringstidspunkt = person.skjæringstidspunkt(periode.endInclusive, utbetalingshistorikk)
        if (skjæringstidspunkt == beregnetSkjæringstidspunkt) return
        log.info(
            "skjæringstidspunktet fra Infotrygd ($skjæringstidspunkt) er ulik beregnet skjæringstidspunkt ($beregnetSkjæringstidspunkt) for {}, {}",
            keyValue("vedtaksperiode_id", id),
            keyValue("periodetype", periodetype())
        )
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
            .plusDays(30)

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand", mapOf(
                    "tilstand" to type.name
                )
            )
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.error("Mottatt overlappende sykmeldinger - det støttes ikke før replay av hendelser er på plass")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            søknad.trimLeft(vedtaksperiode.periode.endInclusive)
            søknad.error("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, søknad: SøknadArbeidsgiver) {
            søknad.trimLeft(vedtaksperiode.periode.endInclusive)
            søknad.error("Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
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
            gjenopptaBehandling.hendelse.info("Tidligere periode ferdig behandlet")
        }

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            hendelse: OverstyrTidslinje
        ) {
            hendelse.error("Forventet ikke overstyring fra saksbehandler i %s", type.name)
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, annullering: Annullering) {
            vedtaksperiode.kontekst(annullering)
            vedtaksperiode.tilstand(annullering, TilInfotrygd)
        }

        fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {}

        fun leaving(aktivitetslogg: IAktivitetslogg) {}
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
                    /*replays = vedtaksperiode.arbeidsgiver.søppelbøtte(
                        vedtaksperiode,
                        sykmelding,
                        Arbeidsgiver.SENERE_EXCLUSIVE,
                        false
                    )*/
                } else {
                    if (vedtaksperiode.arbeidsgiver.harPeriodeEtter(vedtaksperiode)) {
                        vedtaksperiode.arbeidsgiver.søppelbøtte(sykmelding, Arbeidsgiver.SENERE(vedtaksperiode))
                        //vedtaksperiode.arbeidsgiver.søppelbøtte(vedtaksperiode, sykmelding, Arbeidsgiver.SENERE)
                        return@returnPoint TilInfotrygd
                    }
                }

                if (vedtaksperiode.arbeidsgiver.harTilstøtendeForkastet(vedtaksperiode)) {
                    vedtaksperiode.arbeidsgiver.søppelbøtte(sykmelding, Arbeidsgiver.SENERE(vedtaksperiode))
                    return@returnPoint TilInfotrygd
                }

                val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
                val forlengelse = periodeRettFør != null
                val ferdig = vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)
                if (periodeRettFør != null) {
                    vedtaksperiode.dataForVilkårsvurdering = periodeRettFør.dataForVilkårsvurdering
                }
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
            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val erFortsattForlengelse = periodeRettFør != null
            if (erFortsattForlengelse) {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigForlengelse)
            } else {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigGap)
            }
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
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigGap)
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

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            if (søknad.sykdomstidslinje().starterFør(vedtaksperiode.sykdomstidslinje)) {
                søknad.warn("Søknaden inneholder egenmeldingsdager som ikke er oppgitt i inntektsmeldingen")
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
            påminnelse.info("Forespør sykdoms- og inntektshistorikk (Påminnet)")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            validation(ytelser) {
                onError {
                    vedtaksperiode.tilstand(ytelser, TilInfotrygd)
                        .also { vedtaksperiode.trengerInntektsmelding() }
                }
                validerYtelser(vedtaksperiode.periode, ytelser, vedtaksperiode.periodetype())
                lateinit var nesteTilstand: Vedtaksperiodetilstand
                onSuccess {
                    arbeidsgiver.addInntekt(ytelser)
                    Oldtidsutbetalinger().also { oldtid ->
                        ytelser.utbetalingshistorikk().append(oldtid)
                        arbeidsgiver.utbetalteUtbetalinger()
                            .forEach { it.append(arbeidsgiver.organisasjonsnummer(), oldtid) }
                        nesteTilstand = if (oldtid.utbetalingerInkludert(arbeidsgiver).erRettFør(vedtaksperiode.periode)) {
                            ytelser.info("Oppdaget at perioden er en direkte overgang fra periode i Infotrygd")
                            AvventerHistorikk
                        } else {
                            AvventerInntektsmeldingFerdigGap
                        }
                    }
                }
                onSuccess {
                    vedtaksperiode.tilstand(ytelser, nesteTilstand)
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

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }
    }

    internal object AvventerUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_GAP

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerVilkårsprøvingGap)
        }
    }

    internal object AvventerInntektsmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_FORLENGELSE

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val erFortsattForlengelse = periodeRettFør != null
            if (erFortsattForlengelse) {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerHistorikk)
            } else {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerInntektsmeldingFerdigGap)
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerUferdigForlengelse)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }
    }

    internal object AvventerUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG_FORLENGELSE

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {

            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val erFortsattForlengelse = periodeRettFør != null
            if (erFortsattForlengelse) {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerHistorikk)
            } else {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerVilkårsprøvingGap)
            }
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            val periodeRettFør = vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)
            val erFortsattForlengelse = periodeRettFør != null
            if (erFortsattForlengelse) {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, MottattSykmeldingFerdigForlengelse)
            } else {
                vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerSøknadFerdigGap)
            }
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
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

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndter(søknad, AvventerUferdigGap)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling.hendelse, AvventerSøknadFerdigGap)
        }
    }

    internal object AvventerInntektsmeldingFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_FERDIG_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusDays(15)

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(inntektsmelding, AvventerVilkårsprøvingGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.trengerInntektsmelding()
        }

    }

    internal object AvventerVilkårsprøvingGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_GAP

        override fun makstid(
            vedtaksperiode: Vedtaksperiode,
            tilstandsendringstidspunkt: LocalDateTime
        ): LocalDateTime = tilstandsendringstidspunkt
            .plusHours(24)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.trengerYtelser(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerYtelser(påminnelse)
        }

        override fun håndter(
            person: Person,
            arbeidsgiver: Arbeidsgiver,
            vedtaksperiode: Vedtaksperiode,
            ytelser: Ytelser
        ) {
            validation(ytelser) {
                onError { vedtaksperiode.tilstand(ytelser, TilInfotrygd) }
                //       overlappende(vedtaksperiode.periode, person, ytelser)
                validerYtelser(vedtaksperiode.periode, ytelser, vedtaksperiode.periodetype())
                onSuccess { ytelser.addInntekter(person) }
                overlappende(vedtaksperiode.periode, ytelser.foreldrepenger())
                overlappende(vedtaksperiode.periode, ytelser.pleiepenger())
                overlappende(vedtaksperiode.periode, ytelser.omsorgspenger())
                overlappende(vedtaksperiode.periode, ytelser.opplæringspenger())
                overlappende(vedtaksperiode.periode, ytelser.institusjonsopphold())
                onSuccess {
                    arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.also { tilstøtendePeriode ->
                        vedtaksperiode.forlengelseFraInfotrygd = tilstøtendePeriode.forlengelseFraInfotrygd
                        if (tilstøtendePeriode.forlengelseFraInfotrygd == JA)
                            vedtaksperiode.skjæringstidspunktFraInfotrygd = tilstøtendePeriode.skjæringstidspunktFraInfotrygd.also {
                                vedtaksperiode.loggUlikSkjæringstidspunkt(ytelser.utbetalingshistorikk())
                            }
                        else vedtaksperiode.skjæringstidspunktFraInfotrygd = null
                        return@onSuccess
                    }

                    Oldtidsutbetalinger().also { oldtid ->
                        ytelser.utbetalingshistorikk().append(oldtid)
                        arbeidsgiver.utbetalteUtbetalinger()
                            .forEach { it.append(arbeidsgiver.organisasjonsnummer(), oldtid) }

                        if (!oldtid.utbetalingerInkludert(arbeidsgiver).erRettFør(vedtaksperiode.periode)) {
                            vedtaksperiode.forlengelseFraInfotrygd = ForlengelseFraInfotrygd.NEI
                            vedtaksperiode.skjæringstidspunktFraInfotrygd = null
                            ytelser.info("Perioden er en førstegangsbehandling")
                            return@onSuccess
                        }

                        arbeidsgiver.forkastAlleTidligere(vedtaksperiode, ytelser)
                        ytelser.kontekst(vedtaksperiode)

                        vedtaksperiode.forlengelseFraInfotrygd = JA
                        vedtaksperiode.skjæringstidspunktFraInfotrygd = person.skjæringstidspunkt(
                            vedtaksperiode.periode.endInclusive,
                            ytelser.utbetalingshistorikk()
                        )
                    }

                    arbeidsgiver.addInntekt(ytelser)
                    ytelser.info("Perioden er en direkte overgang fra periode i Infotrygd")
                }
                harInntektshistorikk(arbeidsgiver, vedtaksperiode.periode.start)
                lateinit var engineForTimeline: ArbeidsgiverUtbetalinger
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    fun personTidslinje(ytelser: Ytelser, periode: Periode) =
                        Oldtidsutbetalinger().let { oldtid ->
                            ytelser.utbetalingshistorikk().append(oldtid)
                            arbeidsgiver.utbetalteUtbetalinger()
                                .forEach { it.append(arbeidsgiver.organisasjonsnummer(), oldtid) }
                            oldtid.personTidslinje(periode)
                        }

                    engineForTimeline = ArbeidsgiverUtbetalinger(
                        tidslinjer = person.utbetalingstidslinjer(vedtaksperiode.periode, ytelser),
                        personTidslinje = personTidslinje(ytelser, vedtaksperiode.periode),
                        periode = vedtaksperiode.periode,
                        skjæringstidspunkter = person.skjæringstidspunkter(
                            vedtaksperiode.periode.endInclusive,
                            ytelser.utbetalingshistorikk().historiskeTidslinjer()
                        ),
                        alder = Alder(vedtaksperiode.fødselsnummer),
                        arbeidsgiverRegler = NormalArbeidstaker,
                        aktivitetslogg = ytelser.aktivitetslogg,
                        organisasjonsnummer = vedtaksperiode.organisasjonsnummer,
                        fødselsnummer = vedtaksperiode.fødselsnummer
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            trengerSimulering(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (simulering.valider(vedtaksperiode.utbetaling().arbeidsgiverOppdrag().removeUEND()).hasErrorsOrWorse()) {
                return vedtaksperiode.tilstand(simulering, TilInfotrygd)
            }
            vedtaksperiode.dataForSimulering = simulering.simuleringResultat
            vedtaksperiode.tilstand(simulering, AvventerGodkjenning)
        }

        private fun trengerSimulering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            simulering(
                aktivitetslogg = hendelse,
                oppdrag = Fagområde.SykepengerRefusjon.utbetalingslinjer(vedtaksperiode.utbetaling()).removeUEND(),
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            godkjenning(
                aktivitetslogg = hendelse,
                periodeFom = vedtaksperiode.periode.start,
                periodeTom = vedtaksperiode.periode.endInclusive,
                sykepengegrunnlag = vedtaksperiode.arbeidsgiver.inntekt(vedtaksperiode.periode.start)!!,
                vedtaksperiodeaktivitetslogg = vedtaksperiode.person.aktivitetslogg.logg(vedtaksperiode),
                periodetype = vedtaksperiode.periodetype()
            )
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
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.info("Tilbakestiller ikke en vedtaksperiode som har gått til utbetaling")
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            val cast = hendelse as Utbetalingsgodkjenning
            sendUtbetalingsbehov(
                aktivitetslogg = hendelse,
                oppdrag = vedtaksperiode.utbetaling().arbeidsgiverOppdrag(),
                maksdato = vedtaksperiode.maksdato,
                saksbehandler = cast.saksbehandler(),
                saksbehandlerEpost = cast.saksbehandlerEpost(),
                godkjenttidspunkt = cast.godkjenttidspunkt(),
                annullering = false
            )
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, annullering: Annullering) {
            vedtaksperiode.kontekst(annullering)
            vedtaksperiode.tilstand(annullering, TilAnnullering)
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            hendelse.error("Feilrespons fra oppdrag")
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
            hendelse.info("Overstyrer ikke en vedtaksperiode med utbetaling som har feilet")
        }
    }

    internal object AvsluttetUtenUtbetaling : Vedtaksperiodetilstand {
        override val type = AVSLUTTET_UTEN_UTBETALING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndter(
                inntektsmelding,
                if (inntektsmelding.isNotQualified() || vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(
                        vedtaksperiode
                    ) == null
                ) {
                    inntektsmelding.beingQualified()
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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
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

//        override fun håndter(vedtaksperiode: Vedtaksperiode, annullering: Annullering) {
//            vedtaksperiode.kontekst(annullering)
//        }

    }

    internal object Avsluttet : Vedtaksperiodetilstand {
        override val type = AVSLUTTET

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: TilbakestillBehandling) {
            hendelse.info("Tilbakestiller ikke en vedtaksperiode som har gått til utbetalinger")
        }

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.sykmeldingsperiode)
            vedtaksperiode.sendUtbetaltEvent()
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling(vedtaksperiode, hendelse)
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, annullering: Annullering) {
            vedtaksperiode.kontekst(annullering)
            vedtaksperiode.sykdomshistorikk.håndter(annullering)
            vedtaksperiode.sykdomstidslinje =
                vedtaksperiode.arbeidsgiver.sykdomstidslinje().subset(vedtaksperiode.periode)
            vedtaksperiode.hendelseIder.add(annullering.meldingsreferanseId())

            vedtaksperiode.tilstand(annullering, AvventerHistorikk)
        }

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

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: PersonHendelse) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            //vedtaksperiode.arbeidsgiver.søppelbøtte(vedtaksperiode, hendelse, Arbeidsgiver.TIDLIGERE_OG_ETTERGØLGENDE)
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

        internal fun tidligerePerioderFerdigBehandlet(perioder: List<Vedtaksperiode>, vedtaksperiode: Vedtaksperiode) =
            perioder
                .filterNot { it >= vedtaksperiode }
                .all { it.erFerdigBehandlet() }
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
    INFOTRYGDFORLENGELSE
}
