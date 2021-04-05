package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.hendelser.*
import no.nav.helse.person.Arbeidsgiver.Companion.forlengerIkkeBareAnnenArbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.forlengerSammePeriode
import no.nav.helse.person.Arbeidsgiver.Companion.grunnlagForSammenligningsgrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.grunnlagForSykepengegrunnlag
import no.nav.helse.person.Arbeidsgiver.Companion.harNødvendigInntekt
import no.nav.helse.person.Arbeidsgiver.Companion.harOverlappendePeriodeHosAnnenArbeidsgiver
import no.nav.helse.person.Arbeidsgiver.Companion.nåværendeVedtaksperioder
import no.nav.helse.person.infotrygdhistorikk.Infotrygdhistorikk
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.utbetalingstidslinje.Alder
import no.nav.helse.utbetalingstidslinje.Historie
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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

    constructor(
        aktørId: String,
        fødselsnummer: String
    ) : this(aktørId, fødselsnummer, mutableListOf(), Aktivitetslogg(), LocalDateTime.now(), Infotrygdhistorikk(), VilkårsgrunnlagHistorikk(), null)

    private val observers = mutableListOf<PersonObserver>()

    internal fun historie() = Historie(this, infotrygdhistorikk)

    fun håndter(sykmelding: Sykmelding) = håndter(sykmelding, "sykmelding") { sykmelding.forGammel() }

    fun håndter(søknad: Søknad) = håndter(søknad, "søknad")

    fun håndter(søknad: SøknadArbeidsgiver) = håndter(søknad, "søknad til arbeidsgiver")

    fun håndter(inntektsmelding: Inntektsmelding) = håndter(inntektsmelding, "inntektsmelding")

    fun håndter(inntektsmelding: InntektsmeldingReplay) = håndter(inntektsmelding, "replay av inntektsmelding")

    private fun håndter(
        hendelse: SykdomstidslinjeHendelse,
        hendelsesmelding: String,
        avvisIf: () -> Boolean = { false }
    ) {
        registrer(hendelse, "Behandler $hendelsesmelding")
        if (avvisIf()) return
        val arbeidsgiver = finnEllerOpprettArbeidsgiver(hendelse)
        hendelse.fortsettÅBehandle(arbeidsgiver)
    }

    fun håndter(utbetalingshistorikk: Utbetalingshistorikk) {
        utbetalingshistorikk.kontekst(this)
        utbetalingshistorikk.oppdaterHistorikk(infotrygdhistorikk)
        finnArbeidsgiver(utbetalingshistorikk).håndter(utbetalingshistorikk, infotrygdhistorikk)
    }

    fun håndter(ytelser: Ytelser) {
        registrer(ytelser, "Behandler historiske utbetalinger og inntekter")
        ytelser.oppdaterHistorikk(infotrygdhistorikk)
        ytelser.lagreDødsdato(this)
        finnArbeidsgiver(ytelser).håndter(ytelser, infotrygdhistorikk, dødsdato)
    }

    fun håndter(utbetalingsgodkjenning: Utbetalingsgodkjenning) {
        registrer(utbetalingsgodkjenning, "Behandler utbetalingsgodkjenning")
        finnArbeidsgiver(utbetalingsgodkjenning).håndter(utbetalingsgodkjenning)
    }

    fun håndter(vilkårsgrunnlag: Vilkårsgrunnlag) {
        registrer(vilkårsgrunnlag, "Behandler vilkårsgrunnlag")
        finnArbeidsgiver(vilkårsgrunnlag).håndter(vilkårsgrunnlag)
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

    fun annullert(event: PersonObserver.UtbetalingAnnullertEvent) {
        observers.forEach { it.annullering(event) }
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

    fun vedtaksperiodeUtbetalt(event: PersonObserver.UtbetaltEvent) {
        observers.forEach { it.vedtaksperiodeUtbetalt(event) }
    }

    fun vedtaksperiodeEndret(event: PersonObserver.VedtaksperiodeEndretTilstandEvent) {
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

    internal fun vedtakFattet(
        vedtaksperiodeId: UUID,
        periode: Periode,
        hendelseIder: List<UUID>,
        skjæringstidspunkt: LocalDate,
        sykepengegrunnlag: Inntekt,
        inntekt: Inntekt,
        utbetalingId: UUID?
    ) {
        observers.forEach {
            it.vedtakFattet(
                vedtaksperiodeId,
                periode,
                hendelseIder,
                skjæringstidspunkt,
                sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
                inntekt.reflection { _, månedlig, _, _ -> månedlig },
                utbetalingId
            )
        }
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

    internal fun trengerHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse, cutoff: LocalDateTime? = null): Boolean {
        val tidligsteDato = arbeidsgivereMedSykdom().minOf { it.sykdomstidslinje().førsteDag() }
        return infotrygdhistorikk.oppfriskNødvendig(hendelse, tidligsteDato, cutoff)
    }

    internal fun trengerHistorikkFraInfotrygd(hendelse: ArbeidstakerHendelse, vedtaksperiode: Vedtaksperiode, cutoff: LocalDateTime? = null) {
        if (trengerHistorikkFraInfotrygd(hendelse, cutoff)) return hendelse.info("Må oppfriske Infotrygdhistorikken")
        hendelse.info("Trenger ikke oppfriske Infotrygdhistorikken, bruker lagret historikk")
        vedtaksperiode.håndterHistorikkFraInfotrygd(hendelse, infotrygdhistorikk)
    }

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

    private fun registrer(hendelse: ArbeidstakerHendelse, melding: String) {
        hendelse.kontekst(this)
        hendelse.info(melding)
    }

    internal fun invaliderAllePerioder(hendelse: ArbeidstakerHendelse, feilmelding: String?) {
        feilmelding?.also(hendelse::error)
        arbeidsgivere.forEach { it.søppelbøtte(hendelse, Arbeidsgiver.ALLE, ForkastetÅrsak.IKKE_STØTTET) }
    }

    private fun finnEllerOpprettArbeidsgiver(hendelse: ArbeidstakerHendelse) =
        hendelse.organisasjonsnummer().let { orgnr ->
            arbeidsgivere.finnEllerOpprett(orgnr) {
                hendelse.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", orgnr)
                Arbeidsgiver(this, orgnr)
            }
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

    internal fun nåværendeVedtaksperioder() = arbeidsgivere.nåværendeVedtaksperioder().sorted()

    /**
     * Brukes i MVP for flere arbeidsgivere. Alle forlengelser hos alle arbeidsgivere må gjelde samme periode
     * */
    internal fun forlengerAlleArbeidsgivereSammePeriode(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.forlengerSammePeriode(vedtaksperiode)

    internal fun forlengerIkkeBareAnnenArbeidsgiver(arbeidsgiver: Arbeidsgiver, vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.forlengerIkkeBareAnnenArbeidsgiver(arbeidsgiver, vedtaksperiode)

    internal fun harOverlappendePeriodeHosAnnenArbeidsgiver(vedtaksperiode: Vedtaksperiode) =
        arbeidsgivere.harOverlappendePeriodeHosAnnenArbeidsgiver(vedtaksperiode)

    internal fun lagreDødsdato(dødsdato: LocalDate) {
        this.dødsdato = dødsdato
    }

    internal fun lagreInntekter(
        orgnummer: String,
        arbeidsgiverInntekt: Inntektsvurdering.ArbeidsgiverInntekt,
        skjæringstidspunkt: LocalDate,
        vilkårsgrunnlag: Vilkårsgrunnlag
    ) {
        finnArbeidsgiverForInntekter(orgnummer, vilkårsgrunnlag).lagreInntekter(
            arbeidsgiverInntekt,
            skjæringstidspunkt,
            vilkårsgrunnlag
        )
    }

    internal fun lagreInntekter(
        orgnummer: String,
        inntektsopplysninger: List<Inntektsopplysning>,
        aktivitetslogg: IAktivitetslogg,
        hendelseId: UUID
    ) {
        finnArbeidsgiverForInntekter(orgnummer, aktivitetslogg).lagreInntekter(inntektsopplysninger, hendelseId)
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

    internal fun append(bøtte: Historie.Historikkbøtte) {
        arbeidsgivere.forEach { it.append(bøtte) }
    }

    internal fun utbetalingstidslinjer(periode: Periode, historie: Historie) =
        arbeidsgivereMedSykdom()
            .map { arbeidsgiver -> arbeidsgiver to arbeidsgiver.oppdatertUtbetalingstidslinje(periode, historie, infotrygdhistorikk) }
            .toMap()

    private fun finnArbeidsgiverForInntekter(arbeidsgiver: String, aktivitetslogg: IAktivitetslogg): Arbeidsgiver {
        return arbeidsgivere.finnEllerOpprett(arbeidsgiver) {
            aktivitetslogg.info("Ny arbeidsgiver med organisasjonsnummer %s for denne personen", arbeidsgiver)
            Arbeidsgiver(this, arbeidsgiver)
        }
    }

    internal fun harNødvendigInntekt(skjæringstidspunkt: LocalDate) = arbeidsgivere.harNødvendigInntekt(skjæringstidspunkt)

    internal fun harFlereArbeidsgivereMedSykdom() = arbeidsgivereMedSykdom().count() > 1

    private fun arbeidsgivereMedSykdom() = arbeidsgivere.filter(Arbeidsgiver::harSykdom)

    internal fun minimumInntekt(skjæringstidspunkt: LocalDate): Inntekt = Alder(fødselsnummer).minimumInntekt(skjæringstidspunkt)

    internal fun harForlengelseForAlleArbeidsgivereIInfotrygdhistorikken(historie: Historie, vedtaksperiode: Vedtaksperiode) =
        Arbeidsgiver.harForlengelseForAlleArbeidsgivereIInfotrygdhistorikken(arbeidsgivere, historie, vedtaksperiode)

    internal fun søppelbøtte(hendelse: ArbeidstakerHendelse, periode: Periode) {
        infotrygdhistorikk.tøm()
        arbeidsgivere.forEach { it.søppelbøtte(hendelse, it.tidligereOgEttergølgende(periode), ForkastetÅrsak.IKKE_STØTTET) }
    }
}
