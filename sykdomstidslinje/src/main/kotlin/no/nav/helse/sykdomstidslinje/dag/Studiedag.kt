package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.DokumentMottattHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Studiedag internal constructor(gjelder: LocalDate, hendelse: DokumentMottattHendelse): Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitStudiedag(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tStudiedag"

    override fun dagType() = JsonDagType.STUDIEDAG

    override fun nøkkel(): Nøkkel = Nøkkel.EDU
}
