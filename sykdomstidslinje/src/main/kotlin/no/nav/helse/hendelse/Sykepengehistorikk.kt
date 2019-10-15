package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Embedded document - json er output fra Spole
data class Sykepengehistorikk(val json: JsonNode) : Sykdomshendelse {
    val aktørId = json["aktørId"].textValue()
    val organisasjonsnummer = json["organisasjonsnummer"].textValue()

    fun sakskompleksId() = UUID.fromString(json["sakskompleksId"].textValue())

    private fun perioder() =
        json["@løsning"]["perioder"]?.map { Periode(it) } ?: emptyList()

    override fun hendelsetype(): Sykdomshendelse.Type {
        return Sykdomshendelse.Type.Sykepengehistorikk
    }

    override fun aktørId(): String {
        return aktørId()
    }

    override fun rapportertdato(): LocalDateTime {
        TODO("not implemented")
    }

    override fun organisasjonsnummer(): String? = organisasjonsnummer

    override fun sykdomstidslinje() =
        perioder().fold(CompositeSykdomstidslinje(emptyList()) as Sykdomstidslinje) { aggregate, periode ->
            aggregate + Sykdomstidslinje.sykedager(periode.fom, periode.tom, this)
        }

    override fun toJson() = json

    fun påvirkerSakensMaksdato(sakensTidslinje: Sykdomstidslinje) =
        sykdomstidslinje().antallDagerMellom(sakensTidslinje) <= seksMånederIDager

    // forenkling for å kaste ut flest mulige saker. Vil ikke være relevant når vi faktisk regner maksdato
    private val seksMånederIDager = 6*31

    private data class Periode(val jsonNode: JsonNode) {
        val fom: LocalDate = LocalDate.parse(jsonNode["fom"].textValue())
        val tom: LocalDate = LocalDate.parse(jsonNode["tom"].textValue())
        val grad = jsonNode["grad"].textValue()
    }
}
