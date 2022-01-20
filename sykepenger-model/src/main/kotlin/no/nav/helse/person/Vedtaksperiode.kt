package no.nav.helse.person

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.Grunnbeløp
import no.nav.helse.Toggle
import no.nav.helse.hendelser.*
import no.nav.helse.hendelser.Validation.Companion.validation
import no.nav.helse.hendelser.utbetaling.UtbetalingHendelse
import no.nav.helse.hendelser.utbetaling.Utbetalingsgodkjenning
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
import no.nav.helse.person.Arbeidsgiver.GjenopptaBehandling
import no.nav.helse.person.ForkastetÅrsak.ERSTATTES
import no.nav.helse.person.ForkastetÅrsak.IKKE_STØTTET
import no.nav.helse.person.ForlengelseFraInfotrygd.JA
import no.nav.helse.person.ForlengelseFraInfotrygd.NEI
import no.nav.helse.person.InntektsmeldingInfo.Companion.ider
import no.nav.helse.person.Periodetype.*
import no.nav.helse.person.TilstandType.*
import no.nav.helse.person.builders.UtbetaltEventBuilder
import no.nav.helse.person.builders.VedtakFattetBuilder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Dag.Companion.replace
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingslinjer.Utbetaling
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.utbetalingstidslinje.Sykepengerettighet
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Økonomi
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
    private var sykdomstidslinje: Sykdomstidslinje,
    private val hendelseIder: MutableSet<UUID>,
    private var inntektsmeldingInfo: InntektsmeldingInfo?,
    private var periode: Periode,
    private val sykmeldingsperiode: Periode,
    private val utbetalinger: VedtaksperiodeUtbetalinger,
    private var utbetalingstidslinje: Utbetalingstidslinje = Utbetalingstidslinje(),
    private var forlengelseFraInfotrygd: ForlengelseFraInfotrygd = ForlengelseFraInfotrygd.IKKE_ETTERSPURT,
    private var inntektskilde: Inntektskilde,
    private val opprettet: LocalDateTime,
    private var oppdatert: LocalDateTime = opprettet
) : Aktivitetskontekst, Comparable<Vedtaksperiode> {

    private val skjæringstidspunkt get() = skjæringstidspunktFraInfotrygd ?: person.skjæringstidspunkt(periode)
    private val periodetype get() = arbeidsgiver.periodetype(periode)

    internal constructor(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        hendelse: Sykmelding
    ) : this(
        person = person,
        arbeidsgiver = arbeidsgiver,
        id = UUID.randomUUID(),
        aktørId = hendelse.aktørId(),
        fødselsnummer = hendelse.fødselsnummer(),
        organisasjonsnummer = hendelse.organisasjonsnummer(),
        tilstand = Start,
        skjæringstidspunktFraInfotrygd = null,
        sykdomstidslinje = hendelse.sykdomstidslinje(),
        hendelseIder = mutableSetOf(),
        inntektsmeldingInfo = null,
        periode = hendelse.periode(),
        sykmeldingsperiode = hendelse.periode(),
        utbetalinger = VedtaksperiodeUtbetalinger(arbeidsgiver),
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
            skjæringstidspunktFraInfotrygd,
            periodetype,
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
            skjæringstidspunkt,
            skjæringstidspunktFraInfotrygd,
            periodetype,
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
        hendelseIder.add(sykmelding.meldingsreferanseId())
        tilstand.håndter(this, sykmelding)
    }

    internal fun håndter(søknad: Søknad) = overlapperMed(søknad).also {
        if (!it) return it
        kontekst(søknad)
        hendelseIder.add(søknad.meldingsreferanseId())
        tilstand.håndter(this, søknad)
    }

    internal fun håndter(inntektsmelding: Inntektsmelding, other: UUID?, vedtaksperioder: List<Vedtaksperiode>): Boolean {
        val overlapper = overlapperMedSammenhengende(inntektsmelding, other, vedtaksperioder)
        return overlapper.also {
            if (it) hendelseIder.add(inntektsmelding.meldingsreferanseId())
            if (arbeidsgiver.harRefusjonOpphørt(periode.endInclusive) && !erAvsluttet()) {
                kontekst(inntektsmelding)
                inntektsmelding.info("Ville tidligere blitt kastet ut på grunn av refusjon: Refusjon opphører i perioden")
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

    internal fun håndter(ytelser: Ytelser, infotrygdhistorikk: Infotrygdhistorikk, arbeidsgiverUtbetalinger: (IAktivitetslogg) -> ArbeidsgiverUtbetalinger) {
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

    internal fun håndter(påminnelse: Påminnelse): Boolean {
        if (!påminnelse.erRelevant(id)) return false
        kontekst(påminnelse)
        tilstand.påminnelse(this, påminnelse)
        return true
    }

    internal fun håndter(hendelse: GjenopptaBehandling): Boolean {
        if (tilstand.erFerdigBehandlet) return false
        kontekst(hendelse)
        tilstand.håndter(this, hendelse)
        return true
    }

    internal fun håndter(hendelse: OverstyrTidslinje) = hendelse.erRelevant(periode).also { erRelevant ->
        if (!erRelevant) return IKKE_HÅNDTERT
        kontekst(hendelse)
        hendelseIder.add(hendelse.meldingsreferanseId())
        if (!hendelse.alleredeHåndtert()) {
            hendelse.markerHåndtert()
            tilstand.håndter(this, hendelse)
        }
    }

    internal fun håndter(overstyrArbeidsforhold: OverstyrArbeidsforhold): Boolean {
        if (!overstyrArbeidsforhold.erRelevant(skjæringstidspunkt)) return false
        kontekst(overstyrArbeidsforhold)
        hendelseIder.add(overstyrArbeidsforhold.meldingsreferanseId())
        return tilstand.håndter(this, overstyrArbeidsforhold)
    }

    internal fun håndter(hendelse: OverstyrInntekt) {
        kontekst(hendelse)
        if (forlengelseFraInfotrygd == JA) {
            hendelse.error("Forespurt overstyring av inntekt hvor skjæringstidspunktet ligger i infotrygd")
            return
        }

        if (Toggle.RevurdereInntektMedFlereArbeidsgivere.disabled && inntektskilde == Inntektskilde.FLERE_ARBEIDSGIVERE) {
            hendelse.error("Forespurt overstyring av inntekt hvor personen har flere arbeidsgivere (inkl. ghosts)")
            return
        }

        hendelse.loggførHendelsesreferanse(person)
        tilstand.håndter(this, hendelse)
    }

    internal fun nyPeriode(ny: Vedtaksperiode, hendelse: Sykmelding) {
        håndterEndringIEldrePeriode(ny, Vedtaksperiodetilstand::nyPeriodeFør, hendelse)
    }

    internal fun kanReberegne(other: Vedtaksperiode): Boolean {
        if (other > this || other == this) return true
        return tilstand.kanReberegnes
    }

    internal fun periodeReberegnetFør(ny: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        håndterEndringIEldrePeriode(ny, Vedtaksperiodetilstand::reberegnetPeriodeFør, hendelse)
    }

    internal fun nyRevurderingFør(revurdert: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
        håndterEndringIEldrePeriode(revurdert, Vedtaksperiodetilstand::nyRevurderingFør, hendelse)
    }

    private fun <Hendelse : ArbeidstakerHendelse> håndterEndringIEldrePeriode(
        ny: Vedtaksperiode,
        håndterer: (Vedtaksperiodetilstand, Vedtaksperiode, Vedtaksperiode, Hendelse) -> Unit,
        hendelse: Hendelse
    ) {
        if (ny > this || ny == this) return
        kontekst(hendelse)
        håndterer(tilstand, this, ny, hendelse)
        if (hendelse.hasErrorsOrWorse()) return
        if (ny.erSykeperiodeRettFør(this)) return tilstand.håndterTidligereTilstøtendeUferdigPeriode(this, ny, hendelse)
        tilstand.håndterTidligereUferdigPeriode(this, ny, hendelse)
    }

    internal fun håndterRevurderingFeilet(event: IAktivitetslogg) {
        event.kontekst(this)
        tilstand.håndterRevurderingFeilet(this, event)
    }

    private fun harSammeArbeidsgiverSom(vedtaksperiode: Vedtaksperiode) = vedtaksperiode.arbeidsgiver.organisasjonsnummer() == organisasjonsnummer

    internal fun håndterRevurdertUtbetaling(other: Utbetaling, hendelse: ArbeidstakerHendelse) {
        if (utbetalinger.hørerIkkeSammenMed(other)) return
        tilstand.håndterRevurdertUtbetaling(this, other, hendelse)
    }

    override fun compareTo(other: Vedtaksperiode) = this.periode.endInclusive.compareTo(other.periode.endInclusive)

    internal fun erSykeperiodeRettFør(other: Vedtaksperiode) = this.sykdomstidslinje.erRettFør(other.sykdomstidslinje)
    internal fun erSykeperiodeAvsluttetUtenUtbetalingRettFør(other: Vedtaksperiode) =
        this.sykdomstidslinje.erRettFør(other.sykdomstidslinje) && this.tilstand == AvsluttetUtenUtbetaling

    private fun harSykeperiodeRettFør() = arbeidsgiver.finnSykeperiodeRettFør(this) != null

    internal fun kanHåndtereOverstyring(hendelse: OverstyrInntekt): Boolean {
        return utbetalingstidslinje.isNotEmpty() && gjelder(hendelse.skjæringstidspunkt)
    }

    internal fun gjelder(skjæringstidspunkt: LocalDate) = this.skjæringstidspunkt == skjæringstidspunkt

    internal fun inntektskilde() = inntektskilde
    private fun harInntekt() = harInntektsmelding() || arbeidsgiver.grunnlagForSykepengegrunnlag(skjæringstidspunkt, periode.start) != null
    private fun harInntektsmelding() = arbeidsgiver.harInntektsmelding(skjæringstidspunkt)
    private fun avgjørTilstandForInntekt(): Vedtaksperiodetilstand {
        val rettFør = arbeidsgiver.finnSykeperiodeRettFør(this)
        if (harInntekt() || rettFør?.harInntekt() == true) return AvventerHistorikk
        return AvventerInntektsmeldingFerdigForlengelse
    }

    private fun avgjørTilstandForGap(hvisForlengelse: Vedtaksperiodetilstand, hvisGap: Vedtaksperiodetilstand) =
        if (harSykeperiodeRettFør()) hvisForlengelse else hvisGap

    private fun skalHaWarningForFlereArbeidsforholdUtenSykdomEllerUlikStartdato(
        vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
    ) =
        arbeidsgiver.erFørstegangsbehandling(periode, skjæringstidspunkt)
            && (flereArbeidsforholdUtenSykdom(vilkårsgrunnlag) || flereArbeidsforholdUlikStartdato())

    private fun flereArbeidsforholdUtenSykdom(vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement) =
        vilkårsgrunnlag is VilkårsgrunnlagHistorikk.Grunnlagsdata && vilkårsgrunnlag.harInntektFraAOrdningen()

    private fun flereArbeidsforholdUlikStartdato() = person.harFlereArbeidsforholdMedUlikStartdato(skjæringstidspunkt)

    internal fun forkast(hendelse: IAktivitetslogg, årsak: ForkastetÅrsak) {
        kontekst(hendelse)
        hendelse.info("Forkaster vedtaksperiode: %s", this.id.toString())
        if (årsak !== ERSTATTES && !erAvsluttet() && this.tilstand !is RevurderingFeilet) tilstand(hendelse, TilInfotrygd)
        kontekst(hendelse)
        utbetalinger.forkast(hendelse)
        person.vedtaksperiodeAvbrutt(
            hendelse,
            PersonObserver.VedtaksperiodeAvbruttEvent(
                gjeldendeTilstand = tilstand.type,
            )
        )
    }

    private fun erAvsluttet() =
        utbetalinger.erAvsluttet() || tilstand == AvsluttetUtenUtbetaling

    internal fun kanForkastes(utbetalinger: List<Utbetaling>): Boolean {
        if (!this.utbetalinger.harUtbetaling()) return tilstand != AvsluttetUtenUtbetaling
        return this.utbetalinger.kanForkastes(utbetalinger)
    }

    private fun erUtbetalt() = tilstand == Avsluttet && utbetalinger.erAvsluttet()

    internal fun revurder(hendelse: Påminnelse) {
        kontekst(hendelse)
        tilstand.revurder(this, hendelse)
    }

    internal fun revurder(hendelse: OverstyrTidslinje) {
        kontekst(hendelse)
        tilstand.revurder(this, hendelse)
    }

    internal fun revurder(hendelse: OverstyrInntekt) {
        kontekst(hendelse)
        tilstand.revurder(this, hendelse)
    }

    internal fun periode() = periode

    private fun kontekst(hendelse: IAktivitetslogg) {
        hendelse.kontekst(arbeidsgiver)
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
    }

    private fun overlapperMed(hendelse: SykdomstidslinjeHendelse) = hendelse.erRelevant(this.periode)

    private fun overlapperMedSammenhengende(inntektsmelding: Inntektsmelding, other: UUID?, vedtaksperioder: List<Vedtaksperiode>): Boolean {
        val perioder = arbeidsgiver.finnSammenhengendePeriode(skjæringstidspunkt)
        return inntektsmelding.erRelevant(perioder.periode()) && relevantForReplay(other, perioder, vedtaksperioder)
    }

    // IM som replayes skal kunne påvirke alle perioder som er sammenhengende med replay-perioden, men også alle evt. påfølgende perioder.
    // Dvs. IM skal -ikke- påvirke perioder _før_ replay-perioden som ikke er i sammenheng med vedtaksperioden som ba om replay
    private fun relevantForReplay(other: UUID?, sammenhengendePerioder: List<Vedtaksperiode>, vedtaksperioder: List<Vedtaksperiode>): Boolean {
        if (other == null) return true // ingen replay
        if (sammenhengendePerioder.any { it.id == other }) return true // perioden som ba om replay er en del av de sammenhengende periodene som overlapper med IM (som vedtaksperioden er en del av)
        return this > vedtaksperioder.first { it.id == other } // vedtaksperioden er _etter_ den perioden som ba om replay
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

    private fun håndterInntektsmelding(hendelse: Inntektsmelding, hvisIngenErrors: () -> Unit = {}, nesteTilstand: () -> Vedtaksperiodetilstand) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        inntektsmeldingInfo = arbeidsgiver.addInntektsmelding(skjæringstidspunkt, hendelse)

        hendelse.validerFørsteFraværsdag(skjæringstidspunkt)
        finnArbeidsgiverperiode()?.also { hendelse.validerArbeidsgiverperiode(it) }
        hendelse.valider(periode)
        if (hendelse.hasErrorsOrWorse()) return arbeidsgiver.søppelbøtte(hendelse, SENERE_INCLUSIVE(this), IKKE_STØTTET)
        hvisIngenErrors()
        hendelse.info("Fullført behandling av inntektsmelding")
        if (hendelse.hasErrorsOrWorse()) return
        tilstand(hendelse, nesteTilstand())
    }

    private fun håndterInntektsmelding(hendelse: Inntektsmelding, hvisUtenforArbeidsgiverperioden: Vedtaksperiodetilstand) {
        håndterInntektsmelding(hendelse, hvisIngenErrors = {
            if (!alleAndreAvventerArbeidsgivere()) hendelse.validerMuligBrukerutbetaling()
            if (hendelse.hasErrorsOrWorse()) person.invaliderAllePerioder(hendelse, null)
        }) {
            if (erInnenforArbeidsgiverperioden()) AvsluttetUtenUtbetaling else hvisUtenforArbeidsgiverperioden
        }
    }

    private fun oppdaterHistorikk(hendelse: SykdomstidslinjeHendelse) {
        hendelse.padLeft(periode.start)
        hendelse.trimLeft(periode.endInclusive)
        sykdomstidslinje = arbeidsgiver.oppdaterSykdom(hendelse).subset(periode)
    }

    private fun håndterSøknad(hendelse: Søknad, nesteTilstand: Vedtaksperiodetilstand?) {
        periode = periode.oppdaterFom(hendelse.periode())
        oppdaterHistorikk(hendelse)
        if (!person.harFlereArbeidsgivereMedSykdom()) hendelse.validerIkkeOppgittFlereArbeidsforholdMedSykmelding()
        hendelse.valider(periode)
        if (hendelse.hasErrorsOrWorse()) {
            if (person.harFlereArbeidsgivereMedSykdom()) return person.invaliderAllePerioder(
                hendelse,
                "Invaliderer alle perioder pga flere arbeidsgivere og feil i søknad"
            )
            hendelse.error("Invaliderer alle perioder for arbeidsgiver pga feil i søknad")
            return tilstand(hendelse, TilInfotrygd)
        }
        val tilstand = if (erInnenforArbeidsgiverperioden()) AvsluttetUtenUtbetaling else nesteTilstand
        tilstand?.also { tilstand(hendelse, it) }
    }

    private fun overlappendeSøknadIkkeStøttet(søknad: Søknad, egendefinertFeiltekst: String? = null) {
        søknad.trimLeft(periode.endInclusive)
        søknad.error(egendefinertFeiltekst ?: "Mottatt flere søknader for perioden - det støttes ikke før replay av hendelser er på plass")
        if (!tilstand.kanForkastes) return
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
        håndterSøknad(søknad, nesteTilstand)
    }

    private fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag, nesteTilstand: Vedtaksperiodetilstand) {
        vilkårsgrunnlag.lagreInntekter(person, skjæringstidspunkt)

        val grunnlagForSykepengegrunnlag = person.beregnSykepengegrunnlag(skjæringstidspunkt, vilkårsgrunnlag)
        val sammenligningsgrunnlag = person.beregnSammenligningsgrunnlag(skjæringstidspunkt)

        vilkårsgrunnlag.valider(
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            skjæringstidspunkt,
            person.antallArbeidsgivereMedRelevantArbeidsforhold(skjæringstidspunkt),
            periodetype
        )
        person.lagreVilkårsgrunnlag(skjæringstidspunkt, vilkårsgrunnlag)
        if (vilkårsgrunnlag.hasErrorsOrWorse()) {
            return person.invaliderAllePerioder(vilkårsgrunnlag, null)
        }
        vilkårsgrunnlag.info("Vilkårsgrunnlag vurdert")
        tilstand(vilkårsgrunnlag, nesteTilstand)
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
                tom = this.periode.endInclusive
            )
        )
    }

    private fun trengerIkkeInntektsmelding(hendelseskontekst: Hendelseskontekst) {
        this.person.trengerIkkeInntektsmelding(
            hendelseskontekst,
            PersonObserver.TrengerIkkeInntektsmeldingEvent(
                fom = this.periode.start,
                tom = this.periode.endInclusive
            )
        )
    }

    private fun emitVedtaksperiodeReberegnet(hendelseskontekst: Hendelseskontekst) {
        person.vedtaksperiodeReberegnet(hendelseskontekst)
    }

    private fun emitVedtaksperiodeEndret(
        aktivitetslogg: IAktivitetslogg,
        previousState: Vedtaksperiodetilstand = tilstand
    ) {
        val event = PersonObserver.VedtaksperiodeEndretEvent(
            gjeldendeTilstand = tilstand.type,
            forrigeTilstand = previousState.type,
            aktivitetslogg = aktivitetslogg.toMap(),
            harVedtaksperiodeWarnings = person.aktivitetslogg.logg(this).let { it.hasWarningsOrWorse() && !it.hasErrorsOrWorse() },
            hendelser = hendelseIder,
            makstid = tilstand.makstid(this, LocalDateTime.now())
        )

        person.vedtaksperiodeEndret(aktivitetslogg, event)
    }

    private fun sendVedtakFattet(hendelse: IAktivitetslogg) {
        val builder = VedtakFattetBuilder(periode, hendelseIder, skjæringstidspunkt, person.vilkårsgrunnlagFor(skjæringstidspunkt))
        utbetalinger.build(builder)
        person.vedtakFattet(hendelse.hendelseskontekst(), builder.result())
    }

    private fun tickleForArbeidsgiveravhengighet(påminnelse: Påminnelse) {
        Companion.gjenopptaBehandling(påminnelse, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun gjenopptaBehandling(hendelse: ArbeidstakerHendelse, nesteTilstand: Vedtaksperiodetilstand) {
        kontekst(hendelse)
        tilstand(hendelse, nesteTilstand)
    }

    private fun overlappendeVedtaksperioder() = person.nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET).filter { periode.overlapperMed(it.periode) }
    private fun Iterable<Vedtaksperiode>.kanGåTilNesteTilstand() =
        first() == this@Vedtaksperiode && drop(1).all { it.tilstand == AvventerArbeidsgivere }

    private fun alleAndreAvventerArbeidsgivere() = overlappendeVedtaksperioder().all { it == this || it.tilstand == AvventerArbeidsgivere }

    private fun forberedMuligUtbetaling(vilkårsgrunnlag: Vilkårsgrunnlag) {
        håndter(vilkårsgrunnlag, nesteTilstand())
        Companion.gjenopptaBehandling(vilkårsgrunnlag, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun forberedMuligUtbetaling(inntektsmelding: Inntektsmelding) {
        håndterInntektsmelding(inntektsmelding, nesteTilstand())
        Companion.gjenopptaBehandling(inntektsmelding, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun forberedMuligUtbetaling(søknad: Søknad) {
        håndterSøknad(søknad, nesteTilstand())
        Companion.gjenopptaBehandling(søknad, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun nesteTilstand(): Vedtaksperiodetilstand {
        val overlappendeVedtaksperioder = overlappendeVedtaksperioder()
        val nesteTilstand = when {
            overlappendeVedtaksperioder.kanGåTilNesteTilstand() -> AvventerHistorikk
            else -> AvventerArbeidsgivere
        }
        return nesteTilstand
    }
    /**
     * Skedulering av utbetaling opp mot andre arbeidsgivere
     */
    private fun forsøkUtbetaling(
        sykepengerettighet: Sykepengerettighet,
        hendelse: ArbeidstakerHendelse
    ) {
        val vedtaksperioder = person.nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET)
        val første = vedtaksperioder.first()
        if (første == this) {
            return første.forsøkUtbetalingSteg2(vedtaksperioder.drop(1), sykepengerettighet, hendelse)
        }

        this.tilstand(hendelse, AvventerArbeidsgivere)
        Companion.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivere, AvventerHistorikk)
    }

    private fun lagUtbetaling(sykepengerettighet: Sykepengerettighet, hendelse: ArbeidstakerHendelse) {
        utbetalingstidslinje = utbetalinger.lagUtbetaling(fødselsnummer, periode, sykepengerettighet, hendelse)
    }

    private fun forsøkUtbetalingSteg2(
        andreVedtaksperioder: List<Vedtaksperiode>,
        sykepengerettighet: Sykepengerettighet,
        hendelse: ArbeidstakerHendelse
    ) {
        if (andreVedtaksperioder
                .filter { this.periode.overlapperMed(it.periode) }
                .all { it.tilstand == AvventerArbeidsgivere }
        ) {
            (andreVedtaksperioder + this)
                .filter { this.periode.overlapperMed(it.periode) }
                .forEach { it.lagUtbetaling(sykepengerettighet, hendelse) }
            høstingsresultater(hendelse, andreVedtaksperioder)
        } else tilstand(hendelse, AvventerArbeidsgivere)
    }

    private fun høstingsresultater(hendelse: ArbeidstakerHendelse, andreVedtaksperioder: List<Vedtaksperiode>) {
        val ingenUtbetaling = !utbetalinger.harUtbetalinger()
        val kunArbeidsgiverdager = utbetalingstidslinje.kunArbeidsgiverdager()
        val ingenWarnings = !person.aktivitetslogg.logg(this).hasWarningsOrWorse()
        val harBrukerutbetaling = harBrukerutbetaling(andreVedtaksperioder)

        if (!harBrukerutbetaling && villeTidligereBlittKastetUtPåGrunnAvRefusjon()) {
            hendelse.info("Behandlet en vedtaksperiode som tidligere ville blitt kastet ut på grunn av refusjon")
        }

        val filter = person.brukerutbetalingfilter()
            .periodetype(periodetype)
            .also { utbetalinger.build(it) }
            .inntektkilde(inntektskilde())
            .build()

        if (filter.filtrer(hendelse)) {
            hendelse.info("Kandidat for brukerutbetaling${if (harBrukerutbetaling) " og perioden er brukerutbetaling" else ", men perioden har ikke brukerutbetaling"}")
        }

        when {
            utbetalinger.kanIkkeFortsette(hendelse, harBrukerutbetaling) -> {
                person.invaliderAllePerioder(hendelse, "Kan ikke fortsette på grunn av manglende funksjonalitet for utbetaling til bruker")
            }
            ingenUtbetaling && kunArbeidsgiverdager && ingenWarnings -> {
                tilstand(hendelse, Avsluttet) {
                    hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav og avsluttes""")
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

    private fun villeTidligereBlittKastetUtPåGrunnAvRefusjon(): Boolean {
        val meldinger = mutableListOf<String>()
        person.aktivitetslogg.logg(this).accept(object : AktivitetsloggVisitor {
            override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) {
                meldinger.add(melding)
            }
        })
        return meldinger.any { it.startsWith("Ville tidligere blitt kastet ut på grunn av refusjon:") }
    }

    private fun harBrukerutbetaling(andreVedtaksperioder: List<Vedtaksperiode>) =
        utbetalingstidslinje.harBrukerutbetalinger() || andreVedtaksperioder.any { it.utbetalingstidslinje.harBrukerutbetalinger() }

    private fun forsøkRevurdering(sykepengerettighet: Sykepengerettighet, hendelse: ArbeidstakerHendelse) {

        val vedtaksperioder = person.nåværendeVedtaksperioder(IKKE_FERDIG_REVURDERT)
        vedtaksperioder.forEach { it.lagRevurdering(sykepengerettighet, hendelse) }
        val første = vedtaksperioder.first()
        if (første == this) return første.forsøkRevurderingSteg2(vedtaksperioder.drop(1), hendelse)

        this.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        Companion.gjenopptaBehandling(hendelse, person, AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering, IKKE_FERDIG_REVURDERT)
    }

    private fun lagRevurdering(sykepengerettighet: Sykepengerettighet, hendelse: ArbeidstakerHendelse) {
        utbetalingstidslinje = utbetalinger.lagRevurdering(fødselsnummer, periode, sykepengerettighet, hendelse)
    }

    private fun forsøkRevurderingSteg2(andreVedtaksperioder: List<Vedtaksperiode>, hendelse: ArbeidstakerHendelse) {
        when {
            !andreVedtaksperioder.erKlareTilGodkjenning() -> tilstand(hendelse, AvventerArbeidsgivereRevurdering)
            !arbeidsgiver.alleAndrePerioderErKlare(this) -> tilstand(hendelse, AvventerGjennomførtRevurdering)
            else -> høstingsresultaterRevurdering(hendelse, andreVedtaksperioder)
        }
    }

    private fun høstingsresultaterRevurdering(hendelse: ArbeidstakerHendelse, andreVedtaksperioder: List<Vedtaksperiode>) {
        hendelse.info("Videresender utbetaling til alle vedtaksperioder innenfor samme fagsystemid som er til revurdering")
        when {
            utbetalinger.kanIkkeFortsette(hendelse, harBrukerutbetaling(andreVedtaksperioder)) ->
                tilstand(hendelse, RevurderingFeilet) {
                    hendelse.error("""Saken inneholder brukerutbetalinger etter revurdering, settes til "Revurdering feilet"""")
                }
            !utbetalinger.harUtbetalinger() && utbetalingstidslinje.kunArbeidsgiverdager() && !person.aktivitetslogg.logg(this).hasWarningsOrWorse() -> {
                tilstand(hendelse, Avsluttet) {
                    hendelse.info("""Saken inneholder ingen utbetalingsdager for Nav og avsluttes""")
                }
            }
            !utbetalinger.harUtbetalinger() -> {
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

    private fun harNærliggendeUtbetaling(): Boolean {
        val periode = arbeidsgiver.søknadsperioder(hendelseIder).takeUnless { it.isEmpty() }
            ?.let { perioder -> perioder.minOf { it.start } til perioder.maxOf { it.endInclusive } } ?: this.periode
        return person.harNærliggendeUtbetaling(periode)
    }

    private fun mottaUtbetalingTilRevurdering(hendelse: ArbeidstakerHendelse, utbetaling: Utbetaling) {
        utbetalingstidslinje = utbetalinger.mottaRevurdering(hendelse, utbetaling, periode)
    }

    private fun List<Vedtaksperiode>.erKlareTilGodkjenning() = this
        .filter { this@Vedtaksperiode.periode.overlapperMed(it.periode) }
        .all { it.tilstand == AvventerArbeidsgivereRevurdering }

    private fun loggHvisForlengelse(logg: IAktivitetslogg) {
        periodetype.also { periodetype ->
            if (periodetype != FØRSTEGANGSBEHANDLING) {
                logg.info("Perioden er en forlengelse, av type $periodetype")
            }
        }
    }

    private fun sendUtbetaltEvent(hendelse: IAktivitetslogg) {
        val vilkårsgrunnlag = requireNotNull(person.vilkårsgrunnlagFor(skjæringstidspunkt)) {
            "Forventet vilkårsgrunnlag ved opprettelse av utbetalt-event"
        }
        val builder = UtbetaltEventBuilder(hendelseIder, periode, vilkårsgrunnlag)
        utbetalinger.build(builder)
        person.vedtaksperiodeUtbetalt(hendelse.hendelseskontekst(), builder.result())
    }

    private fun håndterMuligForlengelse(
        hendelse: ArbeidstakerHendelse,
        tilstandHvisForlengelse: Vedtaksperiodetilstand,
        tilstandHvisGap: Vedtaksperiodetilstand
    ) {
        tilstand(hendelse, avgjørTilstandForGap(tilstandHvisForlengelse, tilstandHvisGap))
    }

    private fun validerSenerePerioderIInfotrygd(infotrygdhistorikk: Infotrygdhistorikk): Boolean {
        val sisteYtelseFom = infotrygdhistorikk.førsteSykepengedagISenestePeriode(organisasjonsnummer) ?: return true
        return sisteYtelseFom < periode.endInclusive
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

    override fun toString() = "${this.periode.start} - ${this.periode.endInclusive} (${this.tilstand::class.simpleName})"

    private fun finnArbeidsgiverperiode() =
        arbeidsgiver.arbeidsgiverperiode(periode)

    private fun erInnenforArbeidsgiverperioden() = finnArbeidsgiverperiode()?.let { sykdomstidslinje.erInnenforArbeidsgiverperiode(it, periode) } ?: false

    // Gang of four State pattern
    internal interface Vedtaksperiodetilstand : Aktivitetskontekst {
        val type: TilstandType
        val erFerdigBehandlet: Boolean get() = false
        val kanForkastes: Boolean get() = true
        val kanReberegnes: Boolean get() = true

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

        fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {
            hendelse.error("Mottatt sykmelding eller søknad out of order")
            vedtaksperiode.arbeidsgiver.søppelbøtte(hendelse, SENERE_INCLUSIVE(ny), IKKE_STØTTET)
        }

        fun reberegnetPeriodeFør(vedtaksperiode: Vedtaksperiode, reberegnet: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (this !in setOf(Avsluttet, TilUtbetaling, UtbetalingFeilet)) return
            hendelse.error("Blokkerer reberegning av tidligere periode fordi jeg er i tilstand $type")
        }

        fun nyRevurderingFør(vedtaksperiode: Vedtaksperiode, revurdert: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {

            if (revurdert.tilstand != AvsluttetUtenUtbetaling && vedtaksperiode.harSammeArbeidsgiverSom(revurdert) && vedtaksperiode.utbetalinger.hørerIkkeSammenMed(
                    revurdert.utbetalinger
                )
            ) {
                return hendelse.error("Periode ligger senere i tid og har annen fagsystemid enn periode som skal revurderes. Blokkerer revurdering")
            }

            if (revurdert.skjæringstidspunkt != vedtaksperiode.skjæringstidspunkt && vedtaksperiode.erUtbetalt()) {
                return hendelse.error("Kan kun revurdere siste skjæringstidspunkt")
            }
        }

        fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            overlappendeSykmeldingIkkeStøttet(vedtaksperiode, sykmelding)
        }

        fun håndterOverlappendeSykmelding(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            check(
                vedtaksperiode.tilstand in setOf(
                    MottattSykmeldingFerdigGap, MottattSykmeldingUferdigGap, MottattSykmeldingFerdigForlengelse, MottattSykmeldingUferdigForlengelse
                )
            )
            if (!vedtaksperiode.sykmeldingsperiode.inneholder(sykmelding.periode())) return overlappendeSykmeldingIkkeStøttet(vedtaksperiode, sykmelding)

            if (!vedtaksperiode.arbeidsgiver.erSykmeldingenDenSistSkrevne(sykmelding, vedtaksperiode.hendelseIder)) {
                sykmelding.warn("Mottatt en sykmelding som er skrevet tidligere enn den som er lagt til grunn, vurder sykmeldingene og gjør eventuelle justeringer")
            } else {
                if (inneholderForskjelligeGraderinger(vedtaksperiode, sykmelding)) {
                    sykmelding.warn("Korrigert sykmelding er lagt til grunn - kontroller dagene i sykmeldingsperioden")
                } else {
                    sykmelding.info("Ny sykmelding med like graderinger er lagt til grunn")
                }
                vedtaksperiode.oppdaterHistorikk(sykmelding)
            }
        }

        private fun graderFraSykomstidslinje(sykdomstidslinje: Sykdomstidslinje): List<Double> {
            val grader = mutableListOf<Double>()
            sykdomstidslinje.forEach { dag ->
                dag.accept(object : SykdomstidslinjeVisitor {
                    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: SykdomstidslinjeHendelse.Hendelseskilde) {
                        økonomi.medData { grad, _, _ -> grader.add(grad) }
                    }
                })
            }
            return grader
        }

        private fun inneholderForskjelligeGraderinger(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding): Boolean {
            val graderEksisterendeSykmelding = graderFraSykomstidslinje(vedtaksperiode.sykdomstidslinje)
            val graderNySykmelding = graderFraSykomstidslinje(sykmelding.sykdomstidslinje())
            return graderEksisterendeSykmelding != graderNySykmelding
        }

        private fun overlappendeSykmeldingIkkeStøttet(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            sykmelding.overlappIkkeStøttet(vedtaksperiode.periode)
            if (!kanForkastes) return
            vedtaksperiode.tilstand(sykmelding, TilInfotrygd)
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
                onValidationFailed { vedtaksperiode.tilstand(hendelse, TilInfotrygd) }
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
            arbeidsgiverUtbetalingerFun: (IAktivitetslogg) -> ArbeidsgiverUtbetalinger
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

        fun håndter(
            vedtaksperiode: Vedtaksperiode,
            overstyrArbeidsforhold: OverstyrArbeidsforhold
        ): Boolean {
            return false
        }

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: Utbetalingsgrunnlag) {}

        fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {}

        fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.error("Forventet ikke overstyring fra saksbehandler i %s", type.name)
        }

        fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            hendelse.error("Forventet ikke overstyring fra saksbehandler i %s", type.name)
        }

        fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: Påminnelse) {
            hendelse.error("Forventet ikke å restarte overstyring i %s", type.name)
        }

        fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {}
        fun håndterRevurdertUtbetaling(vedtaksperiode: Vedtaksperiode, utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {}
        fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        fun tidligerePeriodeRebehandles(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.error("Forventet ikke at tidligere periode kan rebehandles")
        }
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

            if (forlengelse && refusjonOpphørt) {
                hendelse.info("Ville tidligere blitt kastet ut på grunn av refusjon: Refusjon er opphørt.")
            }

            return when {
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

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun tidligerePeriodeRebehandles(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            håndterOverlappendeSykmelding(vedtaksperiode, sykmelding)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, vedtaksperiode.avgjørTilstandForInntekt())
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.inntektsmeldingInfo?.erSamme(inntektsmelding)
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) { AvventerSøknadFerdigForlengelse }
        }
    }

    internal object MottattSykmeldingUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_FORLENGELSE
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusMonths(12)

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}
        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {}
        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            håndterOverlappendeSykmelding(vedtaksperiode, sykmelding)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerInntektsmeldingUferdigForlengelse)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) { AvventerSøknadUferdigForlengelse }
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

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigGap)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            håndterOverlappendeSykmelding(vedtaksperiode, sykmelding)
        }

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) {
                vedtaksperiode.avgjørTilstandForGap(
                    AvventerSøknadFerdigForlengelse,
                    AvventerSøknadFerdigGap
                )
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerInntektsmeldingEllerHistorikkFerdigGap)
            søknad.info("Fullført behandling av søknad")
        }

    }

    internal object MottattSykmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = MOTTATT_SYKMELDING_UFERDIG_GAP
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) =
            tilstandsendringstidspunkt.plusMonths(12)

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, MottattSykmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, sykmelding: Sykmelding) {
            håndterOverlappendeSykmelding(vedtaksperiode, sykmelding)
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

        override fun håndter(
            vedtaksperiode: Vedtaksperiode,
            inntektsmelding: Inntektsmelding
        ) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding) {
                vedtaksperiode.avgjørTilstandForGap(
                    AvventerSøknadUferdigForlengelse,
                    AvventerSøknadUferdigGap
                )
            }
        }
    }

    internal object AvventerSøknadFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigGap)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.forberedMuligUtbetaling(søknad)
            søknad.info("Fullført behandling av søknad")
        }

    }

    internal object AvventerRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            if (vedtaksperiode.arbeidsgiver.tidligerePerioderFerdigBehandlet(vedtaksperiode)) vedtaksperiode.tilstand(
                gjenopptaBehandling,
                AvventerHistorikkRevurdering
            )
        }

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }
    }

    internal object AvventerGjennomførtRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GJENNOMFØRT_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.arbeidsgiver.gjenopptaRevurdering()
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndterRevurdertUtbetaling(vedtaksperiode: Vedtaksperiode, utbetaling: Utbetaling, hendelse: ArbeidstakerHendelse) {
            hendelse.info("Mottatt revurdert utbetaling fra en annen vedtaksperiode")
            vedtaksperiode.mottaUtbetalingTilRevurdering(hendelse, utbetaling)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            if (!vedtaksperiode.utbetalinger.erAvsluttet()) return
            gjenopptaBehandling.info("Går til avsluttet fordi revurdering er fullført via en annen vedtaksperiode")
            vedtaksperiode.tilstand(gjenopptaBehandling, Avsluttet)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: UtbetalingHendelse) {
            hendelse.info("Kvittering håndteres av vedtaksperioden som har håndtert utbetaling av revurderingen.")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.person.overstyrUtkastRevurdering(hendelse)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.arbeidsgiver.oppdaterHistorikkRevurdering(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.person.overstyrUtkastRevurdering(hendelse)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøvingRevurdering)
            vedtaksperiode.tilstand.håndter(vedtaksperiode, hendelse)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: Påminnelse) {
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }
    }

    internal object AvventerHistorikkRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_HISTORIKK_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
            vedtaksperiode.utbetalinger.forkast(hendelse)
            vedtaksperiode.trengerYtelser(hendelse)
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
            arbeidsgiverUtbetalingerFun: (IAktivitetslogg) -> ArbeidsgiverUtbetalinger
        ) {
            val tmpLog = Aktivitetslogg()
            validation(tmpLog) {
                onValidationFailed {
                    ytelser.warn("Opplysninger fra Infotrygd har endret seg etter at vedtaket ble fattet. Undersøk om det er overlapp med periode fra Infotrygd.")
                }
                valider("Det er utbetalt en periode i Infotrygd etter perioden du skal revurdere nå. Undersøk at antall forbrukte dager og grunnlag i Infotrygd er riktig") {
                    vedtaksperiode.validerSenerePerioderIInfotrygd(
                        infotrygdhistorikk
                    )
                }
                valider { infotrygdhistorikk.valider(this, arbeidsgiver, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
            }
            validation(ytelser) {
                onValidationFailed {
                    ytelser.warn("Validering av ytelser ved revurdering feilet. Utbetalingen må annulleres")
                    vedtaksperiode.tilstand(ytelser, RevurderingFeilet)
                }
                valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                val arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(ytelser)
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    arbeidsgiver.beregn(this, arbeidsgiverUtbetalinger, vedtaksperiode.periode)
                }
                onSuccess {
                    tmpLog.accept(AktivitetsloggDeescalator(ytelser))
                    vedtaksperiode.forsøkRevurdering(arbeidsgiverUtbetalinger.sykepengerettighet, ytelser)
                }
            }
        }

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        /*
            Bakgrunn for Deescalator:
            Fordi vi ikke vil feile i revurdering om vi har errors fra validering av Infotrygdperioder
            samler vi opp alle validerings-meldinger i en egen logg som vi holder utenfor validering av resten av
            Ytelser, helt til vi vet at vi har lykkes.

            Her kopierer vi alle valideringsmeldingene til hendelse, men skriver om
            alle Error til Warn, slik at vi 1) ikke feiler og 2) forteller saksbehandler om situasjonen.
        */
        class AktivitetsloggDeescalator(private val hendelse: PersonHendelse) : AktivitetsloggVisitor {
            override fun visitInfo(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Info, melding: String, tidsstempel: String) =
                hendelse.info(melding)

            override fun visitWarn(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Warn, melding: String, tidsstempel: String) =
                hendelse.warn(melding)

            override fun visitError(kontekster: List<SpesifikkKontekst>, aktivitet: Aktivitetslogg.Aktivitet.Error, melding: String, tidsstempel: String) =
                hendelse.warn(melding)
        }
    }

    internal object AvventerVilkårsprøvingRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING_REVURDERING

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.person.nyInntekt(hendelse)
            vedtaksperiode.person.vilkårsprøvEtterNyInntekt(hendelse)
            if (!hendelse.hasErrorsOrWorse()) {
                vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
            } else {
                vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
            }
        }

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }
    }

    internal object AvventerArbeidsgivere : Vedtaksperiodetilstand {
        override val type = AVVENTER_ARBEIDSGIVERE

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.forkast(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.tickleForArbeidsgiveravhengighet(påminnelse)
        }
    }

    internal object AvventerInntektsmeldingUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerUferdig)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerInntektsmeldingEllerHistorikkFerdigGap)
        }

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerInntektsmelding(hendelse.hendelseskontekst())
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding(aktivitetslogg.hendelseskontekst())
        }
    }

    internal object AvventerInntektsmeldingFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_FERDIG_FORLENGELSE

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.loggInnenforArbeidsgiverperiode()
            vedtaksperiode.person.inntektsmeldingReplay(vedtaksperiode.id)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun tidligerePeriodeRebehandles(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        // TODO: kan fjernes nå https://trello.com/c/Eoug7QnR er gjort
        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            if (!vedtaksperiode.harInntektsmelding()) return
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

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

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
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, AvventerUferdig)
        }
    }

    internal object AvventerSøknadUferdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.håndterMuligForlengelse(gjenopptaBehandling, AvventerSøknadFerdigForlengelse, AvventerSøknadFerdigGap)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerUferdig)
            søknad.info("Fullført behandling av søknad")
        }

    }

    internal object AvventerSøknadFerdigForlengelse : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_FERDIG_FORLENGELSE

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerHistorikk)
            søknad.info("Fullført behandling av søknad")
        }

    }

    internal object AvventerSøknadUferdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_SØKNAD_UFERDIG_GAP

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerSøknadUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterSøknad(søknad, AvventerUferdig)
            søknad.info("Fullført behandling av søknad")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerSøknadFerdigGap)
        }
    }

    internal object AvventerInntektsmeldingEllerHistorikkFerdigGap : Vedtaksperiodetilstand {
        override val type = AVVENTER_INNTEKTSMELDING_ELLER_HISTORIKK_FERDIG_GAP

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.loggInnenforArbeidsgiverperiode()
            vedtaksperiode.person.inntektsmeldingReplay(vedtaksperiode.id)
            if (vedtaksperiode.arbeidsgiver.finnForkastetSykeperiodeRettFør(vedtaksperiode) == null) {
                vedtaksperiode.trengerInntektsmelding(hendelse.hendelseskontekst())
            }
            vedtaksperiode.trengerHistorikkFraInfotrygd(hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigGap)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
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
                onValidationFailed { vedtaksperiode.tilstand(hendelse, TilInfotrygd) }
                valider {
                    infotrygdhistorikk.validerOverlappende(
                        this,
                        arbeidsgiver.avgrensetPeriode(vedtaksperiode.periode),
                        vedtaksperiode.skjæringstidspunkt
                    )
                }
                onValidationFailed { person.invaliderAllePerioder(hendelse, null) }
                onSuccess {
                    if (arbeidsgiver.erForlengelse(vedtaksperiode.periode)) {
                        info("Oppdaget at perioden er en forlengelse")
                        return@onSuccess vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
                    }
                }
            }
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerHistorikkFraInfotrygd(påminnelse)
        }

        override fun leaving(vedtaksperiode: Vedtaksperiode, aktivitetslogg: IAktivitetslogg) {
            vedtaksperiode.trengerIkkeInntektsmelding(aktivitetslogg.hendelseskontekst())
        }

    }

    internal object AvventerVilkårsprøving : Vedtaksperiodetilstand {
        override val type = AVVENTER_VILKÅRSPRØVING
        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime) = tilstandsendringstidspunkt.plusDays(5)

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerVilkårsgrunnlag(hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerVilkårsgrunnlag(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, vilkårsgrunnlag: Vilkårsgrunnlag) {
            vilkårsgrunnlag.lagreArbeidsforhold(vedtaksperiode.person, vedtaksperiode.skjæringstidspunkt)
            vilkårsgrunnlag.lagreSkatteinntekter(vedtaksperiode.person, vedtaksperiode.skjæringstidspunkt)
            vilkårsgrunnlag.loggUkjenteArbeidsforhold(vedtaksperiode.person, vedtaksperiode.skjæringstidspunkt)

            if (vedtaksperiode.person.harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(vedtaksperiode.skjæringstidspunkt)) {
                vilkårsgrunnlag.warn("Arbeidsgiver er ikke registrert i Aa-registeret.")
            }
            vedtaksperiode.forberedMuligUtbetaling(vilkårsgrunnlag)
        }
    }

    internal object AvventerUferdig : Vedtaksperiodetilstand {
        override val type = AVVENTER_UFERDIG

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, gjenopptaBehandling: GjenopptaBehandling) {
            vedtaksperiode.tilstand(gjenopptaBehandling, AvventerHistorikk)
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
            if (vedtaksperiode.arbeidsgiver.harDagUtenSøknad(vedtaksperiode.periode)) {
                hendelse.error("Tidslinjen inneholder minst én dag med kilde sykmelding")
                return vedtaksperiode.tilstand(hendelse, TilInfotrygd)
            }
            vedtaksperiode.trengerYtelser(hendelse)
            vedtaksperiode.utbetalinger.forkast(hendelse)
            hendelse.info("Forespør sykdoms- og inntektshistorikk")
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
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
            arbeidsgiverUtbetalingerFun: (IAktivitetslogg) -> ArbeidsgiverUtbetalinger
        ) {
            val periodetype = vedtaksperiode.periodetype
            validation(ytelser) {
                onValidationFailed {
                    if (!ytelser.hasErrorsOrWorse()) error("Behandling av Ytelser feilet, årsak ukjent")
                    vedtaksperiode.tilstand(ytelser, TilInfotrygd)
                }
                valider("Det er utbetalt en senere periode i Infotrygd") { vedtaksperiode.validerSenerePerioderIInfotrygd(infotrygdhistorikk) }
                onSuccess {
                    vedtaksperiode.skjæringstidspunktFraInfotrygd = person.skjæringstidspunkt(vedtaksperiode.periode)
                }
                valider { infotrygdhistorikk.valider(this, arbeidsgiver, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                valider { ytelser.valider(vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt) }
                validerHvis("Har ikke overgang for alle arbeidsgivere i Infotrygd", periodetype == OVERGANG_FRA_IT) {
                    person.kunOvergangFraInfotrygd(vedtaksperiode)
                }
                validerHvis("Har utbetalinger fra andre arbeidsgivere etter skjæringstidspunktet", periodetype == OVERGANG_FRA_IT) {
                    person.ingenUkjenteArbeidsgivere(vedtaksperiode, vedtaksperiode.skjæringstidspunkt)
                }
                onSuccess { infotrygdhistorikk.addInntekter(person, this) }
                onSuccess {
                    person.lagreVilkårsgrunnlagFraInfotrygd(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.periode(), ytelser)
                }
                validerHvis(
                    "Forventer minst ett sykepengegrunnlag som er fra inntektsmelding eller Infotrygd",
                    person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt) == null
                ) {
                    person.minstEttSykepengegrunnlagSomIkkeKommerFraSkatt(vedtaksperiode.skjæringstidspunkt)
                }
                onSuccess {
                    person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt)?.also {
                        // Må gjøres frem til 1.oktober 2021. Etter denne datoen kan denne koden slettes,
                        // fordi da vil vi ikke ha noen forlengelser til perioder som har harMinimumInntekt = null til behandling lenger.
                        // TODO: Det skjer fortsatt etter 011021, link til søk: https://logs.adeo.no/goto/3d53e0351dd10ac0fddcfe58819e5abe
                        it.oppdaterManglendeMinimumInntekt(person, vedtaksperiode.skjæringstidspunkt)
                    } ?: return@håndter vedtaksperiode.tilstand(ytelser, AvventerVilkårsprøving) {
                        // TODO: Mangler ofte vilkårsgrunnlag for perioder (https://logs.adeo.no/goto/844ac8a834ecd9c7ee5022ba0f89e569).
                        info("Mangler vilkårsgrunnlag for ${vedtaksperiode.skjæringstidspunkt}")
                    }
                }
                lateinit var vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement
                onSuccess {
                    vilkårsgrunnlag = requireNotNull(person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt))
                    ytelser.kontekst(vilkårsgrunnlag)
                }
                valider("Har for mye avvik i inntekt") {
                    vilkårsgrunnlag.sjekkAvviksprosent(ytelser)
                }
                onSuccess {
                    when (periodetype) {
                        in listOf(OVERGANG_FRA_IT, INFOTRYGDFORLENGELSE) -> {
                            vedtaksperiode.forlengelseFraInfotrygd = JA
                            if (vedtaksperiode.skjæringstidspunktFraInfotrygd in LocalDate.of(2021, 5, 1) til LocalDate.of(2021, 5, 16)) {
                                val gammeltGrunnbeløp = Grunnbeløp.`6G`.beløp(LocalDate.of(2021, 4, 30))
                                val sykepengegrunnlag = vilkårsgrunnlag.sykepengegrunnlag()
                                if (sykepengegrunnlag >= gammeltGrunnbeløp) ytelser.warn("Første utbetalingsdag er i Infotrygd og mellom 1. og 16. mai. Kontroller at riktig grunnbeløp er brukt.")
                            }
                        }
                        else -> {
                            vedtaksperiode.forlengelseFraInfotrygd = NEI
                            if (vedtaksperiode.inntektsmeldingInfo == null) arbeidsgiver.finnTidligereInntektsmeldinginfo(vedtaksperiode.skjæringstidspunkt)
                                ?.also { vedtaksperiode.kopierManglende(it) }
                        }
                    }
                }
                // TODO: forventer at denne ikke trengs lenger, sjekk om dette stemmer når lagring av sykepengegrunnlaget er stabilt. Kan hvertfall ikke fjernes før https://trello.com/c/pY0WYUC0
                // TODO: https://trello.com/c/pY0WYUC0 er løst - kan denne fjernes nå?
                harNødvendigInntekt(person, vedtaksperiode.skjæringstidspunkt)
                lateinit var arbeidsgiverUtbetalinger: ArbeidsgiverUtbetalinger
                valider("Feil ved kalkulering av utbetalingstidslinjer") {
                    person.fyllUtPeriodeMedForventedeDager(ytelser, vedtaksperiode.periode, vedtaksperiode.skjæringstidspunkt)
                    arbeidsgiverUtbetalinger = arbeidsgiverUtbetalingerFun(ytelser)
                    arbeidsgiver.beregn(this, arbeidsgiverUtbetalinger, vedtaksperiode.periode)
                }
                onSuccess {
                    if (vedtaksperiode.person.harKunEttAnnetRelevantArbeidsforholdEnn(vedtaksperiode.skjæringstidspunkt, vedtaksperiode.organisasjonsnummer)) {
                        ytelser.warn("Den sykmeldte har skiftet arbeidsgiver, og det er beregnet at den nye arbeidsgiveren mottar refusjon lik forrige. Kontroller at dagsatsen blir riktig.")
                    } else if (vedtaksperiode.skalHaWarningForFlereArbeidsforholdUtenSykdomEllerUlikStartdato(vilkårsgrunnlag)) {
                        ytelser.warn("Flere arbeidsgivere, ulikt starttidspunkt for sykefraværet eller ikke fravær fra alle arbeidsforhold")
                    }
                    if (vedtaksperiode.person.vilkårsgrunnlagFor(vedtaksperiode.skjæringstidspunkt)!!.gjelderFlereArbeidsgivere()) {
                        vedtaksperiode.inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE
                    }
                    vedtaksperiode.forsøkUtbetaling(arbeidsgiverUtbetalinger.sykepengerettighet, ytelser)
                }
            }
        }

    }

    private fun loggInnenforArbeidsgiverperiode() {
        if (!erInnenforArbeidsgiverperioden()) return
        sikkerlogg.info("Vedtaksperioden {} er egentlig innenfor arbeidsgiverperioden ved {}", keyValue("vedtaksperiodeId", id), keyValue("tilstand", tilstand))
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

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            trengerSimulering(vedtaksperiode, påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetalinger.valider(simulering).hasErrorsOrWorse())
                return vedtaksperiode.tilstand(simulering, TilInfotrygd)

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

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.utbetalinger.simuler(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.utbetalinger.simuler(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, simulering: Simulering) {
            if (vedtaksperiode.utbetalinger.valider(simulering).hasErrorsOrWorse()) {
                simulering.warn("Simulering av revurdert utbetaling feilet. Utbetalingen må annulleres")
                return vedtaksperiode.tilstand(simulering, RevurderingFeilet)
            }

            if (!vedtaksperiode.utbetalinger.erKlarForGodkjenning()) {
                return
            }

            vedtaksperiode.tilstand(simulering, AvventerGodkjenningRevurdering)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }
    }

    internal object AvventerGodkjenning : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING
        override val kanReberegnes = false

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerUferdig)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, søknad: Søknad) {
            vedtaksperiode.håndterOverlappendeSøknad(søknad, AvventerHistorikk)
            if (søknad.hasErrorsOrWorse()) return
            vedtaksperiode.emitVedtaksperiodeReberegnet(søknad.hendelseskontekst())
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
                    vedtaksperiode.utbetalinger.erAvvist() -> TilInfotrygd
                    vedtaksperiode.utbetalinger.harUtbetalinger() -> TilUtbetaling
                    else -> Avsluttet
                }
            )
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.oppdaterHistorikk(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikk)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, overstyrArbeidsforhold: OverstyrArbeidsforhold): Boolean {
            vedtaksperiode.person.lagreOverstyrArbeidsforhold(overstyrArbeidsforhold)
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
            if (vedtaksperiode.utbetalinger.erHistorikkEndretSidenBeregning(infotrygdhistorikk)) return vedtaksperiode.tilstand(hendelse, AvventerHistorikk) {
                hendelse.info("Infotrygdhistorikken har endret seg, reberegner periode")
                vedtaksperiode.emitVedtaksperiodeReberegnet(hendelse.hendelseskontekst())
            }

            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.arbeidsgiver.addInntekt(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøving)
        }
    }

    private fun trengerGodkjenning(hendelse: IAktivitetslogg) {
        val aktiveVedtaksperioder = person.nåværendeVedtaksperioder(IKKE_FERDIG_BEHANDLET).map {
            val periodetype = it.periodetype
            Aktivitetslogg.Aktivitet.AktivVedtaksperiode(
                it.arbeidsgiver.organisasjonsnummer(),
                it.id,
                periodetype
            )
        }
        utbetalinger.godkjenning(
            hendelse = hendelse,
            vedtaksperiode = this,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            aktiveVedtaksperioder = aktiveVedtaksperioder,
            arbeidsforholdId = inntektsmeldingInfo?.arbeidsforholdId,
            orgnummereMedRelevanteArbeidsforhold = person.orgnummereMedRelevanteArbeidsforhold(skjæringstidspunkt),
            aktivitetslogg = person.aktivitetslogg
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

    internal fun ikkeFerdigRevurdert() = tilstand in listOf(AvventerArbeidsgivereRevurdering, AvventerHistorikkRevurdering)
    internal fun avventerRevurdering() = tilstand in listOf(AvventerGjennomførtRevurdering, AvventerGodkjenningRevurdering)
    internal fun erITilstandForRevurdering() = tilstand == AvventerSimuleringRevurdering || ikkeFerdigRevurdert() || avventerRevurdering()

    internal fun overlapperMenUlikFerieinformasjon(søknad: Søknad) = søknad.harUlikFerieinformasjon(sykdomstidslinje)

    internal fun loggførHendelsesreferanse(meldingsreferanseId: UUID) = hendelseIder.add(meldingsreferanseId)

    internal fun tidligerePeriodeRebehandles(hendelse: IAktivitetslogg) {
        kontekst(hendelse)
        tilstand.tidligerePeriodeRebehandles(this, hendelse)
    }

    internal object AvventerGodkjenningRevurdering : Vedtaksperiodetilstand {
        override val type = AVVENTER_GODKJENNING_REVURDERING
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.trengerGodkjenning(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {
            vedtaksperiode.trengerGodkjenning(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.person.overstyrUtkastRevurdering(hendelse)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.arbeidsgiver.oppdaterHistorikkRevurdering(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.person.overstyrUtkastRevurdering(hendelse)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøvingRevurdering)
            vedtaksperiode.tilstand.håndter(vedtaksperiode, hendelse)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
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

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }
    }

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
            vedtaksperiode.tickleForArbeidsgiveravhengighet(påminnelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.person.overstyrUtkastRevurdering(hendelse)
        }

        override fun håndterRevurderingFeilet(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, RevurderingFeilet)
        }
    }

    internal object TilUtbetaling : Vedtaksperiodetilstand {
        override val type = TIL_UTBETALING
        override val kanForkastes = false
        override val kanReberegnes = false


        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {}

        override fun nyRevurderingFør(vedtaksperiode: Vedtaksperiode, revurdert: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            hendelse.error("Blokkerer revurdering fordi periode er i TilUtbetaling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som har gått til utbetaling")
        }

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

        override fun nyRevurderingFør(vedtaksperiode: Vedtaksperiode, revurdert: Vedtaksperiode, hendelse: ArbeidstakerHendelse) {
            if (vedtaksperiode.utbetalinger.erSiste(revurdert.utbetalinger)) return
            hendelse.error("Blokkerer revurdering fordi periode er i UtbetalingFeilet")
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

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
                    vedtaksperiode.person.overstyrUtkastRevurdering(påminnelse)
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
            vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
            vedtaksperiode.person.inntektsmeldingReplay(vedtaksperiode.id)
        }

        override fun nyPeriodeFør(vedtaksperiode: Vedtaksperiode, ny: Vedtaksperiode, hendelse: Sykmelding) {}

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (vedtaksperiode.erInnenforArbeidsgiverperioden()) return
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigGap)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            if (vedtaksperiode.erInnenforArbeidsgiverperioden()) return
            vedtaksperiode.tilstand(hendelse, AvventerInntektsmeldingUferdigForlengelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, inntektsmelding: Inntektsmelding) {
            vedtaksperiode.håndterInntektsmelding(inntektsmelding, hvisIngenErrors = {
                vedtaksperiode.emitVedtaksperiodeEndret(inntektsmelding)
                vedtaksperiode.arbeidsgiver.gjenopptaBehandling()
                val debugkeys = arrayOf(
                    keyValue("vedtaksperiodeId", vedtaksperiode.id),
                    keyValue("aktørId", vedtaksperiode.aktørId),
                    keyValue("organisasjonsnummer", vedtaksperiode.organisasjonsnummer)
                )
                when {
                    vedtaksperiode.finnArbeidsgiverperiode() == null -> sikkerlogg.info(
                        "inntektsmelding i auu: har ikke arbeidsgiverperiode etter mottak av im for {} {} {}. Perioden er mest sannsynlig erstattet med arbeidsdager",
                        *debugkeys
                    )
                    vedtaksperiode.erInnenforArbeidsgiverperioden() -> sikkerlogg.info(
                        "inntektsmelding i auu: er fortsatt innenfor arbeidsgiverperioden etter mottak av im for {} {} {}",
                        *debugkeys
                    )
                    !vedtaksperiode.arbeidsgiver.kanReberegnes(vedtaksperiode) -> sikkerlogg.info(
                        "inntektsmelding i auu: Kan ikke reberegne {} for {} {} fordi nyere periode blokkerer",
                        *debugkeys
                    )
                    else -> sikkerlogg.info("inntektsmelding i auu: vil reberegne vedtaksperiode {} for {} {}", *debugkeys)
                }
            }) {
                if (vedtaksperiode.erInnenforArbeidsgiverperioden() || !vedtaksperiode.sykdomstidslinje.harSykedager()) {
                    AvsluttetUtenUtbetaling
                } else if (!vedtaksperiode.arbeidsgiver.kanReberegnes(vedtaksperiode)) {
                    inntektsmelding.error("Kan ikke reberegne perioden fordi det er en nyere periode som er ferdig behandlet")
                    AvsluttetUtenUtbetaling
                } else if (vedtaksperiode.person.harVedtaksperiodeForAnnenArbeidsgiver(vedtaksperiode.arbeidsgiver, vedtaksperiode.skjæringstidspunkt)) {
                    vedtaksperiode.person.invaliderAllePerioder(inntektsmelding, "Kan ikke flytte en vedtaksperiode i AVSLUTTET_UTEN_UTBETALING ved flere arbeidsgivere")
                    AvsluttetUtenUtbetaling
                } else {
                    if (Toggle.GjenopptaAvsluttetUtenUtbetaling.disabled) return@håndterInntektsmelding AvsluttetUtenUtbetaling
                    vedtaksperiode.arbeidsgiver.tidligerePeriodeRebehandles(vedtaksperiode, inntektsmelding)
                    vedtaksperiode.kontekst(inntektsmelding)
                    AvventerHistorikk
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

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            hendelse.info("Overstyrer ikke en vedtaksperiode som er avsluttet uten utbetaling")
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (!vedtaksperiode.person.kanRevurdereInntekt(hendelse.skjæringstidspunkt)) return hendelse.error("Kan ikke revurdere inntekt, da vi mangler datagrunnlag på skjæringstidspunktet")
            vedtaksperiode.person.igangsettRevurdering(hendelse, vedtaksperiode)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøvingRevurdering)
            vedtaksperiode.tilstand.håndter(vedtaksperiode, hendelse)
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
            vedtaksperiode.arbeidsgiver.lås(vedtaksperiode.periode)
            vedtaksperiode.utbetalinger.vedtakFattet(hendelse)
            check(vedtaksperiode.utbetalinger.erAvsluttet()) { "forventer at utbetaling skal være avsluttet" }
            vedtaksperiode.sendVedtakFattet(hendelse)
            vedtaksperiode.sendUtbetaltEvent(hendelse) // TODO: Fjerne når konsumentene lytter på vedtak fattet
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
            vedtaksperiode.person.igangsettRevurdering(hendelse, vedtaksperiode)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            if (vedtaksperiode.person.kanRevurdereInntekt(hendelse.skjæringstidspunkt)) {
                vedtaksperiode.person.igangsettRevurdering(hendelse, vedtaksperiode)
            } else {
                hendelse.error("Kan ikke revurdere inntekt, da vi mangler datagrunnlag på skjæringstidspunktet")
            }
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrTidslinje) {
            vedtaksperiode.arbeidsgiver.oppdaterHistorikkRevurdering(hendelse)
            vedtaksperiode.tilstand(hendelse, AvventerHistorikkRevurdering)
        }

        override fun revurder(vedtaksperiode: Vedtaksperiode, hendelse: OverstyrInntekt) {
            vedtaksperiode.tilstand(hendelse, AvventerVilkårsprøvingRevurdering)
            vedtaksperiode.tilstand.håndter(vedtaksperiode, hendelse)
        }

        override fun håndterTidligereUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndterTidligereTilstøtendeUferdigPeriode(vedtaksperiode: Vedtaksperiode, tidligere: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.tilstand(hendelse, AvventerArbeidsgivereRevurdering)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}
    }

    internal object RevurderingFeilet : Vedtaksperiodetilstand {
        override val type: TilstandType = REVURDERING_FEILET
        override val kanReberegnes = false

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            vedtaksperiode.person.revurderingHarFeilet(hendelse)
        }

        override fun håndter(vedtaksperiode: Vedtaksperiode, påminnelse: Påminnelse) {}

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime = LocalDateTime.MAX
    }

    internal object TilInfotrygd : Vedtaksperiodetilstand {
        override val type = TIL_INFOTRYGD
        override val erFerdigBehandlet = true
        override val kanReberegnes = false

        override fun makstid(vedtaksperiode: Vedtaksperiode, tilstandsendringstidspunkt: LocalDateTime): LocalDateTime =
            LocalDateTime.MAX

        override fun entering(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            hendelse.info("Sykdom for denne personen kan ikke behandles automatisk.")
            vedtaksperiode.person.søppelbøtte(
                hendelse,
                vedtaksperiode.periode
            )
            if (skalOppretteOppgave(vedtaksperiode)) sendOppgaveEvent(vedtaksperiode, hendelse)
        }

        private fun sendOppgaveEvent(vedtaksperiode: Vedtaksperiode, hendelse: IAktivitetslogg) {
            val inntektsmeldingIds =
                vedtaksperiode.arbeidsgiver.finnSammenhengendePeriode(vedtaksperiode.skjæringstidspunkt).mapNotNull { it.inntektsmeldingInfo }.ider()
            if (vedtaksperiode.harNærliggendeUtbetaling()) {
                vedtaksperiode.person.opprettOppgaveForSpeilsaksbehandlere(
                    hendelse,
                    PersonObserver.OpprettOppgaveForSpeilsaksbehandlereEvent(
                        hendelser = vedtaksperiode.hendelseIder + inntektsmeldingIds,
                    )
                )
            } else {
                vedtaksperiode.person.opprettOppgave(
                    hendelse,
                    PersonObserver.OpprettOppgaveEvent(
                        hendelser = vedtaksperiode.hendelseIder + inntektsmeldingIds,
                    )
                )
            }
        }

        private fun skalOppretteOppgave(vedtaksperiode: Vedtaksperiode) =
            vedtaksperiode.inntektsmeldingInfo != null ||
                vedtaksperiode.arbeidsgiver.finnSammenhengendePeriode(vedtaksperiode.skjæringstidspunkt).any { it.inntektsmeldingInfo != null } ||
                vedtaksperiode.arbeidsgiver.søknadsperioder(vedtaksperiode.hendelseIder).isNotEmpty()

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

        internal val SENERE_INCLUSIVE = fun(senereEnnDenne: Vedtaksperiode): VedtaksperiodeFilter {
            return fun(vedtaksperiode: Vedtaksperiode) = vedtaksperiode >= senereEnnDenne
        }

        internal val IKKE_FERDIG_REVURDERT: VedtaksperiodeFilter = { it.ikkeFerdigRevurdert() }

        internal val AVVENTER_GODKJENT_REVURDERING: VedtaksperiodeFilter = { it.avventerRevurdering() }

        internal val REVURDERING_IGANGSATT: VedtaksperiodeFilter = { it.erITilstandForRevurdering() }

        internal val KLAR_TIL_BEHANDLING: VedtaksperiodeFilter = {
            it.tilstand == AvventerArbeidsgivere || it.tilstand == AvventerGodkjenning
        }

        internal val IKKE_FERDIG_BEHANDLET: VedtaksperiodeFilter = { !it.tilstand.erFerdigBehandlet }
        internal val FERDIG_BEHANDLET: VedtaksperiodeFilter = { it.tilstand.erFerdigBehandlet }
        internal val UTEN_UTBETALING: VedtaksperiodeFilter = { it.tilstand == AvsluttetUtenUtbetaling }
        internal val ER_ELLER_HAR_VÆRT_AVSLUTTET: VedtaksperiodeFilter = { it.tilstand is AvsluttetUtenUtbetaling || it.utbetalinger.harAvsluttede() }

        internal val ALLE: VedtaksperiodeFilter = { true }

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
                .all { it.tilstand.erFerdigBehandlet }

        internal fun Iterable<Vedtaksperiode>.nåværendeVedtaksperiode(filter: VedtaksperiodeFilter) = firstOrNull(filter)

        internal fun List<Vedtaksperiode>.medSkjæringstidspunkt(skjæringstidspunkt: LocalDate) =
            this.filter { it.skjæringstidspunkt == skjæringstidspunkt }

        internal fun List<Vedtaksperiode>.harUtbetaling() = any { it.erUtbetalt() }

        internal fun List<Vedtaksperiode>.harNødvendigInntekt() =
            this.takeIf { it.isNotEmpty() }
                ?.harInntekt() ?: true

        internal fun List<Vedtaksperiode>.harInntekt() = any { it.harInntekt() }

        internal fun overlapperMedForkastet(forkastede: Iterable<Vedtaksperiode>, sykmelding: Sykmelding) {
            forkastede
                .filter { it.periode.overlapperMed(sykmelding.periode()) }
                .forEach {
                    sykmelding.error("${sykmelding.kilde} overlapper med forkastet vedtaksperiode")
                    sykmelding.info("${sykmelding.kilde} overlapper med forkastet vedtaksperiode ${it.id}, hendelse sykmeldingsperiode: ${sykmelding.periode()}, vedtaksperiode sykmeldingsperiode: ${it.periode}")
                }
        }

        internal fun sjekkOmOverlapperMedForkastet(forkastede: Iterable<Vedtaksperiode>, inntektsmelding: Inntektsmelding) =
            forkastede.any { it.periode.overlapperMed(inntektsmelding.periode()) }

        private fun List<Vedtaksperiode>.alleNåværendeErKlare(første: Vedtaksperiode, klarTilstand: Vedtaksperiodetilstand) =
            this
                .filter { første.periode.overlapperMed(it.periode) }
                .all { it.tilstand == klarTilstand }


        internal fun gjenopptaBehandling(
            hendelse: ArbeidstakerHendelse,
            person: Person,
            nåværendeTilstand: Vedtaksperiodetilstand,
            nesteTilstand: Vedtaksperiodetilstand,
            filter: VedtaksperiodeFilter = IKKE_FERDIG_BEHANDLET
        ) {
            val nåværende = person.nåværendeVedtaksperioder(filter)
            val første = nåværende.firstOrNull() ?: return
            if (nåværende.alleNåværendeErKlare(første, nåværendeTilstand)) {
                første.gjenopptaBehandling(hendelse, nesteTilstand)
            }
        }

        internal fun Iterable<Vedtaksperiode>.harOverlappendeUtbetaltePerioder(periode: Periode) =
            any { it.periode().overlapperMed(periode) && it.harPassertPunktetHvorViOppdagerFlereArbeidsgivere() }

        internal fun List<Vedtaksperiode>.iderMedUtbetaling(utbetalingId: UUID) =
            filter { it.utbetalinger.harId(utbetalingId) }.map { it.id }

        internal fun List<Vedtaksperiode>.harOverlappendeUtbetaling(periode: Periode) =
            filter { it.tilstand !is AvsluttetUtenUtbetaling }
                .any { it.utbetalinger.overlapperMed(periode) }

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
            perioder: List<Vedtaksperiode>,
            organisasjonsnummer: String,
            sykdomstidslinje: Sykdomstidslinje,
            periode: Periode
        ): Arbeidsgiverperiode? {
            val samletSykdomstidslinje = Sykdomstidslinje.gammelTidslinje(perioder.map { it.sykdomstidslinje }).merge(sykdomstidslinje, replace)
            return person.arbeidsgiverperiodeFor(organisasjonsnummer, samletSykdomstidslinje, samletSykdomstidslinje.sisteDag(), periode)
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

internal class InntektsmeldingInfo(
    private val id: UUID,
    internal val arbeidsforholdId: String?
) {
    internal fun erSamme(inntektsmelding: Inntektsmelding) {
        if (id == inntektsmelding.meldingsreferanseId()) return
        inntektsmelding.warn("Mottatt flere inntektsmeldinger - den første inntektsmeldingen som ble mottatt er lagt til grunn. Utbetal kun hvis det blir korrekt.")
    }

    internal fun leggTil(hendelser: MutableSet<UUID>) {
        hendelser.add(id)
    }

    internal fun accept(visitor: InntektsmeldingInfoVisitor) {
        visitor.visitInntektsmeldinginfo(id, arbeidsforholdId)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is InntektsmeldingInfo) return false
        return this.id == other.id && this.arbeidsforholdId == other.arbeidsforholdId
    }

    override fun hashCode() = Objects.hash(id, arbeidsforholdId)

    internal companion object {
        fun List<InntektsmeldingInfo>.ider() = map { it.id }
    }
}

internal typealias VedtaksperiodeFilter = (Vedtaksperiode) -> Boolean
