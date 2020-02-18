package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Sykmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Søknad
import java.time.LocalDate

internal class Sykedag internal constructor(gjelder: LocalDate, hendelseType: Kildehendelse) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tSykedag"

    override fun turneringsnøkkel(): Turneringsnøkkel {
        return when (kildehendelse) {
            Søknad -> Turneringsnøkkel.S_A
            Sykmelding -> Turneringsnøkkel.S_SM
            else -> throw RuntimeException("Hendelse $kildehendelse er ikke støttet")
        }
    }
}
