package no.nav.helse.person.inntekt

import java.time.LocalDate
import no.nav.helse.økonomi.Inntekt

interface SubsumsjonObserverPort {
    fun `§ 8-15`(
        skjæringstidspunkt: LocalDate,
        organisasjonsnummer: String,
        inntekterSisteTreMåneder: List<Map<String, Any>>,
        forklaring: String,
        oppfylt: Boolean
    )

    fun `§ 8-28 ledd 3 bokstav b`(
        organisasjonsnummer: String,
        startdatoArbeidsforhold: LocalDate,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlag: Inntekt
    )

    fun `§ 8-28 ledd 3 bokstav c`(
        organisasjonsnummer: String,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlag: Inntekt
    )

    fun `§ 8-28 ledd 5`(
        organisasjonsnummer: String,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlag: Inntekt
    )

    fun `§ 8-28 ledd 3 bokstav a`(
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekterSisteTreMåneder: List<Map<String, Any>>,
        grunnlagForSykepengegrunnlag: Inntekt
    )

    fun `§ 8-29`(
        dato: LocalDate,
        omregnetÅrsinntekt: Inntekt,
        subsumsjonsformat: List<Map<String, Any>>,
        organisasjonsnummer: String
    )

    fun `§ 8-30 ledd 1`(omregnetÅrsinntektPerArbeidsgiver: Map<String, Inntekt>, omregnetÅrsinntekt: Inntekt)
    fun `§ 8-10 ledd 2 punktum 1`(
        erBegrenset: Boolean,
        maksimaltSykepengegrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        beregningsgrunnlag: Inntekt
    )

    fun `§ 8-51 ledd 2`(
        oppfyllerMinsteinntektskrav: Boolean,
        skjæringstidspunkt: LocalDate,
        alderPåDato: Int,
        beregningsgrunnlag: Inntekt,
        minsteinntekt: Inntekt
    )

    fun `§ 8-3 ledd 2 punktum 1`(
        oppfyllerMinsteinntektskrav: Boolean,
        skjæringstidspunkt: LocalDate,
        beregningsgrunnlag: Inntekt,
        minsteinntekt: Inntekt
    )
}