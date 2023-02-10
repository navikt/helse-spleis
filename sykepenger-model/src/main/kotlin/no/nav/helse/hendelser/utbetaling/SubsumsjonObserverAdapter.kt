package no.nav.helse.hendelser.utbetaling

import java.time.LocalDate
import no.nav.helse.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.inntekt.SubsumsjonObserverPort
import no.nav.helse.økonomi.Inntekt

fun SubsumsjonObserver.inntektPort() = SubsumsjonObserverAdapter(this)
class SubsumsjonObserverAdapter(private val observer: SubsumsjonObserver) : SubsumsjonObserverPort {
    override fun `§ 8-15`(
        skjæringstidspunkt: LocalDate,
        organisasjonsnummer: String,
        inntekterSisteTreMåneder: List<Map<String, Any>>,
        forklaring: String,
        oppfylt: Boolean
    ) = observer.`§ 8-15`(skjæringstidspunkt, organisasjonsnummer, inntekterSisteTreMåneder, forklaring, oppfylt)

    override fun `§ 8-28 ledd 3 bokstav b`(
        organisasjonsnummer: String,
        startdatoArbeidsforhold: LocalDate,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlag: Inntekt
    ) =
        observer.`§ 8-28 ledd 3 bokstav b`(
            organisasjonsnummer,
            startdatoArbeidsforhold,
            overstyrtInntektFraSaksbehandler,
            skjæringstidspunkt,
            forklaring,
            grunnlagForSykepengegrunnlag
        )

    override fun `§ 8-28 ledd 3 bokstav c`(
        organisasjonsnummer: String,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlag: Inntekt
    ) =
        observer.`§ 8-28 ledd 3 bokstav c`(
            organisasjonsnummer,
            overstyrtInntektFraSaksbehandler,
            skjæringstidspunkt,
            forklaring,
            grunnlagForSykepengegrunnlag
        )

    override fun `§ 8-28 ledd 5`(
        organisasjonsnummer: String,
        overstyrtInntektFraSaksbehandler: Map<String, Any>,
        skjæringstidspunkt: LocalDate,
        forklaring: String,
        grunnlagForSykepengegrunnlag: Inntekt
    ) =
        observer.`§ 8-28 ledd 5`(
            organisasjonsnummer,
            overstyrtInntektFraSaksbehandler,
            skjæringstidspunkt,
            forklaring,
            grunnlagForSykepengegrunnlag
        )

    override fun `§ 8-28 ledd 3 bokstav a`(
        organisasjonsnummer: String,
        skjæringstidspunkt: LocalDate,
        inntekterSisteTreMåneder: List<Map<String, Any>>,
        grunnlagForSykepengegrunnlag: Inntekt
    ) =
        observer.`§ 8-28 ledd 3 bokstav a`(
            organisasjonsnummer,
            inntekterSisteTreMåneder,
            grunnlagForSykepengegrunnlag,
            skjæringstidspunkt
        )

    override fun `§ 8-29`(
        dato: LocalDate,
        omregnetÅrsinntekt: Inntekt,
        subsumsjonsformat: List<Map<String, Any>>,
        organisasjonsnummer: String
    ) = observer.`§ 8-29`(dato, omregnetÅrsinntekt, subsumsjonsformat, organisasjonsnummer)

    override fun `§ 8-30 ledd 1`(omregnetÅrsinntektPerArbeidsgiver: Map<String, Inntekt>, omregnetÅrsinntekt: Inntekt) =
        observer.`§ 8-30 ledd 1`(omregnetÅrsinntektPerArbeidsgiver, omregnetÅrsinntekt)

    override fun `§ 8-10 ledd 2 punktum 1`(
        erBegrenset: Boolean,
        maksimaltSykepengegrunnlag: Inntekt,
        skjæringstidspunkt: LocalDate,
        beregningsgrunnlag: Inntekt
    ) =
        observer.`§ 8-10 ledd 2 punktum 1`(
            erBegrenset,
            maksimaltSykepengegrunnlag,
            skjæringstidspunkt,
            beregningsgrunnlag
        )

    override fun `§ 8-51 ledd 2`(
        oppfyllerMinsteinntektskrav: Boolean,
        skjæringstidspunkt: LocalDate,
        alderPåDato: Int,
        beregningsgrunnlag: Inntekt,
        minsteinntekt: Inntekt
    ) =
        observer.`§ 8-51 ledd 2`(
            oppfyllerMinsteinntektskrav, skjæringstidspunkt, alderPåDato, beregningsgrunnlag,
            minsteinntekt
        )

    override fun `§ 8-3 ledd 2 punktum 1`(
        oppfyllerMinsteinntektskrav: Boolean,
        skjæringstidspunkt: LocalDate,
        beregningsgrunnlag: Inntekt,
        minsteinntekt: Inntekt
    ) {
        return observer.`§ 8-3 ledd 2 punktum 1`(
            oppfyllerMinsteinntektskrav,
            skjæringstidspunkt,
            beregningsgrunnlag,
            minsteinntekt
        )
    }
}