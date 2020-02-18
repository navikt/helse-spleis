package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate

internal class Arbeidsdag internal constructor(
    gjelder: LocalDate,
    hendelseType: NøkkelHendelseType
) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitArbeidsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"

    override fun nøkkel(): Nøkkel =
        when (hendelseType) {
            Søknad -> Nøkkel.WD_A
            Inntektsmelding -> Nøkkel.WD_IM
            else -> throw RuntimeException("Hendelse $hendelseType er ikke støttet")
        }
}
