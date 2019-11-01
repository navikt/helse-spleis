package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime

abstract class SykdomstidslinjeHendelse(private val hendelseId: String): Comparable<SykdomstidslinjeHendelse> {

    internal companion object {
        private val objectMapper = ObjectMapper()
    }

    fun hendelseId() =
        hendelseId

    override fun compareTo(other: SykdomstidslinjeHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    abstract fun rapportertdato(): LocalDateTime
    abstract fun sykdomstidslinje(): Sykdomstidslinje

    abstract fun nøkkelHendelseType(): Dag.NøkkelHendelseType

    open fun toJson(): JsonNode {
        return objectMapper.valueToTree(mapOf(
            "hendelseId" to hendelseId()
        ))
    }

    interface Deserializer{
        fun deserialize(jsonNode: JsonNode): SykdomstidslinjeHendelse
    }

    override fun equals(other: Any?) =
        other is SykdomstidslinjeHendelse && other.hendelseId == this.hendelseId

    override fun hashCode() = hendelseId.hashCode()
}
