package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class Permisjonsdag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse): Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitPermisjonsdag(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tPermisjonsdag"

    override fun dagType() = JsonDagType.PERMISJONSDAG

    override fun nøkkel(): Nøkkel =
        when(hendelse.nøkkelHendelseType()){
            NøkkelHendelseType.Søknad -> Nøkkel.Le_A
            else -> throw RuntimeException("Hendelse ${hendelse.nøkkelHendelseType()} er ikke støttet")
        }
}
