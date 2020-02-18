package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate

internal class Feriedag internal constructor(gjelder: LocalDate, hendelseType: NøkkelHendelseType) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitFeriedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tFerie"

    override fun nøkkel(): Nøkkel =
        when (hendelseType) {
            Søknad -> Nøkkel.V_A
            Inntektsmelding -> Nøkkel.V_IM
            else -> throw RuntimeException("Hendelse $hendelseType er ikke støttet")
        }
}
