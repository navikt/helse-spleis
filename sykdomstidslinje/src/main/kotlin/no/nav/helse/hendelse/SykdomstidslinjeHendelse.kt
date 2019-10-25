package no.nav.helse.hendelse

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate
import java.time.LocalDateTime

interface PersonHendelse {
    fun aktørId(): String
    fun organisasjonsnummer(): String?
}

interface SakskompleksHendelse {

    fun sakskompleksId(): String
}

abstract class SykdomstidslinjeHendelse(private val hendelseId: String): Comparable<SykdomstidslinjeHendelse> {
    enum class Type {
        SendtSøknadMottatt,
        NySøknadMottatt,
        InntektsmeldingMottatt,
        SykepengehistorikkMottatt
    }

    internal companion object {
        fun fromJson(json: JsonNode): SykdomstidslinjeHendelse {
            val type = json["type"].asText()
            return when (type) {
                Type.InntektsmeldingMottatt.name -> InntektsmeldingHendelse.fromJson(json)
                Type.NySøknadMottatt.name -> NySøknadHendelse.fromJson(json)
                Type.SendtSøknadMottatt.name -> SendtSøknadHendelse.fromJson(json)
                Type.SykepengehistorikkMottatt.name -> SykepengehistorikkHendelse.fromJson(json)
                else -> throw RuntimeException("ukjent type: $type for melding ${json["hendelseId"]?.asText()}")
            }
        }

        private val objectMapper = ObjectMapper()
    }

    fun hendelseId() =
        hendelseId

    override fun compareTo(other: SykdomstidslinjeHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    abstract fun hendelsetype(): Type

    abstract fun rapportertdato(): LocalDateTime
    abstract fun sykdomstidslinje(): Sykdomstidslinje

    internal open fun toJson(): JsonNode {
        return objectMapper.valueToTree(mapOf(
            "type" to hendelsetype().name,
            "hendelseId" to hendelseId()
        ))
    }

    override fun equals(other: Any?) =
        other is SykdomstidslinjeHendelse && other.hendelseId == this.hendelseId
}

fun JsonNode.safelyUnwrapDate(): LocalDate? {
    return if (isNull) {
        null
    } else {
        LocalDate.parse(textValue())
    }
}
