package no.nav.helse.dto.serialisering

import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.PeriodeDto

data class OpptjeningUtDto(
    val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagDto>,
    val opptjeningsperiode: PeriodeDto,
    val opptjeningsdager: Int,
    val erOppfylt: Boolean
)

