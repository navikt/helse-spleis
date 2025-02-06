package no.nav.helse.utbetalingstidslinje

import java.time.LocalDate
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.periode
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.UkjentDag
import no.nav.helse.person.inntekt.Inntektstidslinje
import no.nav.helse.sykdomstidslinje.Dag
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

internal class VilkårsprøvdSkjæringstidspunkt(
    internal val `6G`: Inntekt,
    inntekter: List<FaktaavklartInntekt>,
    val tilkommendeInntekter: List<NyInntektUnderveis>,
    val deaktiverteArbeidsforhold: List<String>
) {
    private val inntekterFraInntektsgrunnlaget = inntekter.associate { inntekt -> inntekt.organisasjonsnummer to inntekt.fastsattÅrsinntekt }

    internal fun forArbeidsgiver(organisasjonsnummer: String): Inntekt? = inntekterFraInntektsgrunnlaget[organisasjonsnummer]

    internal fun medGhostOgTilkommenInntekt(utbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        val beregningsperiode = utbetalingstidslinjer.values.flatten().map { it.periode() }.periode()!!

        // Lager Utbetalingstidslinje for alle arbeidsgivere fra inntektsgrunnlaget
        val fraInntektsgrunnlag = inntekterFraInntektsgrunnlaget.mapValues { (orgnr, fastsattÅrsinntekt) ->
            // De som ikke har noen beregnede utbetalingstidslinjer er ghost for hele beregningsperioden
            val beregnedetidslinjer = utbetalingstidslinjer[orgnr] ?: emptyList()
            val ghosttidslinje = ghosttidslinje(beregningsperiode, fastsattÅrsinntekt, `6G`, beregnedetidslinjer)
            beregnedetidslinjer.fold(ghosttidslinje, Utbetalingstidslinje::plus)
        }

        val tilkomneV2 = nyeInntekterUnderveis(beregningsperiode)

        // Lager Utbetalingstidslinje for arbeidsgiverne som ikke er i inntektsgrunnlaget (tilkommen)
        // Her er ikke ghosttidslinjer aktuelt ettersom ghost må være i inntektsgrunnlaget (på skjæringstidspunktet)
        val tilkomneV3 = utbetalingstidslinjer
            .filterKeys { orgnr -> orgnr !in inntekterFraInntektsgrunnlaget.keys }
            .mapValues { (_, utbetalingstidslinjer) -> utbetalingstidslinjer.reduce(Utbetalingstidslinje::plus) }
            .filterValues { it.isNotEmpty() }

        return fraInntektsgrunnlag + tilkomneV2 + tilkomneV3
    }

    private fun ghosttidslinje(beregningsperiode: Periode, fastsattÅrsinntekt: Inntekt, `6G`: Inntekt, arbeidsgiverlinjer: List<Utbetalingstidslinje>): Utbetalingstidslinje {
        val beregnedePerioderForArbeidsgiver = arbeidsgiverlinjer.map { it.periode() }
        val ghostdagerForArbeidsgiver = beregningsperiode
            .filterNot { dato -> beregnedePerioderForArbeidsgiver.any { beregnetPeriode -> dato in beregnetPeriode } }

        // Lager en ghosttidslinje for de dagene i beregningsperioden arbeidsgiveren ikke er beregnet
        return with(Utbetalingstidslinje.Builder()) {
            ghostdagerForArbeidsgiver.forEach { dato ->
                if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
                else addArbeidsdag(
                    dato = dato,
                    økonomi = Økonomi.ikkeBetalt().inntekt(
                        aktuellDagsinntekt = fastsattÅrsinntekt,
                        beregningsgrunnlag = fastsattÅrsinntekt,
                        dekningsgrunnlag = INGEN,
                        `6G` = `6G`,
                        refusjonsbeløp = INGEN
                    )
                )
            }
            build()
        }
    }

    private fun nyeInntekterUnderveis(beregningsperiode: Periode): Map<String, Utbetalingstidslinje> {
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
        return tilkommendeInntekterTidslinje
    }

    data class FaktaavklartInntekt(
        val organisasjonsnummer: String,
        val fastsattÅrsinntekt: Inntekt
    )

    data class NyInntektUnderveis(
        val orgnummer: String,
        val beløpstidslinje: Beløpstidslinje
    )
}

internal data class ArbeidsgiverperiodeForVedtaksperiode(
    val vedtaksperiode: Periode,
    val arbeidsgiverperioder: List<Periode>
)

