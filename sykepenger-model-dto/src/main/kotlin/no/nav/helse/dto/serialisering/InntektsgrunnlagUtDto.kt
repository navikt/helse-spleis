package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto

data class InntektsgrunnlagUtDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningUtDto>,
    val selvstendigInntektsopplysning: SelvstendigInntektsopplysningUtDto?,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningUtDto>,
    val vurdertInfotrygd: Boolean,
    val `6G`: InntektDto,
    val sykepengegrunnlag: InntektDto,
    val totalOmregnetÅrsinntekt: InntektDto,
    val beregningsgrunnlag: InntektDto,
    val er6GBegrenset: Boolean
)
