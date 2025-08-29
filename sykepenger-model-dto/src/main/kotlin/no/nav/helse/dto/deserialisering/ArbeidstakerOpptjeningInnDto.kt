package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.ArbeidsgiverOpptjeningsgrunnlagDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.serialisering.OpptjeningUtDto

sealed interface OpptjeningInnDto {

}

data class ArbeidstakerOpptjeningInnDto(
    val arbeidsforhold: List<ArbeidsgiverOpptjeningsgrunnlagDto>,
    val opptjeningsperiode: PeriodeDto
) : OpptjeningInnDto

data class SelvstendigOpptjeningInnDto(
    val erOppfylt: Boolean
) : OpptjeningInnDto
