package no.nav.helse.dto

data class ØkonomiDto(
    val grad: ProsentdelDto,
    val totalGrad: ProsentdelDto,
    val arbeidsgiverRefusjonsbeløp: InntektDto,
    val aktuellDagsinntekt: InntektDto,
    val beregningsgrunnlag: InntektDto,
    val dekningsgrunnlag: InntektDto,
    val grunnbeløpgrense: InntektDto?,
    val arbeidsgiverbeløp: InntektDto?,
    val personbeløp: InntektDto?,
    val er6GBegrenset: Boolean?
)
data class ProsentdelDto(val prosent: Double)