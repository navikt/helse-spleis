package no.nav.helse.hendelser.sykepengehistorikk

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

// Embedded document - json er output fra Spole
data class Sykepengehistorikk(val jsonNode: JsonNode) {
    val aktørId = jsonNode["aktørId"].textValue()
    val organisasjonsnummer = jsonNode["organisasjonsnummer"].textValue()
    val sakskompleksId = jsonNode["sakskompleksId"].textValue()
    val perioder get() =
        jsonNode["@løsning"]["perioder"]?.map { Periode(it) } ?: emptyList()

    fun toJson() = jsonNode

    data class Periode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val grad = jsonNode["grad"].textValue()
    }
}
