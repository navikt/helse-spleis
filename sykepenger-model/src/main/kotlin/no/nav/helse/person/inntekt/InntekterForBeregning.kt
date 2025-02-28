package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.nesteDag
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN

internal data class InntekterForBeregning(
    private val inntekterPerInntektskilde: Map<String, Inntekter>,
    val `6G`: Inntekt
) {

    internal fun tilBeregning(organisasjonsnummer: String) = inntekterPerInntektskilde.getValue(organisasjonsnummer).let { (it.fastsattÅrsinntekt ?: INGEN) to it.inntektstidslinje }

    internal data class Inntekter(val fastsattÅrsinntekt: Inntekt?, val inntektstidslinje: Beløpstidslinje)

    internal class Builder(private val beregningsperiode: Periode, private val skjæringstidspunkt: LocalDate) {
        private val inntekterPerInntektskilde = mutableMapOf<String, Inntekter>()
        internal fun fraInntektsgrunnlag(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            inntekterPerInntektskilde[organisasjonsnummer] = Inntekter(
                fastsattÅrsinntekt = fastsattÅrsinntekt,
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, fastsattÅrsinntekt, opplysningskilde)
            )
        }

        internal fun deaktivertFraInntektsgrunnlag(organisasjonsnummer: String, opplysningskilde: Kilde) {
            inntekterPerInntektskilde[organisasjonsnummer] = Inntekter(
                fastsattÅrsinntekt = null,
                inntektstidslinje = Beløpstidslinje.fra(beregningsperiode, INGEN, opplysningskilde)
            )
        }

        internal fun inntektsendringer(inntektskilde: String, inntektstidslinje: Beløpstidslinje) {
            if (!inntekterPerInntektskilde.containsKey(inntektskilde)) {
                // dette kan kanskje noen ganger være tilkommen inntekt
                inntekterPerInntektskilde[inntektskilde] = Inntekter(
                    fastsattÅrsinntekt = null,
                    inntektstidslinje = inntektstidslinje.subset(beregningsperiode)
                )
            } else {
                // inntektsendring på en arbeidsgiver som finnes i inntektsgrunnlaget
                //      - økt arbeidsinnsats hos biarbeidsgiver (ghosts)
                //      - inntektsendring hos en arbeidsgiver du er syk hos (tror vi)

                // for arbeidsgivere som finnes i inntektsgrunnlaget, kan ikke beløpet på skjæringstidspunktet (fastsatt årsinntekt) endres som en inntektsendring. Da må inntektsgrunnlaget overstyres
                val før = inntekterPerInntektskilde.getValue(inntektskilde)
                val inntektsendring = inntektstidslinje.subset(beregningsperiode).fraOgMed(skjæringstidspunkt.nesteDag)
                val etter = før.copy(inntektstidslinje = før.inntektstidslinje + inntektsendring)
                inntekterPerInntektskilde.replace(inntektskilde, etter)
            }
        }

        private lateinit var `6G`: Inntekt

        internal fun medGjeldende6G(gjeldende6G: Inntekt) {
            this.`6G` = gjeldende6G
        }

        internal fun build() = InntekterForBeregning(inntekterPerInntektskilde.toMap(), `6G`)
    }
}
