package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Permisjonsdag internal constructor(gjelder: LocalDate, hendelseType: NøkkelHendelseType): Dag(gjelder, hendelseType) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitPermisjonsdag(this)
    }

    override fun toString() = formatter.format(dagen) + "\tPermisjonsdag"

    override fun dagType() = JsonDagType.PERMISJONSDAG

    override fun nøkkel(): Nøkkel =
        when(hendelseType){
            NøkkelHendelseType.Søknad -> Nøkkel.Le_A
            else -> throw RuntimeException("Hendelse $hendelseType er ikke støttet")
        }
}
