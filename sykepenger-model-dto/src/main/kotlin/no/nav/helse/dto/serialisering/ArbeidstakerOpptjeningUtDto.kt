package no.nav.helse.dto.serialisering

import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.PeriodeDto

sealed interface OpptjeningUtDto {
    val erOppfylt: Boolean
}

data class ArbeidstakerOpptjeningUtDto(
    val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagDto>,
    val opptjeningsperiode: PeriodeDto,
    val opptjeningsdager: Int,
    override val erOppfylt: Boolean
) : OpptjeningUtDto

data class SelvstendigOpptjeningUtDto(
    val opptjeningsperiode: PeriodeDto,
    override val erOppfylt: Boolean
) : OpptjeningUtDto
