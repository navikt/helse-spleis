package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
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
    private val fastsattÅrsinntekter: Map<Inntektskilde, InntektMedKilde>,
    private val inntektjusteringer: Map<Inntektskilde, Beløpstidslinje>,
    private val beregningsperiode: Periode
) {
    internal fun tilBeregning(organisasjonsnummer: String) = tilBeregning(Inntektskilde(organisasjonsnummer))
    private fun tilBeregning(inntektskilde: Inntektskilde) = (fastsattÅrsinntekter[inntektskilde]?.inntekt ?: INGEN) to (inntektjusteringer[inntektskilde] ?: Beløpstidslinje())

    internal fun inntektsjusteringer(periode: Periode): Map<Inntektskilde, Beløpstidslinje> {
        check(periode in beregningsperiode) { "Perioden $periode er utenfor beregningsperioden $beregningsperiode" }
        return inntektjusteringer
            .mapValues { (_, inntektjustering) -> inntektjustering.subset(periode).medBeløp() }
            .filterValues { it.isNotEmpty() }
    }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        return (fastsattÅrsinntekter.keys + inntektjusteringer.keys).map { inntektskilde ->
            val arbeidsgiverberegning = beregnedeUtbetalingstidslinjer.firstOrNull { it.orgnummer == inntektskilde.id } ?: Arbeidsgiverberegning(
                orgnummer = inntektskilde.id,
                vedtaksperioder = emptyList(),
                ghostOgAndreInntektskilder = emptyList()
            )
            val beregnedeUtbetalingstidslinjerForInntektskilde = arbeidsgiverberegning.vedtaksperioder
            val beregendePerioderForInntektskilde = beregnedeUtbetalingstidslinjerForInntektskilde.map { it.utbetalingstidslinje.periode() }
            val uberegnedeDagerForArbeidsgiver = beregningsperiode.uten(beregendePerioderForInntektskilde)
            val uberegnetUtbetalingstidslinjeForArbeidsgiver = arbeidsdager(inntektskilde, uberegnedeDagerForArbeidsgiver)
            arbeidsgiverberegning.copy(
                ghostOgAndreInntektskilder = uberegnetUtbetalingstidslinjeForArbeidsgiver
            )
        }
    }

    private fun arbeidsdager(inntektskilde: Inntektskilde, perioderMedArbeid: List<Periode>) = perioderMedArbeid.map { periode ->
        val (fastsattÅrsinntekt, inntektjusteringer) = tilBeregning(inntektskilde)
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
        private val fastsatteÅrsinntekter = mutableMapOf<String, InntektMedKilde>()
        private val inntektjusteringer = mutableMapOf<Inntektskilde, Beløpstidslinje>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in fastsatteÅrsinntekter) { "Organisasjonsnummer $organisasjonsnummer er allerede lagt til" }
            fastsatteÅrsinntekter[organisasjonsnummer] = InntektMedKilde(fastsattÅrsinntekt, opplysningskilde)
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in fastsatteÅrsinntekter) { "Organisasjonsnummer $organisasjonsnummer er allerede lagt til" }
            fastsatteÅrsinntekter[organisasjonsnummer] = InntektMedKilde(INGEN, opplysningskilde)
        }

        internal fun inntektsendringer(inntektskilde: Inntektskilde, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId) {
            val periode = (fom til listOfNotNull(tom, beregningsperiode.endInclusive).min()).subset(beregningsperiode)
            val kilde = Kilde(meldingsreferanseId, SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
            val inntektsendring = Beløpstidslinje.fra(periode, inntekt, kilde)
            inntektjusteringer.compute(inntektskilde) { _, inntekter ->
                ((inntekter ?: Beløpstidslinje()).erstatt(inntektsendring))
            }
        }

        internal fun build() = InntekterForBeregning(
            fastsattÅrsinntekter = fastsatteÅrsinntekter.mapKeys { Inntektskilde(it.key) },
            beregningsperiode = beregningsperiode,
            inntektjusteringer = inntektjusteringer
        )
    }

    private data class InntektMedKilde(val inntekt: Inntekt, val kilde: Kilde)
}
