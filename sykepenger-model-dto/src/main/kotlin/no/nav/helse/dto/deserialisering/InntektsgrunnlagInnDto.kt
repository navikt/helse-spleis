package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto

data class InntektsgrunnlagInnDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningInnDto>,
    val selvstendigInntektsopplysning: SelvstendigInntektsopplysningInnDto?,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningInnDto>,
    val vurdertInfotrygd: Boolean,
    val `6G`: InntektbeløpDto.Årlig
)
