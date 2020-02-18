package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.Kildehendelse.Søknad
import java.time.LocalDate

internal class Egenmeldingsdag internal constructor(gjelder: LocalDate, hendelseType: Kildehendelse) : Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitEgenmeldingsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tEgenmeldingsdag"

    override fun turneringsnøkkel(): Turneringsnøkkel =
        when (kildehendelse) {
            Søknad -> Turneringsnøkkel.SRD_A
            Inntektsmelding -> Turneringsnøkkel.SRD_IM
            else -> throw RuntimeException("Hendelse $kildehendelse er ikke støttet")
        }
}
