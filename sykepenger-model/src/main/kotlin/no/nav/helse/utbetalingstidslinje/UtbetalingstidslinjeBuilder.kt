package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Dag as Beløpstidslinjedag
import no.nav.helse.person.beløp.UkjentDag
import no.nav.helse.person.inntekt.Refusjonsopplysning
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi
import org.slf4j.LoggerFactory

internal class VilkårsprøvdSkjæringstidspunkt(
    private val skjæringstidspunkt: LocalDate,
    private val `6G`: Inntekt,
    inntekter: List<FaktaavklartInntekt>,
    val tilkommendeInntekter: List<NyInntektUnderveis>,
    val deaktiverteArbeidsforhold: List<String>
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

    internal fun medGhostOgNyeInntekterUnderveis(utbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        return nyeInntekterUnderveis(ghosttidslinjer(utbetalingstidslinjer))
    }

    private fun ghosttidslinjer(utbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        val beregningsperiode = utbetalingstidslinjer.values.flatten().map { it.periode() }.periode()!!
        return inntekter
            .mapValues { (orgnr, v) -> v.ghosttidslinje(beregningsperiode, skjæringstidspunkt, `6G`, utbetalingstidslinjer[orgnr] ?: emptyList()) }
            .filterValues { it.isNotEmpty() }
    }

    private fun nyeInntekterUnderveis(utbetalingstidslinjer: Map<String, Utbetalingstidslinje>): Map<String, Utbetalingstidslinje> {
        val beregningsperiode = utbetalingstidslinjer.values.map { it.periode() }.periode()!!
        val tilkommendeInntekterTidslinje = tilkommendeInntekter.associate { nyInntekt ->
            val tilkommenInntektTidslinje = Utbetalingstidslinje.Builder().apply {
                beregningsperiode.forEach { dato ->
                    when (val beløpsdag = nyInntekt.beløpstidslinje[dato]) {
                        is Beløpsdag -> {
                            addArbeidsdag(
                                dato, Økonomi.ikkeBetalt().inntekt(
                                aktuellDagsinntekt = beløpsdag.beløp,
                                beregningsgrunnlag = INGEN,
                                `6G` = `6G`,
                                refusjonsbeløp = INGEN
                            )
                            )
                        }

                        is UkjentDag -> {
                            addArbeidsdag(dato, Økonomi.ikkeBetalt())
                        }
                    }
                }
            }.build()
            nyInntekt.orgnummer to tilkommenInntektTidslinje
        }
        // hvis vi skal kunne ha søknad og tilkommen inntekt for en og samme arbeidsgiver så må vi
        // gjøre en litt bedre merging enn Map.plus() her :)
        return utbetalingstidslinjer + tilkommendeInntekterTidslinje
    }

    data class FaktaavklartInntekt(
        val organisasjonsnummer: String,
        val fastsattÅrsinntekt: Inntekt,
        val gjelder: Periode,
        val refusjonsopplysninger: Refusjonsopplysning.Refusjonsopplysninger
    )

    data class NyInntektUnderveis(
        val orgnummer: String,
        val beløpstidslinje: Beløpstidslinje
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
        refusjon: Beløpstidslinjedag
    ): Økonomi {
        return medInntekt(dato, økonomi, regler, refusjon, lagDefaultRefusjonsbeløpHvisMangler, mutableListOf())
    }

    internal fun medInntektOrThrow(
        dato: LocalDate,
        økonomi: Økonomi,
        regler: ArbeidsgiverRegler,
        refusjon: Beløpstidslinjedag,
        forskjeller: MutableList<String>,
    ): Økonomi {
        return medInntekt(dato, økonomi, regler, refusjon, krevRefusjonsbeløpHvisMangler, forskjeller)
    }

    private fun medInntekt(
        dato: LocalDate,
        økonomi: Økonomi,
        regler: ArbeidsgiverRegler,
        refusjon: Beløpstidslinjedag,
        refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt,
        forskjeller: MutableList<String>
    ): Økonomi {
        val aktuellDagsinntekt = fastsattÅrsinntekt(dato)
        return økonomi.inntekt(
            aktuellDagsinntekt = aktuellDagsinntekt,
            beregningsgrunnlag = beregningsgrunnlag(skjæringstidspunkt),
            dekningsgrunnlag = aktuellDagsinntekt * regler.dekningsgrad(),
            `6G` = if (dato < skjæringstidspunkt) INGEN else `6G`,
            refusjonsbeløp = refusjonsbeløp(dato, refusjon, aktuellDagsinntekt, refusjonsopplysningFinnesIkkeStrategi, forskjeller)
        )
    }

    private fun refusjonsbeløp(dato: LocalDate, refusjon: Beløpstidslinjedag, aktuellDagsinntekt: Inntekt, refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt, forskjeller: MutableList<String>): Inntekt {
        val refusjonFraInntektsgrunnlag = refusjonsopplysninger.refusjonsbeløpOrNull(dato)
        val refusjonFraBehandling = refusjon.takeIf { it is Beløpsdag }?.beløp
        return (refusjonFraBehandling ?: refusjonsopplysningFinnesIkkeStrategi(dato, aktuellDagsinntekt)).also {
            if (it.dagligInt != refusjonFraInntektsgrunnlag?.dagligInt) forskjeller.add("$dato: Brukte ${it.dagligInt} fra behandlingen. Hadde ${refusjonFraInntektsgrunnlag?.dagligInt} fra inntektsgrunnlaget.")
        }
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
                    else addArbeidsdag(
                        dag, Økonomi.ikkeBetalt().inntekt(
                        aktuellDagsinntekt = aktuellDagsinntekt,
                        beregningsgrunnlag = beregningsgrunnlag(skjæringstidspunkt),
                        dekningsgrunnlag = INGEN,
                        `6G` = `6G`,
                        refusjonsbeløp = INGEN
                    )
                    )
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
    private val arbeidsgiverperiode: List<Periode>,
    private val refusjonstidslinje: Beløpstidslinje
) {
    val forskjeller = mutableListOf<String>()

    internal fun result(sykdomstidslinje: Sykdomstidslinje): Utbetalingstidslinje {
        val builder = Utbetalingstidslinje.Builder()
        sykdomstidslinje.forEach { dag ->
            when (dag) {
                /** <potensielt arbeidsgiverperiode-dager> **/
                is Dag.ArbeidsgiverHelgedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.økonomi)
                    else helg(builder, dag.dato, dag.økonomi)
                }

                is Dag.Arbeidsgiverdag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.økonomi)
                    else avvistDag(builder, dag.dato, dag.økonomi.ikkeBetalt(), Begrunnelse.EgenmeldingUtenforArbeidsgiverperiode)
                }

                is Dag.Sykedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.økonomi)
                    else navDag(builder, dag.dato, dag.økonomi)
                }

                is Dag.SykHelgedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.økonomi)
                    else helg(builder, dag.dato, dag.økonomi)
                }

                is Dag.SykedagNav -> {
                    if (erAGP(dag.dato)) builder.addArbeidsgiverperiodedagNav(dag.dato, faktaavklarteInntekter.medInntektOrThrow(dag.dato, dag.økonomi, regler, refusjonstidslinje[dag.dato], forskjeller))
                    else when (dag.dato.erHelg()) {
                        true -> helg(builder, dag.dato, dag.økonomi)
                        false -> navDag(builder, dag.dato, dag.økonomi)
                    }
                }

                is Dag.AndreYtelser -> {
                    // andreytelse-dagen er fridag hvis den overlapper med en agp-dag, eller om vedtaksperioden ikke har noen agp -- fordi andre ytelsen spiser opp alt
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, Økonomi.ikkeBetalt())
                    else if (arbeidsgiverperiode.isEmpty() || dag.dato < arbeidsgiverperiode.first().start) fridag(builder, dag.dato)
                    else {
                        val begrunnelse = when (dag.ytelse) {
                            Dag.AndreYtelser.AnnenYtelse.AAP -> Begrunnelse.AndreYtelserAap
                            Dag.AndreYtelser.AnnenYtelse.Dagpenger -> Begrunnelse.AndreYtelserDagpenger
                            Dag.AndreYtelser.AnnenYtelse.Foreldrepenger -> Begrunnelse.AndreYtelserForeldrepenger
                            Dag.AndreYtelser.AnnenYtelse.Omsorgspenger -> Begrunnelse.AndreYtelserOmsorgspenger
                            Dag.AndreYtelser.AnnenYtelse.Opplæringspenger -> Begrunnelse.AndreYtelserOpplaringspenger
                            Dag.AndreYtelser.AnnenYtelse.Pleiepenger -> Begrunnelse.AndreYtelserPleiepenger
                            Dag.AndreYtelser.AnnenYtelse.Svangerskapspenger -> Begrunnelse.AndreYtelserSvangerskapspenger
                        }
                        avvistDag(builder, dag.dato, Økonomi.ikkeBetalt(), begrunnelse)
                    }
                }

                is Dag.Feriedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, Økonomi.ikkeBetalt())
                    else fridag(builder, dag.dato)
                }

                is Dag.ForeldetSykedag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.økonomi)
                    else builder.addForeldetDag(dag.dato, faktaavklarteInntekter.medInntektHvisFinnes(dag.dato, dag.økonomi, regler, refusjonstidslinje[dag.dato]))
                }

                is Dag.ArbeidIkkeGjenopptattDag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, Økonomi.ikkeBetalt())
                    else fridag(builder, dag.dato)
                }

                is Dag.Permisjonsdag -> {
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, Økonomi.ikkeBetalt())
                    else fridag(builder, dag.dato)
                }
                /** </potensielt arbeidsgiverperiode-dager> **/

                is Dag.Arbeidsdag -> arbeidsdag(builder, dag.dato)
                is Dag.FriskHelgedag -> arbeidsdag(builder, dag.dato)
                is Dag.ProblemDag -> {
                    // den andre builderen kaster egentlig exception her, men trenger vi det –– sånn egentlig?
                    fridag(builder, dag.dato)
                }

                is Dag.UkjentDag -> {
                    // todo: pga strekking av egenmeldingsdager fra søknad så har vi vedtaksperioder med ukjentdager
                    // error("Forventer ikke å finne en ukjentdag i en vedtaksperiode")
                    when (dag.dato.erHelg()) {
                        true -> fridag(builder, dag.dato)
                        false -> arbeidsdag(builder, dag.dato)
                    }
                }
            }
        }
        if (forskjeller.isNotEmpty()) {
            sikkerlogger.info("Refusjonsoppsummering:\n\t${forskjeller.joinToString("\n\t")}")
        }
        return builder.build()
    }

    private fun erAGP(dato: LocalDate) = arbeidsgiverperiode.any { dato in it }
    private fun arbeidsgiverperiodedag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi) {
        builder.addArbeidsgiverperiodedag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi.ikkeBetalt(), regler, refusjonstidslinje[dato]))
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi, regler, refusjonstidslinje[dato]), listOf(begrunnelse))
    }

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi) {
        builder.addHelg(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, økonomi.ikkeBetalt(), regler, refusjonstidslinje[dato]))
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi) {
        builder.addNAVdag(dato, faktaavklarteInntekter.medInntektOrThrow(dato, økonomi, regler, refusjonstidslinje[dato], forskjeller))
    }

    private fun fridag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addFridag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, Økonomi.ikkeBetalt(), regler, refusjonstidslinje[dato]))
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addArbeidsdag(dato, faktaavklarteInntekter.medInntektHvisFinnes(dato, Økonomi.ikkeBetalt(), regler, refusjonstidslinje[dato]))
    }

    private companion object {
        private val sikkerlogger = LoggerFactory.getLogger("tjenestekall")
    }
}
