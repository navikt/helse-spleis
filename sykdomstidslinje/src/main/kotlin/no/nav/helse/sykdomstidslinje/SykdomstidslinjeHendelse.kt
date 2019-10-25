package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate
import java.time.LocalDateTime

abstract class SykdomstidslinjeHendelse(private val hendelseId: String): Comparable<SykdomstidslinjeHendelse> {

    internal companion object {
        private val objectMapper = ObjectMapper()
    }

    fun hendelseId() =
        hendelseId

    override fun compareTo(other: SykdomstidslinjeHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    protected abstract fun hendelsetype(): String

    abstract fun rapportertdato(): LocalDateTime
    abstract fun sykdomstidslinje(): Sykdomstidslinje

    abstract fun nøkkelHendelseType(): Dag.NøkkelHendelseType

    internal open fun toJson(): JsonNode {
        return objectMapper.valueToTree(mapOf(
            "type" to hendelsetype(),
            "hendelseId" to hendelseId()
        ))
    }



    override fun equals(other: Any?) =
        other is SykdomstidslinjeHendelse && other.hendelseId == this.hendelseId


    interface Deserializer{
        fun deserialize(jsonNode: JsonNode): SykdomstidslinjeHendelse
    }
}

fun JsonNode.safelyUnwrapDate(): LocalDate? {
    return if (isNull) {
        null
    } else {
        LocalDate.parse(textValue())
    }
}
