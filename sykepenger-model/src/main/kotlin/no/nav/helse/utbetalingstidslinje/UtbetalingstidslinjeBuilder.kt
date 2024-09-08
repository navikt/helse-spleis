package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.etterlevelse.Subsumsjonslogg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.SykdomstidslinjeVisitor
import no.nav.helse.person.aktivitetslogg.IAktivitetslogg
import no.nav.helse.person.aktivitetslogg.Varselkode.RV_UT_3
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.SykdomshistorikkHendelse.Hendelseskilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

internal sealed class UtbetalingstidslinjeBuilderException(message: String) : RuntimeException(message) {
    internal fun logg(aktivitetslogg: IAktivitetslogg) {
        aktivitetslogg.info("Feilmelding: $message")
        aktivitetslogg.funksjonellFeil(RV_UT_3)
    }

    internal class ProblemdagException(melding: String) : UtbetalingstidslinjeBuilderException(
        "Forventet ikke ProblemDag i utbetalingstidslinjen. Melding: $melding"
    )
}

internal class VilkårsprøvdSkjæringstidspunkt(
    private val skjæringstidspunkt: LocalDate,
    private val `6G`: Inntekt,
    inntekter: List<FaktaavklartInntekt>
) {
    private val inntekter = inntekter.associate { inntekt ->
        inntekt.organisasjonsnummer to ArbeidsgiverFaktaavklartInntekt(
            skjæringstidspunkt = skjæringstidspunkt,
            `6G` = `6G`,
            fastsattÅrsinntekt = inntekt.fastsattÅrsinntekt,
            gjelder = inntekt.gjelder,
            refusjonsopplysninger = inntekt.refusjonsopplysninger
        )
    }
    internal fun forArbeidsgiver(organisasjonsnummer: String): ArbeidsgiverFaktaavklartInntekt? {
        return inntekter[organisasjonsnummer]
    }

    internal fun ghosttidslinjer(utbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        val beregningsperiode = utbetalingstidslinjer.values.flatten().map { it.periode() }.periode()!!
        return inntekter.mapValues { (orgnr, v) ->
            v.ghosttidslinje(beregningsperiode, skjæringstidspunkt, `6G`, utbetalingstidslinjer[orgnr] ?: emptyList())
        }
    }

    internal class FaktaavklartInntekt(
        val organisasjonsnummer: String,
        val fastsattÅrsinntekt: Inntekt,
        val gjelder: Periode,
        val refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger
    )
}

