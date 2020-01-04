package no.nav.helse.sykdomstidslinje

import no.nav.helse.person.ArbeidstakerHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.util.*

abstract class SykdomstidslinjeHendelse(hendelseId: UUID, hendelsetype: Hendelsetype) :
    ArbeidstakerHendelse(hendelseId, hendelsetype) {
    internal abstract fun sykdomstidslinje(): ConcreteSykdomstidslinje

    internal abstract fun nøkkelHendelseType(): Dag.NøkkelHendelseType

    companion object Builder {
        fun fromJson(json: String): SykdomstidslinjeHendelse {
            return when (val hendelse = ArbeidstakerHendelse.fromJson(json)) {
                is SykdomstidslinjeHendelse -> hendelse
                else -> throw RuntimeException("kjenner ikke hendelsetypen ${hendelse.hendelsetype()}")
            }
        }
    }
}
