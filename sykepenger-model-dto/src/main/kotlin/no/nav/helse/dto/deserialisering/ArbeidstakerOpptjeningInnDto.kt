package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.PeriodeDto

data class OpptjeningInnDto(
    val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagDto>,
    val opptjeningsperiode: PeriodeDto,
)
