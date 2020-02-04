package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Sykmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate

internal class Sykedag internal constructor(gjelder: LocalDate, hendelseType: NøkkelHendelseType) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tSykedag"

    override fun dagType(): JsonDagType = JsonDagType.SYKEDAG

    override fun nøkkel(): Nøkkel {
        return when (hendelseType) {
            Søknad -> Nøkkel.S_A
            Sykmelding -> Nøkkel.S_SM
            else -> throw RuntimeException("Hendelse $hendelseType er ikke støttet")
        }
    }
}
