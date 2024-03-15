package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.ArbeidsgiverInntektsopplysningDto
import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.SammenligningsgrunnlagDto

data class SykepengegrunnlagInnDto(
    val arbeidsgiverInntektsopplysninger: List<ArbeidsgiverInntektsopplysningDto>,
    val deaktiverteArbeidsforhold: List<ArbeidsgiverInntektsopplysningDto>,
    val vurdertInfotrygd: Boolean,
    val sammenligningsgrunnlag: SammenligningsgrunnlagDto,
    val `6G`: InntektDto.Årlig
)