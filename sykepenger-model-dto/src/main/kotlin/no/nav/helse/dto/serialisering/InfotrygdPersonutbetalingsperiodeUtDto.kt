package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.PeriodeDto
import no.nav.helse.dto.ProsentdelDto

data class InfotrygdPersonutbetalingsperiodeUtDto(
    val orgnr: String,
    val periode: PeriodeDto,
    val grad: ProsentdelDto,
    val inntekt: InntektDto
)