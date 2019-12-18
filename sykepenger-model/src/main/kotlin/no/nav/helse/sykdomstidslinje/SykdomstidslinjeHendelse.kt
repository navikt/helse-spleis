package no.nav.helse.sykdomstidslinje

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.sak.ArbeidstakerHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

abstract class SykdomstidslinjeHendelse(hendelseId: UUID, hendelsetype: Hendelsetype): ArbeidstakerHendelse(hendelseId, hendelsetype), Comparable<SykdomstidslinjeHendelse> {

    override fun compareTo(other: SykdomstidslinjeHendelse) = this.rapportertdato().compareTo(other.rapportertdato())

    internal abstract fun rapportertdato(): LocalDateTime
    internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

    internal abstract fun nøkkelHendelseType(): Dag.NøkkelHendelseType

    internal abstract fun toJsonNode(): JsonNode

    companion object Builder {
        fun fromJson(json: String): SykdomstidslinjeHendelse {
            return when (val hendelse = ArbeidstakerHendelse.fromJson(json)) {
                is SykdomstidslinjeHendelse -> hendelse
                else -> throw RuntimeException("kjenner ikke hendelsetypen ${hendelse.hendelsetype()}")
            }
        }
    }
}
