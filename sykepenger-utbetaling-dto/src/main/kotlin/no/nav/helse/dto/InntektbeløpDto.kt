package no.nav.helse.dto

data class InntektDto(
    val årlig: InntektbeløpDto.Årlig,
    val månedligDouble: InntektbeløpDto.MånedligDouble,
    val dagligDouble: InntektbeløpDto.DagligDouble,
    val dagligInt: InntektbeløpDto.DagligInt,
)

sealed class InntektbeløpDto {
    abstract val beløp: Number

    data class Årlig(override val beløp: Double) : InntektbeløpDto()

    data class MånedligDouble(override val beløp: Double) : InntektbeløpDto()

    data class DagligDouble(override val beløp: Double) : InntektbeløpDto()

    data class DagligInt(override val beløp: Int) : InntektbeløpDto()
}
