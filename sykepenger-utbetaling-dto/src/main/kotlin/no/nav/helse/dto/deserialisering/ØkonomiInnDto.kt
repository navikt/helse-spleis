package no.nav.helse.dto.deserialisering

import no.nav.helse.dto.InntektbeløpDto
import no.nav.helse.dto.ProsentdelDto

data class ØkonomiInnDto(
    val grad: ProsentdelDto,
    val totalGrad: ProsentdelDto,
    val arbeidsgiverRefusjonsbeløp: InntektbeløpDto.DagligDouble,
    val aktuellDagsinntekt: InntektbeløpDto.DagligDouble,
    val beregningsgrunnlag: InntektbeløpDto.DagligDouble,
    val dekningsgrunnlag: InntektbeløpDto.DagligDouble,
    val grunnbeløpgrense: InntektbeløpDto.Årlig?,
    val arbeidsgiverbeløp: InntektbeløpDto.DagligDouble?,
    val personbeløp: InntektbeløpDto.DagligDouble?,
    val er6GBegrenset: Boolean?
)