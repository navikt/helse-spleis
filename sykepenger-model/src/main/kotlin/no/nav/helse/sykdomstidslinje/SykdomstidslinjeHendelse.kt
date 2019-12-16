package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime

abstract class SykdomstidslinjeHendelse(private val hendelseId: String): Comparable<SykdomstidslinjeHendelse> {

    fun hendelseId() = hendelseId

    override fun compareTo(other: SykdomstidslinjeHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    internal abstract fun rapportertdato(): LocalDateTime
    internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

    internal abstract fun nøkkelHendelseType(): Dag.NøkkelHendelseType

    abstract fun toJson(): JsonNode

    interface Deserializer{
        fun deserialize(jsonNode: JsonNode): SykdomstidslinjeHendelse
    }

    override fun equals(other: Any?) =
        other is SykdomstidslinjeHendelse && other.hendelseId == this.hendelseId

    override fun hashCode() = hendelseId.hashCode()
}
