package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.helse.hendelser.MeldingsreferanseId
import no.nav.helse.person.inntekt.SkatteopplysningerForSykepengegrunnlag.AnsattPeriode.Companion.harArbeidsforholdNyereEnn
import no.nav.helse.yearMonth

// § 8-28-inntekter fra skatteetaten
internal data class SkatteopplysningerForSykepengegrunnlag(
    val arbeidsgiver: String,
    val hendelseId: MeldingsreferanseId,
    val skjæringstidspunkt: LocalDate,
    val inntektsopplysninger: List<Skatteopplysning>,
    val ansattPerioder: List<AnsattPeriode>,
    val tidsstempel: LocalDateTime
) {
    val utgangspunkt = skjæringstidspunkt.yearMonth
    // inntekter tre måneder før skjæringstidspunktet brukes for å regne om til årsinntekt
    val beregningsperiode = utgangspunkt.minusMonths(3)..utgangspunkt.minusMonths(1)

    val treMånederFørSkjæringstidspunkt = inntektsopplysninger.filter { it.måned in beregningsperiode }
    val omregnetÅrsinntekt = Skatteopplysning.omregnetÅrsinntekt(treMånederFørSkjæringstidspunkt)

    val inntektsdata = Inntektsdata(
        hendelseId = hendelseId,
        dato = skjæringstidspunkt,
        beløp = omregnetÅrsinntekt,
        tidsstempel = tidsstempel
    )

    val ansattVedSkjæringstidspunkt = ansattVedSkjæringstidspunkt(skjæringstidspunkt)
    val nyoppstartetArbeidsforhold = nyoppstartetArbeidsforhold(skjæringstidspunkt)

    // må ha inntekter innenfor to måneder før skjæringstidspunktet for at vi skal hensynta skatteinntektene
    val inntektvurderingsperiode = utgangspunkt.minusMonths(MAKS_INNTEKT_GAP.toLong())..utgangspunkt.minusMonths(1)
    val harInntekterToMånederFørSkjæringstidspunkt = inntektsopplysninger.any { it.måned in inntektvurderingsperiode }
    val erNyoppstartetArbeidsforhold = inntektsopplysninger.isEmpty() && nyoppstartetArbeidsforhold

    val erGhostarbeidsgiver = ansattPerioder.isNotEmpty() && ansattVedSkjæringstidspunkt && (erNyoppstartetArbeidsforhold || harInntekterToMånederFørSkjæringstidspunkt)

    private fun nyoppstartetArbeidsforhold(skjæringstidspunkt: LocalDate) =
        ansattPerioder.harArbeidsforholdNyereEnn(skjæringstidspunkt, MAKS_INNTEKT_GAP)

    private fun ansattVedSkjæringstidspunkt(dato: LocalDate) =
        ansattPerioder.any { ansattPeriode -> ansattPeriode.gjelder(dato) }

    data class AnsattPeriode(
        val ansattFom: LocalDate,
        val ansattTom: LocalDate?
    ) {
        fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)
        fun harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Int) =
            ansattFom >= skjæringstidspunkt.withDayOfMonth(1).minusMonths(antallMåneder.toLong())

        companion object {
            fun List<AnsattPeriode>.harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Int) =
                harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder).isNotEmpty()

            private fun List<AnsattPeriode>.harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Int) = this
                .filter { it.harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder) }
                .filter { it.gjelder(skjæringstidspunkt) }
        }
    }

    internal companion object {
        private const val MAKS_INNTEKT_GAP = 2
    }
}
