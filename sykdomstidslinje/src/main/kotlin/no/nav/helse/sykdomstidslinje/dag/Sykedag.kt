package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Sykedag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitSykedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 1
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 1

    override fun toString() = formatter.format(dagen) + "\tSykedag"

    override fun dagType(): JsonDagType = JsonDagType.SYKEDAG

    override fun nøkkel(): Nøkkel =
        when (hendelse.hendelsetype()) {
            SykdomstidslinjeHendelse.Type.SendtSøknadMottatt -> Nøkkel.S_A
            SykdomstidslinjeHendelse.Type.NySøknadMottatt -> Nøkkel.S_SM
            else -> throw RuntimeException("Hendelse ${hendelse.hendelsetype()} er ikke støttet")
        }
}
