package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto

data class InfotrygdPersonutbetalingsperiodeInnDto(
    val orgnr: String,
    val periode: PeriodeDto,
    val grad: ProsentdelDto,
    val inntekt: InntektbeløpDto.DagligInt
)