internal class UtbetalingstidslinjeBuilderVedtaksperiode(
    private val fastsattÅrsinntekt: Inntekt?,
    private val skjæringstidspunkt: LocalDate,
    private val `6G`: Inntekt,
    private val regler: ArbeidsgiverRegler,
    private val arbeidsgiverperiode: List<Periode>,
    private val dagerNavOvertarAnsvar: List<Periode>,
    private val refusjonstidslinje: Beløpstidslinje,
    inntektsendringer: Beløpstidslinje
) {
    private val inntektstidslinje = Inntektstidslinje(
        inntektsendringer = inntektsendringer.fraOgMed(skjæringstidspunkt.nesteDag),
        fastsattÅrsinntekt = fastsattÅrsinntekt
    )

    private val lagDefaultRefusjonsbeløpHvisMangler = { _: LocalDate, aktuellDagsinntekt: Inntekt -> aktuellDagsinntekt }
    private val krevRefusjonsbeløpHvisMangler = { dato: LocalDate, _: Inntekt ->
        error("Har ingen refusjonsopplysninger på vilkårsgrunnlag for utbetalingsdag $dato")
    }

    private fun refusjonsbeløp(dato: LocalDate, aktuellDagsinntekt: Inntekt, refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt): Inntekt {
        val refusjonFraBehandling = refusjonstidslinje[dato].takeIf { it is Beløpsdag }?.beløp
        return refusjonFraBehandling ?: refusjonsopplysningFinnesIkkeStrategi(dato, aktuellDagsinntekt)
    }

    internal fun medInntektHvisFinnes(
        dato: LocalDate,
        økonomi: Økonomi
    ): Økonomi {
        return medInntekt(dato, økonomi, lagDefaultRefusjonsbeløpHvisMangler)
    }

    private fun medInntektOrThrow(
        dato: LocalDate,
        økonomi: Økonomi,
    ): Økonomi {
        return medInntekt(dato, økonomi, krevRefusjonsbeløpHvisMangler)
    }

    private fun medInntekt(
        dato: LocalDate,
        økonomi: Økonomi,
        refusjonsopplysningFinnesIkkeStrategi: (LocalDate, Inntekt) -> Inntekt
    ): Økonomi {
        val aktuellDagsinntekt = inntektstidslinje[dato]
        return økonomi.inntekt(
            aktuellDagsinntekt = aktuellDagsinntekt,
            beregningsgrunnlag = fastsattÅrsinntekt ?: INGEN,
            dekningsgrunnlag = aktuellDagsinntekt * regler.dekningsgrad(),
            `6G` = if (dato < skjæringstidspunkt) INGEN else `6G`,
            refusjonsbeløp = refusjonsbeløp(dato, aktuellDagsinntekt, refusjonsopplysningFinnesIkkeStrategi)
        )
    }

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
                    if (erAGP(dag.dato)) arbeidsgiverperiodedag(builder, dag.dato, dag.økonomi)
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
                    else builder.addForeldetDag(dag.dato, medInntektHvisFinnes(dag.dato, dag.økonomi))
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
        return builder.build()
    }

    private fun erAGP(dato: LocalDate) = arbeidsgiverperiode.any { dato in it }
    private fun erAGPNavAnsvar(dato: LocalDate) = dagerNavOvertarAnsvar.any { dato in it }
    private fun arbeidsgiverperiodedag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi) {
        if (erAGPNavAnsvar(dato))
            return builder.addArbeidsgiverperiodedagNav(dato, medInntektOrThrow(dato, økonomi))
        builder.addArbeidsgiverperiodedag(dato, medInntektHvisFinnes(dato, økonomi.ikkeBetalt()))
    }

    private fun avvistDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi, begrunnelse: Begrunnelse) {
        builder.addAvvistDag(dato, medInntektHvisFinnes(dato, økonomi), listOf(begrunnelse))
    }

    private fun helg(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi) {
        builder.addHelg(dato, medInntektHvisFinnes(dato, økonomi.ikkeBetalt()))
    }

    private fun navDag(builder: Utbetalingstidslinje.Builder, dato: LocalDate, økonomi: Økonomi) {
        builder.addNAVdag(dato, medInntektOrThrow(dato, økonomi))
    }

    private fun fridag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addFridag(dato, medInntektHvisFinnes(dato, Økonomi.ikkeBetalt()))
    }

    private fun arbeidsdag(builder: Utbetalingstidslinje.Builder, dato: LocalDate) {
        builder.addArbeidsdag(dato, medInntektHvisFinnes(dato, Økonomi.ikkeBetalt()))
    }
}
