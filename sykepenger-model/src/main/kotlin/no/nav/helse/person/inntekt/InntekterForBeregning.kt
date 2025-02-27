package no.nav.helse.person.inntekt

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.beløp.Beløpstidslinje
import no.nav.helse.person.beløp.Kilde
import no.nav.helse.økonomi.Inntekt

internal data class InntekterForBeregning(val inntekterPerInntektskilde: Map<String, Beløpstidslinje>, val `6G`: Inntekt) {

    internal fun forArbeidsgiver(organisasjonsnummer: String) = inntekterPerInntektskilde.getValue(organisasjonsnummer)

    internal class Builder(private val beregningsperiode: Periode) {
        private val inntekterPerInntektskilde = mutableMapOf<String, Beløpstidslinje>()
        internal fun fastsattÅrsinntekt(organisasjonsnummer: String, fastsattÅrsinntekt: Inntekt, opplysningskilde: Kilde) {
            inntekterPerInntektskilde[organisasjonsnummer] = Beløpstidslinje.fra(beregningsperiode, fastsattÅrsinntekt, opplysningskilde)
        }

        internal fun inntektsendringer(inntektskilde: String, beløpstidslinje: Beløpstidslinje) {
            if (!inntekterPerInntektskilde.containsKey(inntektskilde)) {
                // dette kan kanskje noen ganger være tilkommen inntekt
                inntekterPerInntektskilde[inntektskilde] = beløpstidslinje.subset(beregningsperiode)
            } else {
                // inntektsendring på en arbeidsgiver som finnes i inntektsgrunnlaget
                //      - økt arbeidsinnsats hos biarbeidsgiver (ghosts)
                //      - inntektsendring hos en arbeidsgiver du er syk hos (tror vi)
                inntekterPerInntektskilde.replace(inntektskilde, inntekterPerInntektskilde.getValue(inntektskilde) + beløpstidslinje.subset(beregningsperiode))
            }
        }

        private lateinit var `6G`: Inntekt

        internal fun medGjeldende6G(gjeldende6G: Inntekt) {
            this.`6G` = gjeldende6G
        }

        internal fun build() = InntekterForBeregning(inntekterPerInntektskilde.toMap(), `6G`)
    }
}