internal class ArbeidsgiverFaktaavklartInntekt(
    private val skjæringstidspunkt: LocalDate,
    private val `6G`: Inntekt,
    private val fastsattÅrsinntekt: Inntekt,
    private val gjelder: Periode,
    private val refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger
) {
    private val lagDefaultRefusjonsbeløpHvisMangler = { _: LocalDate, aktuellDagsinntekt: Inntekt -> aktuellDagsinntekt }
    private val krevRefusjonsbeløpHvisMangler = { dato: LocalDate, _: Inntekt ->
        error("Har ingen refusjonsopplysninger på vilkårsgrunnlag for utbetalingsdag $dato")
    }

    private fun fastsattÅrsinntekt(dagen: LocalDate): Inntekt {
        if (dagen !in gjelder) return INGEN
        return fastsattÅrsinntekt
    }

    private fun beregningsgrunnlag(skjæringstidspunkt: LocalDate): Inntekt {
        if (!gjelderPåSkjæringstidspunktet(skjæringstidspunkt)) return INGEN
        return fastsattÅrsinntekt
    }
    private fun gjelderPåSkjæringstidspunktet(skjæringstidspunkt: LocalDate) = skjæringstidspunkt == gjelder.start

    internal fun medInntektHvisFinnes(
        dato: LocalDate,
        økonomi: Økonomi,
        regler: ArbeidsgiverRegler,
        subsumsjonslogg: Subsumsjonslogg
    ): Økonomi {
        return medInntekt(dato, økonomi, regler, subsumsjonslogg, lagDefaultRefusjonsbeløpHvisMangler)
    }

    internal fun medInntektEllersVarsel(
        dato: LocalDate,
        økonomi: Økonomi,
        regler: ArbeidsgiverRegler,
        subsumsjonslogg: Subsumsjonslogg,
    ): Økonomi {
        return medInntekt(dato, økonomi, regler, subsumsjonslogg, krevRefusjonsbeløpHvisMangler)
    }

    internal fun medInntekt(dato: LocalDate, økonomi: Økonomi, regler: ArbeidsgiverRegler, subsumsjonslogg: Subsumsjonslogg, refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt): Økonomi {
        val aktuellDagsinntekt = fastsattÅrsinntekt(dato)
        val refusjonsbeløp = refusjonsopplysninger.refusjonsbeløpOrNull(dato) ?: refusjonsopplysningFinnesIkkeStrategi(dato, aktuellDagsinntekt)
        return økonomi.inntekt(
            aktuellDagsinntekt = aktuellDagsinntekt,
            beregningsgrunnlag = beregningsgrunnlag(skjæringstidspunkt),
            dekningsgrunnlag = aktuellDagsinntekt.dekningsgrunnlag(dato, regler, subsumsjonslogg),
            `6G` = if (dato < skjæringstidspunkt) INGEN else `6G`,
            refusjonsbeløp = refusjonsbeløp
        )
    }

    internal fun ghosttidslinje(beregningsperiode: Periode, skjæringstidspunkt: LocalDate, `6G`: Inntekt, arbeidsgiverlinjer: List<Utbetalingstidslinje>): Utbetalingstidslinje {
        // avdekker hvilken periode det er aktuelt å lage ghost-dager i
        val aktueltGhostområde = if (gjelder.start <= beregningsperiode.endInclusive) listOf(beregningsperiode.subset(gjelder.start til LocalDate.MAX)) else emptyList()

        // fjerner perioder med registrert vedtaksperiode
        val ghostperioder = arbeidsgiverlinjer.fold(aktueltGhostområde) { result, linje ->
            result.dropLast(1) + (result.lastOrNull()?.trim(linje.periode()) ?: emptyList())
        }

        // lager faktiske ghost-tidslinjer fra brudd-periodene
        val ghosttidslinje = ghostperioder.map { periode ->
            Utbetalingstidslinje.Builder().apply {
                periode.forEach { dag ->
                    val aktuellDagsinntekt = fastsattÅrsinntekt(dag)
                    if (dag.erHelg()) addFridag(dag, Økonomi.ikkeBetalt())
                    else addArbeidsdag(dag, Økonomi.ikkeBetalt().inntekt(
                        aktuellDagsinntekt = aktuellDagsinntekt,
                        beregningsgrunnlag = beregningsgrunnlag(skjæringstidspunkt),
                        dekningsgrunnlag = INGEN,
                        `6G` = `6G`,
                        refusjonsbeløp = INGEN
                    ))
                }
            }.build()
        }
        return (ghosttidslinje + arbeidsgiverlinjer).fold(Utbetalingstidslinje(), Utbetalingstidslinje::plus)
    }
}

internal data class ArbeidsgiverperiodeForVedtaksperiode(
    val vedtaksperiode: Periode,
    val arbeidsgiverperioder: List<Periode>
)

