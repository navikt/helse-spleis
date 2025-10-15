package no.nav.helse.utbetalingstidslinje

import no.nav.helse.erHelg
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Økonomi
import java.time.LocalDate

internal class InntekterForBeregning private constructor(
    private val yrkesaktiviteter: Set<Arbeidsgiverberegning.Yrkesaktivitet>,
    private val fastsattÅrsinntekter: Map<Arbeidsgiverberegning.Yrkesaktivitet, InntektMedKilde>,
    private val inntektjusteringer: Map<Arbeidsgiverberegning.Yrkesaktivitet, Beløpstidslinje>,
    private val beregningsperiode: Periode
) {
    internal fun tilBeregning(yrkesaktivitet: Arbeidsgiverberegning.Yrkesaktivitet) =
        (fastsattÅrsinntekter[yrkesaktivitet]?.inntekt ?: Inntekt.Companion.INGEN) to (inntektjusteringer[yrkesaktivitet] ?: Beløpstidslinje())

    internal fun inntektsjusteringer(periode: Periode): Map<Arbeidsgiverberegning.Yrkesaktivitet, Beløpstidslinje> {
        check(periode in beregningsperiode) { "Perioden $periode er utenfor beregningsperioden $beregningsperiode" }
        return inntektjusteringer
            .mapValues { (_, inntektjustering) -> inntektjustering.subset(periode).medBeløp() }
            .filterValues { it.isNotEmpty() }
    }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        val alleYrkesaktiviteter = (yrkesaktiviteter + beregnedeUtbetalingstidslinjer.map { it.yrkesaktivitet })
        return alleYrkesaktiviteter.map { yrkesaktivitet ->
            val arbeidsgiverberegning = beregnedeUtbetalingstidslinjer.firstOrNull { it.yrkesaktivitet == yrkesaktivitet } ?: Arbeidsgiverberegning(
                yrkesaktivitet = yrkesaktivitet,
                vedtaksperioder = emptyList(),
                ghostOgAndreInntektskilder = emptyList()
            )
            val beregnedeUtbetalingstidslinjerForInntektskilde = arbeidsgiverberegning.vedtaksperioder
            val beregendePerioderForInntektskilde = beregnedeUtbetalingstidslinjerForInntektskilde.map { it.utbetalingstidslinje.periode() }
            val uberegnedeDagerForArbeidsgiver = beregningsperiode.uten(beregendePerioderForInntektskilde)
            val uberegnetUtbetalingstidslinjeForArbeidsgiver = arbeidsdager(yrkesaktivitet, uberegnedeDagerForArbeidsgiver)
            arbeidsgiverberegning.copy(
                ghostOgAndreInntektskilder = uberegnetUtbetalingstidslinjeForArbeidsgiver
            )
        }
    }

    private fun arbeidsdager(yrkesaktivitet: Arbeidsgiverberegning.Yrkesaktivitet, perioderMedArbeid: List<Periode>) = perioderMedArbeid.map { periode ->
        val (fastsattÅrsinntekt, inntektjusteringer) = tilBeregning(yrkesaktivitet)
        with(Utbetalingstidslinje.Builder()) {
            periode.forEach { dato ->
                if (dato.erHelg()) addFridag(dato, Økonomi.Companion.ikkeBetalt())
                else addArbeidsdag(
                    dato = dato,
                    økonomi = Økonomi.Companion.ikkeBetalt(
                        aktuellDagsinntekt = fastsattÅrsinntekt,
                        inntektjustering = (inntektjusteringer[dato] as? Beløpsdag)?.beløp ?: Inntekt.Companion.INGEN
                    ),
                )
            }
            build()
        }
    }

    internal class Builder(private val beregningsperiode: Periode) {
        private val yrkesaktiviteter = mutableSetOf<Arbeidsgiverberegning.Yrkesaktivitet>()
        private val fastsatteÅrsinntekter = mutableMapOf<Arbeidsgiverberegning.Yrkesaktivitet, InntektMedKilde>()
        private val inntektjusteringer = mutableMapOf<Arbeidsgiverberegning.Yrkesaktivitet, Beløpstidslinje>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            leggTilInntekt(Arbeidsgiverberegning.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer), InntektMedKilde(fastsattÅrsinntekt, opplysningskilde))
        }

        internal fun selvstendigNæringsdrivende(fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            leggTilInntekt(Arbeidsgiverberegning.Yrkesaktivitet.Selvstendig, InntektMedKilde(fastsattÅrsinntekt, opplysningskilde))
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            fraInntektsgrunnlag(organisasjonsnummer, Inntekt.Companion.INGEN, opplysningskilde)
        }

        private fun leggTilInntekt(yrkesaktivitet: Arbeidsgiverberegning.Yrkesaktivitet, inntekt: InntektMedKilde) {
            yrkesaktiviteter.add(yrkesaktivitet)
            check(fastsatteÅrsinntekter.putIfAbsent(yrkesaktivitet, inntekt) == null) {
                "Inntekt for $yrkesaktivitet er allerede lagt til"
            }
        }

        internal fun inntektsendringer(yrkesaktivitet: Arbeidsgiverberegning.Yrkesaktivitet, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, kilde: Kilde) {
            val periode = (fom til listOfNotNull(tom, beregningsperiode.endInclusive).min()).subset(beregningsperiode)
            val inntektsendring = Beløpstidslinje.Companion.fra(periode, inntekt, kilde)

            yrkesaktiviteter.add(yrkesaktivitet)
            inntektjusteringer.compute(yrkesaktivitet) { _, inntekter ->
                ((inntekter ?: Beløpstidslinje()).erstatt(inntektsendring))
            }
        }

        internal fun build() = InntekterForBeregning(
            yrkesaktiviteter = yrkesaktiviteter.toSet(),
            fastsattÅrsinntekter = fastsatteÅrsinntekter.toMap(),
            beregningsperiode = beregningsperiode,
            inntektjusteringer = inntektjusteringer.toMap()
        )
    }

    private data class InntektMedKilde(val inntekt: Inntekt, val kilde: Kilde)
}
