package no.nav.helse.hendelser

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

// Embedded document - json er output fra Spole
internal data class Sykepengehistorikk(private val jsonNode: JsonNode) {
    private val perioder get() = jsonNode["perioder"]?.map {
        Periode(
            it
        )
    } ?: emptyList()

    fun sisteFraværsdag() =
        perioder.maxBy { it.tom }?.tom

    internal data class Periode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val grad = jsonNode["grad"].textValue()
    }
}