internal class UtbetalingstidslinjeBuilderVedtaksperiode(
    private val faktaavklarteInntekter: ArbeidsgiverFaktaavklartInntekt,
    private val regler: ArbeidsgiverRegler,
    private val subsumsjonslogg: Subsumsjonslogg,
    private val arbeidsgiverperiode: List<Periode>
) : SykdomstidslinjeVisitor {
    private val builder = Utbetalingstidslinje.Builder()

    internal fun result(): Utbetalingstidslinje {
        return builder.build()
    }

    private fun erAGP(dato: LocalDate) = arbeidsgiverperiode.any { dato in it }

    private fun arbeidsgiverperiodedag(dato: LocalDate, økonomi: Økonomi) {
        builder.addArbeidsgiverperiodedag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }
    private fun avvistDag(dato: LocalDate, økonomi: Økonomi, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi, regler, subsumsjonslogg), listOf(begrunnelse))
    }
    private fun helg(dato: LocalDate, økonomi: Økonomi) {
        builder.addHelg(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }
    private fun navDag(dato: LocalDate, økonomi: Økonomi) {
        builder.addNAVdag(dato, faktaavklarteInntekter.medInntektEllersVarsel(dato, økonomi, regler, subsumsjonslogg))
    }
    private fun fridag(dato: LocalDate) {
        builder.addFridag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }
    private fun arbeidsdag(dato: LocalDate) {
        builder.addArbeidsdag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, Økonomi.ikkeBetalt(), regler, subsumsjonslogg))
    }

    /** <potensielt arbeidsgiverperiode-dager> **/
    override fun visitDag(dag: Dag.Arbeidsgiverdag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else avvistDag(dato, økonomi.ikkeBetalt(), Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
    }

    override fun visitDag(dag: Dag.ArbeidsgiverHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else helg(dato, økonomi)
    }

    override fun visitDag(dag: Dag.Sykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else navDag(dato, økonomi)
    }

    override fun visitDag(dag: Dag.SykHelgedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else helg(dato, økonomi)
    }

    override fun visitDag(dag: Dag.SykedagNav, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) builder.addArbeidsgiverperiodedagNav(dato, faktaavklarteInntekter.medInntektEllersVarsel(dato, økonomi, regler, subsumsjonslogg))
        else when (dato.erHelg()) {
            true -> helg(dato, økonomi)
            false -> navDag(dato, økonomi)
        }
    }

    override fun visitDag(dag: Dag.AndreYtelser, dato: LocalDate, kilde: Hendelseskilde, ytelse: Dag.AndreYtelser.AnnenYtelse) {
        // andreytelse-dagen er fridag hvis den overlapper med en agp-dag, eller om vedtaksperioden ikke har noen agp -- fordi andre ytelsen spiser opp alt
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else if (arbeidsgiverperiode.isEmpty() || dato < arbeidsgiverperiode.first().start) fridag(dato)
        else {
            val begrunnelse = when(ytelse) {
                Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
            }
            avvistDag(dato, Økonomi.ikkeBetalt(), begrunnelse)

        }
    }

    override fun visitDag(dag: Dag.Feriedag, dato: LocalDate, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else fridag(dato)
    }

    override fun visitDag(dag: Dag.ForeldetSykedag, dato: LocalDate, økonomi: Økonomi, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, økonomi)
        else builder.addForeldetDag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi, regler, subsumsjonslogg))
    }

    override fun visitDag(dag: Dag.ArbeidIkkeGjenopptattDag, dato: LocalDate, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else fridag(dato)
    }

    override fun visitDag(dag: Dag.Permisjonsdag, dato: LocalDate, kilde: Hendelseskilde) {
        if (erAGP(dato)) arbeidsgiverperiodedag(dato, Økonomi.ikkeBetalt())
        else fridag(dato)
    }
    /** </potensielt arbeidsgiverperiode-dager> **/

    override fun visitDag(dag: Dag.UkjentDag, dato: LocalDate, kilde: Hendelseskilde) {
        // todo: pga strekking av egenmeldingsdager fra søknad så har vi vedtaksperioder med ukjentdager
        // error("Forventer ikke å finne en ukjentdag i en vedtaksperiode")
        when (dato.erHelg()) {
            true -> fridag(dato)
            false -> arbeidsdag(dato)
        }
    }

    override fun visitDag(dag: Dag.Arbeidsdag, dato: LocalDate, kilde: Hendelseskilde) {
        arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.FriskHelgedag, dato: LocalDate, kilde: Hendelseskilde) {
        arbeidsdag(dato)
    }

    override fun visitDag(dag: Dag.ProblemDag, dato: LocalDate, kilde: Hendelseskilde, other: Hendelseskilde?, melding: String) {
        // den andre builderen kaster egentlig exception her, men trenger vi det –– sånn egentlig?
        fridag(dato)
    }
}
