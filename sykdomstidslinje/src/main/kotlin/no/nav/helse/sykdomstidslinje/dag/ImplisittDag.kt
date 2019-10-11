package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

internal class ImplisittDag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse) :
    Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {}

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun antallSykedagerHvorViIkkeTellerMedHelg(): Int = 0

    override fun tilDag() = ikkeSykedag(dagen, hendelse)

    override fun toString() = formatter.format(dagen) + "\tImplisitt arbeidsdag"

    override fun dagType() = throw RuntimeException("Not implemented, should never be serialized")

    override fun nøkkel(): Nøkkel = Nøkkel.WD_I
}
