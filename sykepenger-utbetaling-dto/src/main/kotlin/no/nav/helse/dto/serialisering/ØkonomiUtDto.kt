package no.nav.helse.dto.serialisering

import no.nav.helse.dto.InntektDto
import no.nav.helse.dto.ProsentdelDto

data class ØkonomiUtDto(
    val grad: ProsentdelDto,
    val totalGrad: ProsentdelDto,
    val utbetalingsgrad: ProsentdelDto,
    val arbeidsgiverRefusjonsbeløp: InntektDto,
    val aktuellDagsinntekt: InntektDto,
    val dekningsgrunnlag: InntektDto,
    val dekningsgrad: ProsentdelDto,
    val arbeidsgiverbeløp: InntektDto?,
    val personbeløp: InntektDto?
)
