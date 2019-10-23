package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.helse.sykdomstidslinje.CompositeSykdomstidslinje
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// Embedded document - json er output fra Spole
data class Sykepengehistorikk(val jsonNode: JsonNode) : DokumentMottattHendelse {
    init {
        if (!jsonNode.hasNonNull("hendelseId")) {
            (jsonNode as ObjectNode).put("hendelseId", UUID.randomUUID().toString())
        }
    }

    val hendelseId = jsonNode["hendelseId"].asText()!!
    override fun hendelseId() = hendelseId

    val aktørId = jsonNode["aktørId"].textValue()
    val organisasjonsnummer = jsonNode["organisasjonsnummer"].textValue()

    fun sakskompleksId() = UUID.fromString(jsonNode["sakskompleksId"].textValue())

    private fun perioder() =
        jsonNode["@løsning"]["perioder"]?.map { Periode(it) } ?: emptyList()

    override fun hendelsetype(): DokumentMottattHendelse.Type {
        return DokumentMottattHendelse.Type.Sykepengehistorikk
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

    override fun toJson() = jsonNode

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
