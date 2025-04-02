package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
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
    private val inntekterPerInntektskilde: Map<Inntektskilde, Beløpstidslinje>,
    private val beregningsperiode: Periode
) {
    internal fun tilBeregning(organisasjonsnummer: String) = inntekterPerInntektskilde[Inntektskilde(organisasjonsnummer)] ?: ingenInntekt(beregningsperiode)

    internal fun forPeriode(periode: Periode): Map<Inntektskilde, Beløpstidslinje> {
        check(periode in beregningsperiode) { "Perioden $periode er utenfor beregningsperioden $beregningsperiode" }
        return inntekterPerInntektskilde
            .mapValues { (_, inntekter) -> inntekter.subset(periode).medBeløp() }
            .filterValues { inntekter -> inntekter.isNotEmpty() }
    }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: List<Arbeidsgiverberegning>): List<Arbeidsgiverberegning> {
        return inntekterPerInntektskilde.map { (inntektskilde, inntekter) ->
            val arbeidsgiverberegning = beregnedeUtbetalingstidslinjer.firstOrNull { it.orgnummer == inntektskilde.id } ?: Arbeidsgiverberegning(
                orgnummer = inntektskilde.id,
                vedtaksperioder = emptyList(),
                ghostOgAndreInntektskilder = emptyList()
            )
            val beregnedeUtbetalingstidslinjerForInntektskilde = arbeidsgiverberegning.vedtaksperioder
            val beregendePerioderForInntektskilde = beregnedeUtbetalingstidslinjerForInntektskilde.map { it.utbetalingstidslinje.periode() }
            val uberegnedeDagerForArbeidsgiver = beregningsperiode.uten(beregendePerioderForInntektskilde)
            val uberegnetUtbetalingstidslinjeForArbeidsgiver = arbeidsdager(inntekter, uberegnedeDagerForArbeidsgiver)
            arbeidsgiverberegning.copy(
                ghostOgAndreInntektskilder = uberegnetUtbetalingstidslinjeForArbeidsgiver
            )
        }
    }

    private fun arbeidsdager(inntekter: Beløpstidslinje, perioderMedArbeid: List<Periode>) = perioderMedArbeid.map { periode ->
        with(Utbetalingstidslinje.Builder()) {
            periode.forEach { dato ->
                if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
                else addArbeidsdag(
                    dato = dato,
                    økonomi = Økonomi.ikkeBetalt(aktuellDagsinntekt = inntekter[dato].beløp)
                )
            }
            build()
        }
    }

    internal class Builder(private val beregningsperiode: Periode) {
        private val inntekterFraInntektsgrunnlaget = mutableMapOf<String, Beløpstidslinje>()
        private val inntektsendringer = mutableMapOf<Inntektskilde, Beløpstidslinje>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in inntekterFraInntektsgrunnlaget) { "Organisasjonsnummer $organisasjonsnummer er allerede lagt til" }
            inntekterFraInntektsgrunnlaget[organisasjonsnummer] = Beløpstidslinje.fra(beregningsperiode, fastsattÅrsinntekt, opplysningskilde)
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in inntekterFraInntektsgrunnlaget) { "Organisasjonsnummer $organisasjonsnummer er allerede lagt til" }
            inntekterFraInntektsgrunnlaget[organisasjonsnummer] = Beløpstidslinje.fra(beregningsperiode, INGEN, opplysningskilde)
        }

        internal fun inntektsendringer(inntektskilde: Inntektskilde, fom: LocalDate, tom: LocalDate?, inntekt: Inntekt, meldingsreferanseId: MeldingsreferanseId) {
            val periode = (fom til listOfNotNull(tom, beregningsperiode.endInclusive).min()).subset(beregningsperiode)
            val kilde = Kilde(meldingsreferanseId, SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
            val inntektsendring = Beløpstidslinje.fra(periode, inntekt, kilde)
            inntektsendringer.compute(inntektskilde) { _, inntekter ->
                ((inntekter ?: Beløpstidslinje()).erstatt(inntektsendring))
            }
        }

        internal fun build(): InntekterForBeregning {
            val result = buildMap<Inntektskilde, Beløpstidslinje> {
                // Tar utgangspunkt i inntektene fra inntektsgrunnlaget
                inntekterFraInntektsgrunnlaget.forEach { (organisasjonsnummer, inntekter) ->
                    put(Inntektskilde(organisasjonsnummer), inntekter)
                }

                // Og fletter inn inntektsendringene
                inntektsendringer.forEach { (inntektskilde, inntektsendring) ->
                    compute(inntektskilde) { _, inntekter ->
                        val opplysningeskilde = inntektsendring.firstOrNull()?.kilde ?: return@compute inntekter
                        (inntekter ?: Beløpstidslinje.fra(beregningsperiode, INGEN, opplysningeskilde)).erstatt(inntektsendring)
                    }
                }
            }
            return InntekterForBeregning(
                inntekterPerInntektskilde = result,
                beregningsperiode = beregningsperiode
            )
        }
    }

    internal companion object {
        private val TøyseteMeldingsreferanseId = MeldingsreferanseId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
        private val TøyseteTidsstempel = LocalDate.EPOCH.atStartOfDay()
        private val TøyseteOpplysningskilde = Kilde(TøyseteMeldingsreferanseId, SYSTEM, TøyseteTidsstempel)
        private fun ingenInntekt(periode: Periode) = Beløpstidslinje.fra(periode, INGEN, TøyseteOpplysningskilde)
    }
}
