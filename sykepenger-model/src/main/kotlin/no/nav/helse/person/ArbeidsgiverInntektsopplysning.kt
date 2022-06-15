package no.nav.helse.person

import java.time.LocalDate
import no.nav.helse.person.Inntektshistorikk.Inntektsopplysning.Companion.valider
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.utbetalingstidslinje.Arbeidsgiverperiode
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.økonomi.Økonomi

internal class ArbeidsgiverInntektsopplysning(
    private val orgnummer: String,
    private val inntektsopplysning: Inntektshistorikk.Inntektsopplysning
) {
    private fun omregnetÅrsinntekt(acc: Inntekt): Inntekt {
        return acc + inntektsopplysning.omregnetÅrsinntekt()
    }

    internal fun accept(visitor: ArbeidsgiverInntektsopplysningVisitor) {
        visitor.preVisitArbeidsgiverInntektsopplysning(this, orgnummer)
        inntektsopplysning.accept(visitor)
        visitor.postVisitArbeidsgiverInntektsopplysning(this, orgnummer)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ArbeidsgiverInntektsopplysning) return false
        return orgnummer == other.orgnummer && inntektsopplysning == other.inntektsopplysning
    }

    override fun hashCode(): Int {
        var result = orgnummer.hashCode()
        result = 31 * result + inntektsopplysning.hashCode()
        return result
    }

    internal companion object {
        internal fun List<ArbeidsgiverInntektsopplysning>.valider(aktivitetslogg: IAktivitetslogg) {
            map { it.inntektsopplysning }.valider(aktivitetslogg)
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.harInntekt(organisasjonsnummer: String) =
            any { it.orgnummer == organisasjonsnummer }

        internal fun List<ArbeidsgiverInntektsopplysning>.omregnetÅrsinntekt() =
            fold(INGEN) { acc, item -> item.omregnetÅrsinntekt(acc)}

        internal fun List<ArbeidsgiverInntektsopplysning>.omregnetÅrsinntektPerArbeidsgiver() =
            associate { it.orgnummer to it.inntektsopplysning.omregnetÅrsinntekt() }

        internal fun List<ArbeidsgiverInntektsopplysning>.sammenligningsgrunnlag(): Inntekt {
            return map { it.inntektsopplysning.rapportertInntekt() }.summer()
        }

        internal fun List<ArbeidsgiverInntektsopplysning>.inntektsopplysningPerArbeidsgiver() =
            associate { it.orgnummer to it.inntektsopplysning }

        internal fun List<ArbeidsgiverInntektsopplysning>.medInntekt(organisasjonsnummer: String, skjæringstidspunkt: LocalDate, dato: LocalDate, økonomi: Økonomi, arbeidsgiverperiode: Arbeidsgiverperiode?, regler: ArbeidsgiverRegler, subsumsjonObserver: SubsumsjonObserver): Økonomi? {
            return singleOrNull { it.orgnummer == organisasjonsnummer }?.inntektsopplysning?.omregnetÅrsinntekt()?.let { inntekt ->
                økonomi.inntekt(
                    aktuellDagsinntekt = inntekt,
                    dekningsgrunnlag = inntekt.dekningsgrunnlag(dato, regler, subsumsjonObserver),
                    skjæringstidspunkt = skjæringstidspunkt,
                    arbeidsgiverperiode = arbeidsgiverperiode
                )
            }
        }
    }
}
