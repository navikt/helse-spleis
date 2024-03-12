package no.nav.helse.dto

sealed class InntektDto {
    abstract val beløp: Number

    data class Årlig(override val beløp: Double) : InntektDto()
    data class MånedligDouble(override val beløp: Double) : InntektDto()
    data class MånedligInt(override val beløp: Int) : InntektDto()
    data class DagligDouble(override val beløp: Double) : InntektDto()
    data class DagligInt(override val beløp: Int) : InntektDto()
}

