package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.ProsentdelDto

data class ØkonomiInnDto(
    val grad: ProsentdelDto,
    val totalGrad: ProsentdelDto,
    val utbetalingsgrad: ProsentdelDto,
    val arbeidsgiverRefusjonsbeløp: InntektbeløpDto.DagligDouble,
    val aktuellDagsinntekt: InntektbeløpDto.DagligDouble,
    val inntektjustering: InntektbeløpDto.DagligDouble,
    val dekningsgrad: ProsentdelDto,
    val arbeidsgiverbeløp: InntektbeløpDto.DagligDouble?,
    val personbeløp: InntektbeløpDto.DagligDouble?,
    val reservertArbeidsgiverbeløp: InntektbeløpDto.DagligDouble?,
    val reservertPersonbeløp: InntektbeløpDto.DagligDouble?,
)
