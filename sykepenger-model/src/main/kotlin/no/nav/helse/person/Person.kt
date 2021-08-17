package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.Toggles
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver.Companion.antallArbeidsgivereMedOverlappendeVedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.beregnFeriepengerForAlleArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.forlengerIkkeBareAnnenArbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.forlengerSammePeriode
import no.nav.helse.person.Arbeidsgiver.Companion.grunnlagForSammenligningsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.grunnlagForSykepengegrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.harArbeidsgivereMedOverlappendeUtbetaltePerioder
import no.nav.helse.person.Arbeidsgiver.Companion.harGrunnlagForSykepengegrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigInntekt
import no.nav.helse.person.Arbeidsgiver.Companion.harRelevanteArbeidsforholdForFlereArbeidsgivere
import no.nav.helse.person.Arbeidsgiver.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.Arbeidsgiver.Companion.relevanteArbeidsforhold
import no.nav.helse.person.Vedtaksperiode.Companion.ALLE
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler.Companion.NormalArbeidstaker
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverUtbetalinger
import no.nav.helse.utbetalingstidslinje.Feriepengeberegner
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Prosent
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.roundToInt

class Person private constructor(
    private val aktørId: String,
    private val fødselsnummer: String,
    private val arbeidsgivere: MutableList<Arbeidsgiver>,
    internal val aktivitetslogg: Aktivitetslogg,
    private val opprettet: LocalDateTime,
    private val infotrygdhistorikk: Infotrygdhistorikk,
    internal val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private var dødsdato: LocalDate?
) : Aktivitetskontekst {
    private companion object {
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    constructor(
        aktørId: String,
        fødselsnummer: String
    ) : this(aktørId, fødselsnummer, mutableListOf(), Aktivitetslogg(), LocalDateTime.now(), Infotrygdhistorikk(), VilkårsgrunnlagHistorikk(), null)

    private val observers = mutableListOf<PersonObserver>()

    fun håndter(sykmelding: Sykmelding) = håndter(sykmelding, "sykmelding")

    fun håndter(søknad: Søknad) = håndter(søknad, "søknad")

    fun håndter(søknad: SøknadArbeidsgiver) = håndter(søknad, "søknad til arbeidsgiver")

    fun håndter(inntektsmelding: Inntektsmelding) = håndter(inntektsmelding, "inntektsmelding")

    fun håndter(inntektsmelding: InntektsmeldingReplay) {
        registrer(inntektsmelding, "Behandler replay av inntektsmelding")
        finnArbeidsgiver(inntektsmelding).håndter(inntektsmelding)
    }

    private fun håndter(
        hendelse: SykdomstidslinjeHendelse,
        hendelsesmelding: String
    ) {
        registrer(hendelse, "Behandler $hendelsesmelding")
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(hendelse)
        hendelse.fortsettÅBehandle(arbeidsgiver)
    }

    fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        utbetalingshistorikk.kontekst(this)
        utbetalingshistorikk.oppdaterHistorikk(infotrygdhistorikk)
        finnArbeidsgiver(utbetalingshistorikk).håndter(utbetalingshistorikk, infotrygdhistorikk)
    }

    fun håndter(utbetalingshistorikk: UtbetalingshistorikkForFeriepenger) {
        utbetalingshistorikk.kontekst(this)

        if (utbetalingshistorikk.skalBeregnesManuelt) {
            val msg = "Person er markert for manuell beregning av feriepenger - aktørId: $aktørId"
            sikkerLogg.info(msg)
            utbetalingshistorikk.info(msg)
            return
        }

        val feriepengeberegner = Feriepengeberegner(
            alder = Alder(fødselsnummer),
            opptjeningsår = utbetalingshistorikk.opptjeningsår,
            utbetalingshistorikkForFeriepenger = utbetalingshistorikk,
            person = this
        )

        val feriepengepengebeløpPersonUtbetaltAvInfotrygd = utbetalingshistorikk.utbetalteFeriepengerTilPerson()
        val beregnetFeriepengebeløpPersonInfotrygd = feriepengeberegner.beregnFeriepengerForInfotrygdPerson().roundToInt()

        if (beregnetFeriepengebeløpPersonInfotrygd != 0 && beregnetFeriepengebeløpPersonInfotrygd !in feriepengepengebeløpPersonUtbetaltAvInfotrygd) {
            sikkerLogg.info(
                """
                Beregnet feriepengebeløp til person i IT samsvarer ikke med faktisk utbetalt beløp
                AktørId: $aktørId
                Faktisk utbetalt beløp: $feriepengepengebeløpPersonUtbetaltAvInfotrygd
                Beregnet beløp: $beregnetFeriepengebeløpPersonInfotrygd
                """.trimIndent()
            )
        }

        utbetalingshistorikk.sikreAtArbeidsgivereEksisterer {
            arbeidsgivere.finnEllerOpprett(it) {
                utbetalingshistorikk.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", it)
                Arbeidsgiver(this, it)
            }
        }
        arbeidsgivere.beregnFeriepengerForAlleArbeidsgivere(aktørId, feriepengeberegner, utbetalingshistorikk)

        if (Toggles.SendFeriepengeOppdrag.enabled) {
            aktivitetslogg.info("Feriepenger er utbetalt")
        }
    }

    fun håndter(ytelser: Ytelser) {
        registrer(ytelser, "Behandler historiske utbetalinger og inntekter")
        ytelser.oppdaterHistorikk(infotrygdhistorikk)
        ytelser.lagreDødsdato(this)

        finnArbeidsgiver(ytelser).håndter(ytelser, arbeidsgiverUtbetalinger(), infotrygdhistorikk)
    }

    internal fun arbeidsgiverUtbetalinger(regler: ArbeidsgiverRegler = NormalArbeidstaker): ArbeidsgiverUtbetalinger {
        val skjæringstidspunkter = skjæringstidspunkter()
        return ArbeidsgiverUtbetalinger(
            regler = regler,
            arbeidsgivere = arbeidsgivereMedSykdom().associateWith {
                infotrygdhistorikk.builder(
                    organisasjonsnummer = it.organisasjonsnummer(),
                    builder = it.builder(regler, skjæringstidspunkter)
                )
            },
            infotrygdtidslinje = infotrygdhistorikk.utbetalingstidslinje(),
            alder = Alder(fødselsnummer),
            dødsdato = dødsdato,
            vilkårsgrunnlagHistorikk = vilkårsgrunnlagHistorikk
        )
    }


    fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        registrer(utbetalingsgodkjenning, "Behandler utbetalingsgodkjenning")
        finnArbeidsgiver(utbetalingsgodkjenning).håndter(utbetalingsgodkjenning)
    }

    fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
    }

    fun håndter(utbetalingsgrunnlag: Utbetalingsgrunnlag) {
        registrer(utbetalingsgrunnlag, "Behandler utbetalingsgrunnlag")
        finnArbeidsgiver(utbetalingsgrunnlag).håndter(utbetalingsgrunnlag)
    }

    fun håndter(simulering: Simulering) {
        registrer(simulering, "Behandler simulering")
        finnArbeidsgiver(simulering).håndter(simulering)
    }

    fun håndter(utbetaling: UtbetalingOverført) {
        registrer(utbetaling, "Behandler utbetaling overført")
        finnArbeidsgiver(utbetaling).håndter(utbetaling)
    }

    fun håndter(utbetaling: UtbetalingHendelse) {
        registrer(utbetaling, "Behandler utbetaling")
        finnArbeidsgiver(utbetaling).håndter(utbetaling)
    }

    fun håndter(påminnelse: Utbetalingpåminnelse) {
        påminnelse.kontekst(this)
        finnArbeidsgiver(påminnelse).håndter(påminnelse)
    }

    fun håndter(påminnelse: PersonPåminnelse) {
        påminnelse.kontekst(this)
        påminnelse.info("Håndterer påminnelse for person")
    }

    fun håndter(påminnelse: Påminnelse) {
        try {
            påminnelse.kontekst(this)
            if (finnArbeidsgiver(påminnelse).håndter(påminnelse)) return
        } catch (err: Aktivitetslogg.AktivitetException) {
            påminnelse.error("Fikk påminnelse uten at vi fant arbeidsgiver eller vedtaksperiode")
        }
        observers.forEach { påminnelse.vedtaksperiodeIkkeFunnet(it) }
    }

    fun håndter(avstemming: Avstemming) {
        avstemming.kontekst(this)
        avstemming.info("Avstemmer utbetalinger og vedtaksperioder")
        val result = Avstemmer(this).toMap()
        observers.forEach { it.avstemt(result) }
    }

    fun håndter(hendelse: OverstyrTidslinje) {
        hendelse.kontekst(this)
        finnArbeidsgiver(hendelse).håndter(hendelse)
    }

    fun håndter(hendelse: OverstyrInntekt) {
        hendelse.kontekst(this)
        finnArbeidsgiver(hendelse).håndter(hendelse)
    }

    fun annullert(event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
    }

    internal fun igangsettRevurdering(hendelse: OverstyrTidslinje, vedtaksperiode: Vedtaksperiode) {
        arbeidsgivere.forEach {
            it.startRevurderingForAlleBerørtePerioder(hendelse, vedtaksperiode)
        }

        if (Toggles.RevurderTidligerePeriode.enabled) {
            if (hendelse.hasErrorsOrWorse()) return
            vedtaksperiode.revurder(hendelse, vedtaksperiode)
        }
    }

    internal fun igangsettRevurdering(hendelse: OverstyrInntekt, vedtaksperiode: Vedtaksperiode) {
        arbeidsgivere.forEach {
            it.startRevurderingForAlleBerørtePerioder(hendelse, vedtaksperiode)
        }

        if (Toggles.RevurderTidligerePeriode.enabled) {
            if (hendelse.hasErrorsOrWorse()) return
            vedtaksperiode.revurder(hendelse, vedtaksperiode)
        }
    }

    fun vedtaksperiodePåminnet(vedtaksperiodeId: UUID, påminnelse: Påminnelse) {
        observers.forEach { it.vedtaksperiodePåminnet(vedtaksperiodeId, påminnelse) }
    }

    fun vedtaksperiodeIkkePåminnet(påminnelse: Påminnelse, vedtaksperiodeId: UUID, tilstandType: TilstandType) {
        observers.forEach { it.vedtaksperiodeIkkePåminnet(påminnelse, vedtaksperiodeId, tilstandType) }
    }

    fun vedtaksperiodeAvbrutt(event: PersonObserver.VedtaksperiodeAvbruttEvent) {
        observers.forEach { it.vedtaksperiodeAvbrutt(event) }
    }

    @Deprecated("Fjernes til fordel for utbetaling_utbetalt")
    fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        observers.forEach { it.vedtaksperiodeUtbetalt(event) }
    }

    fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretEvent) {
        observers.forEach {
            it.vedtaksperiodeEndret(event)
            it.personEndret(
                PersonObserver.PersonEndretEvent(
                    aktørId = aktørId,
                    fødselsnummer = fødselsnummer,
                    person = this
                )
            )
        }
    }

    fun vedtaksperiodeReberegnet(vedtaksperiodeId: UUID) {
        observers.forEach {
            it.vedtaksperiodeReberegnet(vedtaksperiodeId)
        }
    }

    fun inntektsmeldingReplay(event: PersonObserver.InntektsmeldingReplayEvent) {
        observers.forEach {
            it.inntektsmeldingReplay(event)
        }
    }

    fun trengerInntektsmelding(event: PersonObserver.ManglendeInntektsmeldingEvent) {
        observers.forEach { it.manglerInntektsmelding(event) }
    }

    fun trengerIkkeInntektsmelding(event: PersonObserver.TrengerIkkeInntektsmeldingEvent) {
        observers.forEach { it.trengerIkkeInntektsmelding(event) }
    }

    internal fun utbetalingUtbetalt(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtbetalt(event) }
    }

    internal fun utbetalingUtenUtbetaling(event: PersonObserver.UtbetalingUtbetaltEvent) {
        observers.forEach { it.utbetalingUtenUtbetaling(event) }
    }

    internal fun utbetalingEndret(event: PersonObserver.UtbetalingEndretEvent) {
        observers.forEach { it.utbetalingEndret(event) }
    }

    internal fun vedtakFattet(vedtakFattetEvent: PersonObserver.VedtakFattetEvent) {
        observers.forEach { it.vedtakFattet(vedtakFattetEvent) }
    }

    internal fun feriepengerUtbetalt(feriepengerUtbetaltEvent: PersonObserver.FeriepengerUtbetaltEvent) {
        observers.forEach { it.feriepengerUtbetalt(feriepengerUtbetaltEvent) }
    }

    fun håndter(hendelse: AnnullerUtbetaling) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(hendelse)
            ?: hendelse.error("Finner ikke arbeidsgiver")
    }

    fun håndter(hendelse: Grunnbeløpsregulering) {
        hendelse.kontekst(this)
        arbeidsgivere.finn(hendelse.organisasjonsnummer())?.håndter(arbeidsgivere, hendelse)
            ?: hendelse.error("Finner ikke arbeidsgiver")
    }

    fun addObserver(observer: PersonObserver) {
        observers.add(observer)
    }

    internal fun nyesteIdForVilkårsgrunnlagHistorikk() =
        vilkårsgrunnlagHistorikk.sisteId()

    internal fun skjæringstidspunkt(orgnummer: String, sykdomstidslinje: Sykdomstidslinje, periode: Periode) =
        infotrygdhistorikk.skjæringstidspunkt(orgnummer, periode, sykdomstidslinje)

    internal fun skjæringstidspunkt(periode: Periode) =
        Arbeidsgiver.skjæringstidspunkt(arbeidsgivere, periode, infotrygdhistorikk)

    internal fun skjæringstidspunkter() =
        Arbeidsgiver.skjæringstidspunkter(arbeidsgivere, infotrygdhistorikk)

    internal fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, cutoff: LocalDateTime? = null): Boolean {
        val tidligsteDato = arbeidsgivereMedSykdom().minOf { it.tidligsteDato() }
        return infotrygdhistorikk.oppfriskNødvendig(hendelse, tidligsteDato, cutoff)
    }

    internal fun trengerHistorikkFraInfotrygd(hendelse: IAktivitetslogg, vedtaksperiode: Vedtaksperiode, cutoff: LocalDateTime? = null) {
        if (trengerHistorikkFraInfotrygd(hendelse, cutoff)) return hendelse.info("Må oppfriske Infotrygdhistorikken")
        hendelse.info("Trenger ikke oppfriske Infotrygdhistorikken, bruker lagret historikk")
        vedtaksperiode.håndterHistorikkFraInfotrygd(hendelse, infotrygdhistorikk)
    }

    internal fun historikkFor(orgnummer: String, sykdomstidslinje: Sykdomstidslinje) =
        infotrygdhistorikk.historikkFor(orgnummer, sykdomstidslinje)

    internal fun harInfotrygdUtbetalt(orgnummer: String, skjæringstidspunkt: LocalDate) =
        infotrygdhistorikk.harBetalt(orgnummer, skjæringstidspunkt)

    internal fun accept(visitor: PersonVisitor) {
        visitor.preVisitPerson(this, opprettet, aktørId, fødselsnummer, dødsdato)
        visitor.visitPersonAktivitetslogg(aktivitetslogg)
        aktivitetslogg.accept(visitor)
        visitor.preVisitArbeidsgivere()
        arbeidsgivere.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsgivere()
        infotrygdhistorikk.accept(visitor)
        vilkårsgrunnlagHistorikk.accept(visitor)
        visitor.postVisitPerson(this, opprettet, aktørId, fødselsnummer, dødsdato)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return SpesifikkKontekst("Person", mapOf("fødselsnummer" to fødselsnummer, "aktørId" to aktørId))
    }

    private fun registrer(hendelse: PersonHendelse, melding: String) {
        hendelse.kontekst(this)
        hendelse.info(melding)
    }

    internal fun invaliderAllePerioder(hendelse: IAktivitetslogg, feilmelding: String?) {
        feilmelding?.also(hendelse::error)
        arbeidsgivere.forEach { it.søppelbøtte(hendelse, ALLE, ForkastetÅrsak.IKKE_STØTTET) }
    }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        finnEllerOpprettArbeidsgiver(hendelse.organisasjonsnummer(), hendelse)

    private fun finnEllerOpprettArbeidsgiver(orgnummer: String, aktivitetslogg: IAktivitetslogg) =
        arbeidsgivere.finnEllerOpprett(orgnummer) {
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnummer)
            Arbeidsgiver(this, orgnummer)
        }

    private fun finnArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finn(orgnr) ?: hendelse.severe("Finner ikke arbeidsgiver")
        }

    private fun MutableList<Arbeidsgiver>.finn(orgnr: String) = find { it.organisasjonsnummer() == orgnr }

    private fun MutableList<Arbeidsgiver>.finnEllerOpprett(orgnr: String, creator: () -> Arbeidsgiver) =
        finn(orgnr) ?: run {
            val newValue = creator()
            add(newValue)
            newValue
        }

    internal fun nåværendeVedtaksperioder(filter: VedtaksperiodeFilter) = arbeidsgivere.nåværendeVedtaksperioder(filter).sorted()

    /**
     * Brukes i MVP for flere arbeidsgivere. Alle forlengelser hos alle arbeidsgivere må gjelde samme periode
     * */
    internal fun forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.forlengerSammePeriode(vedtaksperiode)

    internal fun forlengerIkkeBareAnnenArbeidsgiver(arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.forlengerIkkeBareAnnenArbeidsgiver(arbeidsgiver, vedtaksperiode)

    internal fun harOverlappendePeriodeHosAnnenArbeidsgiver(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.antallArbeidsgivereMedOverlappendeVedtaksperioder(vedtaksperiode) > 1

    internal fun antallArbeidsgivereMedOverlappendeVedtaksperioder(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.antallArbeidsgivereMedOverlappendeVedtaksperioder(vedtaksperiode)

    internal fun lagreDødsdato(dødsdato: LocalDate) {
        this.dødsdato = dødsdato
    }

    internal fun lagreSykepengegrunnlagFraInfotrygd(
        orgnummer: String,
        arbeidsgiverInntekt: ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        hendelse: PersonHendelse
    ) {
        finnArbeidsgiverForInntekter(orgnummer, hendelse).lagreSykepengegrunnlag(arbeidsgiverInntekt, skjæringstidspunkt, hendelse)
    }

    internal fun lagreSammenligningsgrunnlag(
        orgnummer: String,
        arbeidsgiverInntekt: ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        hendelse: PersonHendelse
    ) {
        finnArbeidsgiverForInntekter(orgnummer, hendelse).lagreSammenligningsgrunnlag(arbeidsgiverInntekt, skjæringstidspunkt, hendelse)
    }

    internal fun lagreSykepengegrunnlagFraInfotrygd(
        orgnummer: String,
        inntektsopplysninger: List<Inntektsopplysning>,
        aktivitetslogg: IAktivitetslogg,
        hendelseId: UUID
    ) {
        finnArbeidsgiverForInntekter(orgnummer, aktivitetslogg).lagreSykepengegrunnlagFraInfotrygd(inntektsopplysninger, hendelseId)
    }

    internal fun sykepengegrunnlag(skjæringstidspunkt: LocalDate, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden: LocalDate) =
        grunnlagForSykepengegrunnlag(skjæringstidspunkt, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden)
            ?.let { grunnlagForSykepengegrunnlag ->
                minOf(grunnlagForSykepengegrunnlag, Grunnbeløp.`6G`.beløp(skjæringstidspunkt, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden))
            }

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden: LocalDate) =
        arbeidsgivere.grunnlagForSykepengegrunnlag(skjæringstidspunkt, personensSisteKjenteSykedagIDenSammenhengdendeSykeperioden)

    internal fun sammenligningsgrunnlag(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.grunnlagForSammenligningsgrunnlag(skjæringstidspunkt)

    private fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.grunnlagForSykepengegrunnlag(skjæringstidspunkt)

    private fun finnArbeidsgiverForInntekter(arbeidsgiver: String, aktivitetslogg: IAktivitetslogg): Arbeidsgiver {
        return arbeidsgivere.finnEllerOpprett(arbeidsgiver) {
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", arbeidsgiver)
            Arbeidsgiver(this, arbeidsgiver)
        }
    }

    internal fun kanRevurdereInntekt(skjæringstidspunkt: LocalDate) =
        sammenligningsgrunnlag(skjæringstidspunkt) != null && grunnlagForSykepengegrunnlag(skjæringstidspunkt) != null && vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) != null

    internal fun harNødvendigInntekt(skjæringstidspunkt: LocalDate) = arbeidsgivere.harNødvendigInntekt(skjæringstidspunkt)

    internal fun harFlereArbeidsgivereMedSykdom() = arbeidsgivereMedSykdom().count() > 1

    private fun arbeidsgivereMedSykdom() = arbeidsgivere.filter(Arbeidsgiver::harSykdom)

    internal fun minimumInntekt(skjæringstidspunkt: LocalDate): Inntekt = Alder(fødselsnummer).minimumInntekt(skjæringstidspunkt)

    internal fun kunOvergangFraInfotrygd(vedtaksperiode: Vedtaksperiode) =
        Arbeidsgiver.kunOvergangFraInfotrygd(arbeidsgivere, vedtaksperiode)

    internal fun ingenUkjenteArbeidsgivere(vedtaksperiode: Vedtaksperiode, skjæringstidspunkt: LocalDate) =
        Arbeidsgiver.ingenUkjenteArbeidsgivere(arbeidsgivere, vedtaksperiode, infotrygdhistorikk, skjæringstidspunkt)

    internal fun søppelbøtte(hendelse: IAktivitetslogg, periode: Periode) {
        infotrygdhistorikk.tøm()
        arbeidsgivere.forEach { it.søppelbøtte(hendelse, it.tidligereOgEttergølgende(periode), ForkastetÅrsak.IKKE_STØTTET) }
    }

    internal fun oppdaterHarMinimumInntekt(skjæringstidspunkt: LocalDate, grunnlagsdata: VilkårsgrunnlagHistorikk.Grunnlagsdata) {
        val harMinimumInntekt = grunnlagForSykepengegrunnlag(skjæringstidspunkt)?.let { it > Alder(fødselsnummer).minimumInntekt(skjæringstidspunkt) } ?: false
        val grunnlagsdataMedHarMinimumInntekt = grunnlagsdata.grunnlagsdataMedMinimumInntektsvurdering(harMinimumInntekt)
        vilkårsgrunnlagHistorikk.lagre(skjæringstidspunkt, grunnlagsdataMedHarMinimumInntekt)
    }

    internal fun warningsFraVilkårsgrunnlag(vilkårsgrunnlagId: UUID): List<Aktivitetslogg.Aktivitet> {
        val aktiviteter = mutableListOf<Aktivitetslogg.Aktivitet>()
        aktivitetslogg.accept(object : AktivitetsloggVisitor {
            override fun visitWarn(
                kontekster: List<SpesifikkKontekst>,
                aktivitet: Aktivitetslogg.Aktivitet.Warn,
                melding: String,
                tidsstempel: String
            ) {
                kontekster.filter { it.kontekstType == "Vilkårsgrunnlag" }
                    .map { it.kontekstMap["meldingsreferanseId"] }
                    .map(UUID::fromString)
                    .find { it == vilkårsgrunnlagId }
                    ?.also { aktiviteter.add(aktivitet) }
            }
        })
        return aktiviteter
    }

    internal fun førerIkkeTilVidereBehandling(hendelse: PersonHendelse) {
        observers.forEach { it.hendelseIkkeHåndtert(PersonObserver.HendelseIkkeHåndtertEvent(hendelse.meldingsreferanseId())) }
    }

    internal fun harArbeidsgivereMedOverlappendeUtbetaltePerioder(organisasjonsnummer: String, periode: Periode) =
        arbeidsgivere.harArbeidsgivereMedOverlappendeUtbetaltePerioder(organisasjonsnummer, periode)

    internal fun lagreArbeidsforhold(orgnummer: String, arbeidsforhold: List<Arbeidsforhold>, aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate) {
        finnEllerOpprettArbeidsgiver(orgnummer, aktivitetslogg).lagreArbeidsforhold(arbeidsforhold, skjæringstidspunkt)
    }

    internal fun brukOuijaBrettForÅKommunisereMedPotensielleSpøkelser(orgnummerFraAAreg: List<String>, skjæringstidspunkt: LocalDate) {
        val arbeidsgivereMedSykdom = arbeidsgivere.filter { it.harSykdomFor(skjæringstidspunkt) }.map(Arbeidsgiver::organisasjonsnummer)
        if (arbeidsgivereMedSykdom.containsAll(orgnummerFraAAreg)) {
            sikkerLogg.info("Ingen spøkelser, har sykdom hos alle kjente arbeidsgivere antall=${arbeidsgivereMedSykdom.size}")
        } else {
            sikkerLogg.info("Vi har kontakt med spøkelser, fnr=$fødselsnummer, antall=${orgnummerFraAAreg.size}")
        }
    }

    internal fun loggUkjenteOrgnummere(orgnummerFraAAreg: List<String>) {
        val kjenteOrgnummer = arbeidsgivereMedSykdom().map { it.organisasjonsnummer() }
            .filter { it != "0" }
        val orgnummerMedSpleisSykdom = arbeidsgivere.filter { it.harSpleisSykdom() }.map { it.organisasjonsnummer() }

        val manglerIAAReg = kjenteOrgnummer.filter { !orgnummerFraAAreg.contains(it) }
        val spleisOrgnummerManglerIAAreg = kjenteOrgnummer.filter { !orgnummerMedSpleisSykdom.contains(it) }
        val nyeOrgnummer = orgnummerFraAAreg.filter { !kjenteOrgnummer.contains(it) }
        if (spleisOrgnummerManglerIAAreg.isNotEmpty()) {
            sikkerLogg.info("Fant arbeidsgivere i spleis som ikke er i AAReg(${manglerIAAReg}), opprettet(${nyeOrgnummer}) for $fødselsnummer")
        } else if (manglerIAAReg.isNotEmpty()) {
            sikkerLogg.info("Fant arbeidsgivere i IT som ikke er i AAReg(${manglerIAAReg}), opprettet(${nyeOrgnummer}) for $fødselsnummer")
        } else {
            sikkerLogg.info("AAReg kjenner til alle arbeidsgivere i spleis, opprettet (${nyeOrgnummer}) for $fødselsnummer")
        }
    }

    internal fun fyllUtPeriodeMedForventedeDager(hendelse: PersonHendelse, periode: Periode, skjæringstidspunkt: LocalDate) {
        arbeidsgivereMedAktiveArbeidsforhold(skjæringstidspunkt)
            .harGrunnlagForSykepengegrunnlag(skjæringstidspunkt)
            .forEach { it.fyllUtPeriodeMedForventedeDager(hendelse, periode) }
    }

    internal fun harFlereArbeidsgivereUtenSykdomVedSkjæringstidspunkt(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.relevanteArbeidsforhold(skjæringstidspunkt).any { it.grunnlagForSykepengegrunnlagKommerFraSkatt(skjæringstidspunkt) }

    private fun arbeidsgivereMedAktiveArbeidsforhold(skjæringstidspunkt: LocalDate): List<Arbeidsgiver> =
        arbeidsgivere.filter { it.harAktivtArbeidsforhold(skjæringstidspunkt) }

    internal fun harVedtaksperiodeForArbeidsgiverMedUkjentArbeidsforhold(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.any { it.harVedtaksperiodeMedUkjentArbeidsforhold(skjæringstidspunkt) }

    internal fun orgnummereMedAktiveArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsgivere
        .filter { it.harAktivtArbeidsforhold(skjæringstidspunkt) }
        .map { it.organisasjonsnummer() }

    internal fun harAktivtArbeidsforholdEllerInntekt(skjæringstidspunkt: LocalDate, orgnummer: String) = arbeidsgivere
        .firstOrNull { it.organisasjonsnummer() == orgnummer }
        ?.let { it.harAktivtArbeidsforhold(skjæringstidspunkt) || it.harGrunnlagForSykepengegrunnlagEllerSammenligningsgrunnlag(skjæringstidspunkt) } ?: false

    internal fun harKunEtAnnetAktivtArbeidsforholdEnn(skjæringstidspunkt: LocalDate, orgnummer: String): Boolean {
        val aktiveArbeidsforhold = arbeidsgivereMedAktiveArbeidsforhold(skjæringstidspunkt)
        return aktiveArbeidsforhold.size == 1 && aktiveArbeidsforhold.single().organisasjonsnummer() != orgnummer
    }

    internal fun vilkårsprøvEtterNyInntekt(hendelse: OverstyrInntekt) {
        val skjæringstidspunkt = hendelse.skjæringstidspunkt
        val grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag(skjæringstidspunkt)
            ?: hendelse.severe("Fant ikke grunnlag for sykepengegrunnlag for skjæringstidspunkt: ${skjæringstidspunkt}. Kan ikke revurdere inntekt.")
        val sammenligningsgrunnlag = sammenligningsgrunnlag(skjæringstidspunkt)
            ?: hendelse.severe("Fant ikke sammenligningsgrunnlag for skjæringstidspunkt: ${skjæringstidspunkt}. Kan ikke revurdere inntekt.")
        val avviksprosent = grunnlagForSykepengegrunnlag.avviksprosent(sammenligningsgrunnlag)
        val akseptabeltAvvik = avviksprosent <= Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT

        hendelse.etterlevelse.`§8-30 ledd 2`(
            akseptabeltAvvik,
            Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT,
            grunnlagForSykepengegrunnlag,
            sammenligningsgrunnlag,
            avviksprosent
        )

        if (akseptabeltAvvik) {
            hendelse.info(
                "Har %.0f %% eller mindre avvik i inntekt (%.2f %%)",
                Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent(),
                avviksprosent.prosent()
            )
        } else {
            hendelse.warn("Har mer enn %.0f %% avvik", Prosent.MAKSIMALT_TILLATT_AVVIK_PÅ_ÅRSINNTEKT.prosent())
        }

        when (val grunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt)) {
            is VilkårsgrunnlagHistorikk.Grunnlagsdata -> {
                val harMinimumInntekt =
                    validerMinimumInntekt(hendelse, fødselsnummer, hendelse.skjæringstidspunkt, grunnlagForSykepengegrunnlag)
                vilkårsgrunnlagHistorikk.lagre( //TODO: Se i debugger
                    skjæringstidspunkt, grunnlag.kopierGrunnlagsdataMed(
                        minimumInntektVurdering = harMinimumInntekt,
                        sammenligningsgrunnlagVurdering = akseptabeltAvvik,
                        sammenligningsgrunnlag = sammenligningsgrunnlag,
                        avviksprosent = avviksprosent,
                        meldingsreferanseId = hendelse.meldingsreferanseId()
                    )
                )
            }
            is VilkårsgrunnlagHistorikk.InfotrygdVilkårsgrunnlag -> hendelse.error("Vilkårsgrunnlaget ligger i infotrygd. Det er ikke støttet i revurdering.")
            else -> hendelse.error("Fant ikke vilkårsgrunnlag. Kan ikke revurdere inntekt.")
        }
    }

    internal fun harRelevanteArbeidsforholdForFlereArbeidsgivere(skjæringstidspunkt: LocalDate) =
        arbeidsgivere.harRelevanteArbeidsforholdForFlereArbeidsgivere(skjæringstidspunkt)
}
