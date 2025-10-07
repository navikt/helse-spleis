package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Behandlingsporing
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpsdag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverberegning
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Økonomi

@JvmInline
value class Inntektskilde(val id: String) {
    fun dto() = InntektskildeDto(id)

    companion object {
        fun gjenopprett(dto: InntektskildeDto) = Inntektskilde(dto.id)
    }
}

internal class InntekterForBeregning private constructor(
    private val yrkesaktiviteter: Set<Behandlingsporing.Yrkesaktivitet>,
    private val fastsattÅrsinntekter: Map<Behandlingsporing.Yrkesaktivitet, InntektMedKilde>,
    private val inntektjusteringer: Map<Behandlingsporing.Yrkesaktivitet, Beløpstidslinje>,
    private val beregningsperiode: Periode
) {
    internal fun tilBeregning(yrkesaktivitet: Behandlingsporing.Yrkesaktivitet) =
        (fastsattÅrsinntekter[yrkesaktivitet]?.inntekt ?: INGEN) to (inntektjusteringer[yrkesaktivitet] ?: Beløpstidslinje())

    internal fun inntektsjusteringer(periode: Periode): Map<Behandlingsporing.Yrkesaktivitet, Beløpstidslinje> {
        check(periode in beregningsperiode) { "Perioden $periode er utenfor beregningsperioden $beregningsperiode" }
        return inntektjusteringer
            .mapValues { (_, inntektjustering) -> inntektjustering.subset(periode).medBeløp() }
            .filterValues { it.isNotEmpty() }
    }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        return yrkesaktiviteter.map { yrkesaktivitet ->
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

    private fun arbeidsdager(yrkesaktivitet: Behandlingsporing.Yrkesaktivitet, perioderMedArbeid: List<Periode>) = perioderMedArbeid.map { periode ->
        val (fastsattÅrsinntekt, inntektjusteringer) = tilBeregning(yrkesaktivitet)
        with(Utbetalingstidslinje.Builder()) {
            periode.forEach { dato ->
                if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
                else addArbeidsdag(
                    dato = dato,
                    økonomi = Økonomi.ikkeBetalt(
                        aktuellDagsinntekt = fastsattÅrsinntekt,
                        inntektjustering = (inntektjusteringer[dato] as? Beløpsdag)?.beløp ?: INGEN
                    ),
                )
            }
            build()
        }
    }

    internal class Builder(private val beregningsperiode: Periode) {
        private val yrkesaktiviteter = mutableSetOf<Behandlingsporing.Yrkesaktivitet>()
        private val fastsatteÅrsinntekter = mutableMapOf<Behandlingsporing.Yrkesaktivitet, InntektMedKilde>()
        private val inntektjusteringer = mutableMapOf<Behandlingsporing.Yrkesaktivitet, Beløpstidslinje>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            leggTilInntekt(Behandlingsporing.Yrkesaktivitet.Arbeidstaker(organisasjonsnummer), InntektMedKilde(fastsattÅrsinntekt, opplysningskilde))
        }

        internal fun selvstendigNæringsdrivende(fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            leggTilInntekt(Behandlingsporing.Yrkesaktivitet.Selvstendig, InntektMedKilde(fastsattÅrsinntekt, opplysningskilde))
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            fraInntektsgrunnlag(organisasjonsnummer, INGEN, opplysningskilde)
        }

        private fun leggTilInntekt(yrkesaktivitet: Behandlingsporing.Yrkesaktivitet, inntekt: InntektMedKilde) {
            yrkesaktiviteter.add(yrkesaktivitet)
            check(fastsatteÅrsinntekter.putIfAbsent(yrkesaktivitet, inntekt) == null) {
                "Inntekt for $yrkesaktivitet er allerede lagt til"
            }
        }

        internal fun inntektsendringer(yrkesaktivitet: Behandlingsporing.Yrkesaktivitet, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, kilde: Kilde) {
            val periode = (fom til listOfNotNull(tom, beregningsperiode.endInclusive).min()).subset(beregningsperiode)
            val inntektsendring = Beløpstidslinje.fra(periode, inntekt, kilde)

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
