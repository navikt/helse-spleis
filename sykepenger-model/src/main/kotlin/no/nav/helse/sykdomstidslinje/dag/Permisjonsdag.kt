package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Permisjonsdag internal constructor(gjelder: LocalDate, hendelseType: Kildehendelse): Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitPermisjonsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tPermisjonsdag"

    override fun turneringsnøkkel(): Turneringsnøkkel =
        when(kildehendelse){
            Kildehendelse.Søknad -> Turneringsnøkkel.Le_A
            else -> throw RuntimeException("Hendelse $kildehendelse er ikke støttet")
        }
}
