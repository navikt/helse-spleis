package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate

internal class Egenmeldingsdag internal constructor(gjelder: LocalDate, hendelseType: NøkkelHendelseType) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitEgenmeldingsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tEgenmeldingsdag"

    override fun nøkkel(): Nøkkel =
        when (hendelseType) {
            Søknad -> Nøkkel.SRD_A
            Inntektsmelding -> Nøkkel.SRD_IM
            else -> throw RuntimeException("Hendelse $hendelseType er ikke støttet")
        }
}
