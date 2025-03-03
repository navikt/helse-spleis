package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.utbetalingstidslinje.Utbetalingstidslinje
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

internal data class InntekterForBeregning(
    private val inntekterPerInntektskilde: Map<String, Inntekter>,
    val `6G`: Inntekt
) {

    internal fun tilBeregning(organisasjonsnummer: String) = inntekterPerInntektskilde.getValue(organisasjonsnummer).let { it.fastsattÅrsinntekt to it.inntektstidslinje }

    internal fun medVektlagteUtbetalingstidslinjer(beregnedeUtbetalingstidslinjer: Map<String, List<Utbetalingstidslinje>>): Map<String, List<Utbetalingstidslinje>> {
        TODO("Her må vi vel loope inntekterPerInntekskilde og lage Utbetalingstidslinje som skal vektlegges selv om de ikke skal utbetales")
    }

    internal sealed interface Inntekter {
        val fastsattÅrsinntekt: Inntekt
        val inntektstidslinje: Beløpstidslinje
        fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje): Inntekter
        fun vektlagtUtbetalingstidslinje(beregnedeUtbetalingstidslinjer: List<Utbetalingstidslinje>, `6G`: Inntekt): Utbetalingstidslinje
        data class FraInntektsgrunnlag(override val fastsattÅrsinntekt: Inntekt, override val inntektstidslinje: Beløpstidslinje): Inntekter {
            override fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje) = copy(
                // For arbeidsgiver i inntektsgrunnlaget kan ikke inntekten på skjæringstidspunktet endres av en inntektsendring.
                inntektstidslinje = this.inntektstidslinje + inntektsendringer.fraOgMed(skjæringstidspunkt.nesteDag)
            )
            override fun vektlagtUtbetalingstidslinje(beregnedeUtbetalingstidslinjer: List<Utbetalingstidslinje>, `6G`: Inntekt): Utbetalingstidslinje {
                TODO("Det er her vi må lage de beryktede 'ghosttidslinjene' da")
            }
        }

        data class DeaktivertFraInntektsgrunnlag(override val inntektstidslinje: Beløpstidslinje): Inntekter {
            override val fastsattÅrsinntekt = INGEN
            override fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje) = copy(
                // For arbeidsgiver i inntektsgrunnlaget kan ikke inntekten på skjæringstidspunktet endres av en inntektsendring.
                inntektstidslinje = this.inntektstidslinje + inntektsendringer.fraOgMed(skjæringstidspunkt.nesteDag)
            )

            override fun vektlagtUtbetalingstidslinje(beregnedeUtbetalingstidslinjer: List<Utbetalingstidslinje>, `6G`: Inntekt): Utbetalingstidslinje {
                TODO("For deaktiverte kan vi vel bare returnere en tom Utbetalingstidslinje?")
            }
        }

        data class NyInntektskilde(override val inntektstidslinje: Beløpstidslinje = Beløpstidslinje()) : Inntekter {
            override val fastsattÅrsinntekt = INGEN
            override fun inntektsendring(skjæringstidspunkt: LocalDate, inntektsendringer: Beløpstidslinje) = copy(
                inntektstidslinje = this.inntektstidslinje + inntektsendringer
            )
            override fun vektlagtUtbetalingstidslinje(beregnedeUtbetalingstidslinjer: List<Utbetalingstidslinje>, `6G`: Inntekt): Utbetalingstidslinje {
                TODO("Her må vi bare lage en utbetalingstidslinje for de delene inntektstidslinjen dekker. Ikke noe dekking av hull som kjent fra ghost")
            }
        }
    }

    internal class Builder(private val beregningsperiode: Periode, private val skjæringstidspunkt: LocalDate) {
        private val inntekterPerInntektskilde = mutableMapOf<String, Inntekter>()

        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in inntekterPerInntektskilde) { "Organisasjonsnummer er allerede lagt til som ${inntekterPerInntektskilde.getValue(organisasjonsnummer)::class.simpleName}" }
            inntekterPerInntektskilde[organisasjonsnummer] = Inntekter.FraInntektsgrunnlag(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, fastsattÅrsinntekt, opplysningskilde)
            )
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            check(organisasjonsnummer !in inntekterPerInntektskilde) { "Organisasjonsnummer er allerede lagt til som ${inntekterPerInntektskilde.getValue(organisasjonsnummer)::class.simpleName}" }
            inntekterPerInntektskilde[organisasjonsnummer] = Inntekter.DeaktivertFraInntektsgrunnlag(
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, INGEN, opplysningskilde)
            )
        }

        internal fun inntektsendringer(inntektskilde: String, inntektsendring: Beløpstidslinje) {
            // Om vi ikke kjenner til inntektskilden fra før kan vi være diverse snax. Derunder "tilkommen inntekt"
            if (inntektskilde !in inntekterPerInntektskilde) {
                inntekterPerInntektskilde[inntektskilde] = Inntekter.NyInntektskilde()
            }
            // Litt avhengig av inntekten som er lagt til grunn håndterer vi innteksendringer litt forskjellig
            inntekterPerInntektskilde[inntektskilde] = inntekterPerInntektskilde
                .getValue(inntektskilde)
                .inntektsendring(skjæringstidspunkt, inntektsendring.subset(beregningsperiode))
        }

        private lateinit var `6G`: Inntekt

        internal fun medGjeldende6G(gjeldende6G: Inntekt) {
            this.`6G` = gjeldende6G
        }

        internal fun build() = InntekterForBeregning(inntekterPerInntektskilde.toMap(), `6G`)
    }
}
