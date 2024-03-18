package no.nav.helse.dto.serialisering

import no.nav.helse.dto.ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto
import no.nav.helse.dto.InntektDto

data class SammenligningsgrunnlagUtDto(
    val sammenligningsgrunnlag: InntektDto,
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningForSammenligningsgrunnlagDto>,
)