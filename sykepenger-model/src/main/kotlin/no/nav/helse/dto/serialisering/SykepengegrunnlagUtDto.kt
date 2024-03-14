package no.nav.helse.dto.serialisering

import no.nav.helse.dto.ArbeidsgiverInntektsopplysningDto
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.SammenligningsgrunnlagDto

data class SykepengegrunnlagUtDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val `6G`: InntektDto.Årlig
)