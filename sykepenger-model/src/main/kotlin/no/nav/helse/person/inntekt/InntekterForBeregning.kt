package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.dto.InntektskildeDto
import no.nav.helse.erHelg
import no.nav.helse.hendelser.Avsender.SYSTEM
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
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

internal data class InntekterForBeregning(
    private val inntekterPerInntektskilde: Map<Inntektskilde, Beløpstidslinje>,
    private val beregningsperiode: Periode
) {

    internal fun tilBeregning(organisasjonsnummer: String) = inntekterPerInntektskilde.getValue(Inntektskilde(organisasjonsnummer))

    internal fun forPeriode(periode: Periode) = inntekterPerInntektskilde
        .mapValues { (_, inntekter) -> inntekter.subset(periode) }
        .filterValues { inntekter -> inntekter.isNotEmpty() }

    internal fun hensyntattAlleInntektskilder(beregnedeUtbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, Utbetalingstidslinje> {
        return inntekterPerInntektskilde.mapValues { (inntektskilde, inntekter) ->
            val beregnedeUtbetalingstidslinjerForInntektskilde = beregnedeUtbetalingstidslinjer[inntektskilde.id] ?: emptyList()
            val beregendePerioderForInntektskilde = beregnedeUtbetalingstidslinjerForInntektskilde.map(Utbetalingstidslinje::periode)
            val uberegnedeDagerForArbeidsgiver = beregningsperiode.uten(beregendePerioderForInntektskilde).flatten()
            val uberegnetUtbetalingstidslinjeForArbeidsgiver = arbeidsdager(inntekter, uberegnedeDagerForArbeidsgiver)
            beregnedeUtbetalingstidslinjerForInntektskilde.fold(uberegnetUtbetalingstidslinjeForArbeidsgiver, Utbetalingstidslinje::plus)
        }.mapKeys { (inntektskilde, _) -> inntektskilde.id }.filterValues { it.isNotEmpty() }
    }

    private fun arbeidsdager(inntekter: Beløpstidslinje, dager: List<LocalDate>) = with(Utbetalingstidslinje.Builder()) {
        dager.forEach { dato ->
            if (dato.erHelg()) addFridag(dato, Økonomi.ikkeBetalt())
            else addArbeidsdag(
                dato = dato,
                økonomi = Økonomi.ikkeBetalt(aktuellDagsinntekt = inntekter[dato].beløp)
            )
        }
        build()
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
            val periode = fom til listOfNotNull(tom, beregningsperiode.endInclusive).min()
            val kilde = Kilde(meldingsreferanseId, SYSTEM, LocalDateTime.now()) // TODO: TilkommenV4 smak litt på denne
            val inntektsendring = Beløpstidslinje.fra(periode, inntekt, kilde)
            inntektsendringer.compute(inntektskilde) { _, inntekter ->
                ((inntekter ?: Beløpstidslinje.fra(beregningsperiode, INGEN, kilde)).erstatt(inntektsendring)).subset(beregningsperiode)
            }
        }

        internal fun build(): InntekterForBeregning {
            val result = buildMap<Inntektskilde, Beløpstidslinje> {
                // Tar utgangspunkt i inntektene fra inntektsgrunnlaget
                inntekterFraInntektsgrunnlaget.forEach { (organisasjonsnummer, inntekter) ->
                    put(Inntektskilde(organisasjonsnummer), inntekter)
                }

                // Og fletter inn inntektsendringene
                inntektsendringer.forEach { (kilde, inntektsendring) ->
                    compute(kilde) { _, inntekter ->
                        inntekter?.erstatt(inntektsendring) ?: inntektsendring
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

        fun forAuu(periode: Periode, organisasjonsnummer: String, inntektsgrunnlag: Inntektsgrunnlag?): Pair<Beløpstidslinje, InntekterForBeregning> {
            if (inntektsgrunnlag == null) {
                val inntekterForBeregning = with(Builder(periode)) {
                    build()
                }
                // Når vi skal lage en utbetalingstidslinje på en AUU hvor det ikke finnes noe inntektsgrunnlag vil vi ikke lagre de tøysete verdiene på behandlingen, bare bruke dem for å lage utbetalingstidslinje
                return Pair(Beløpstidslinje.fra(periode, INGEN, TøyseteOpplysningskilde), inntekterForBeregning)
            }

            val inntekterForBeregning = with(Builder(periode)) {
                inntektsgrunnlag.beverte(this)
                build()
            }

            val inntektstidslinje = inntekterForBeregning.inntekterPerInntektskilde[Inntektskilde(organisasjonsnummer)] ?: error(
                "Det er en arbeidsgiver som ikke inngår i SP: $organisasjonsnummer som har søknader: $periode.\n" +
                "Burde ikke arbeidsgiveren være kjent i sykepengegrunnlaget, enten i form av en skatteinntekt eller en tilkommet?"
            )

            // Når vi skal lage en utbetalingstidslinje på en AUU hvor det finnes inntektsgrunnlag så lagrer vi ned de ekte inntektene
            return inntektstidslinje to inntekterForBeregning
        }
    }
}
