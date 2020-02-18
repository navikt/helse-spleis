package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Søknad
import java.time.LocalDate

internal class Arbeidsdag internal constructor(
    gjelder: LocalDate,
    hendelseType: Kildehendelse
) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitArbeidsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"

    override fun turneringsnøkkel(): Turneringsnøkkel =
        when (kildehendelse) {
            Søknad -> Turneringsnøkkel.WD_A
            Inntektsmelding -> Turneringsnøkkel.WD_IM
            else -> throw RuntimeException("Hendelse $kildehendelse er ikke støttet")
        }
}
