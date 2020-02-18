package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Søknad
import java.time.LocalDate

internal class Feriedag internal constructor(gjelder: LocalDate, hendelseType: Kildehendelse) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitFeriedag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tFerie"

    override fun turneringsnøkkel(): Turneringsnøkkel =
        when (kildehendelse) {
            Søknad -> Turneringsnøkkel.V_A
            Inntektsmelding -> Turneringsnøkkel.V_IM
            else -> throw RuntimeException("Hendelse $kildehendelse er ikke støttet")
        }
}
