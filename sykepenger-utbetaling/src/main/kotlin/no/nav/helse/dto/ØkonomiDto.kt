package no.nav.helse.dto

data class ØkonomiDto(
    val grad: ProsentdelDto,
    val totalGrad: ProsentdelDto,
    val arbeidsgiverRefusjonsbeløp: InntektDto.DagligDouble,
    val aktuellDagsinntekt: InntektDto.DagligDouble,
    val beregningsgrunnlag: InntektDto.DagligDouble,
    val dekningsgrunnlag: InntektDto.DagligDouble,
    val grunnbeløpgrense: InntektDto.Årlig?,
    val arbeidsgiverbeløp: InntektDto.DagligDouble?,
    val personbeløp: InntektDto.DagligDouble?,
    val er6GBegrenset: Boolean?
)
data class ProsentdelDto(val prosent: Double)