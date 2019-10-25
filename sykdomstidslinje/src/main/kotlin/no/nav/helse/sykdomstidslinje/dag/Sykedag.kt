package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Sykmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate

class Sykedag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 1
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 1

    override fun toString() = formatter.format(dagen) + "\tSykedag"

    override fun dagType(): JsonDagType = JsonDagType.SYKEDAG

    override fun nøkkel(): Nøkkel {
        return when (hendelse.nøkkelHendelseType()) {
            Søknad -> Nøkkel.S_A
            Sykmelding -> Nøkkel.S_SM
            else -> throw RuntimeException("Hendelse ${hendelse.nøkkelHendelseType()} er ikke støttet")
        }
    }
}
