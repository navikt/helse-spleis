package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.NyInntektUnderveisDto

data class InntektsgrunnlagInnDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningInnDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningInnDto>,
    val tilkommendeInntekter: List<NyInntektUnderveisDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagInnDto,
    val `6G`: InntektbeløpDto.Årlig
)