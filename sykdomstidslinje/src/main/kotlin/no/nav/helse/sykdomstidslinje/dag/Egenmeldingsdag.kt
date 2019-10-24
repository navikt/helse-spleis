package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.SykdomstidslinjeHendelse
import no.nav.helse.hendelse.SykdomstidslinjeHendelse.Type.InntektsmeldingMottatt
import no.nav.helse.hendelse.SykdomstidslinjeHendelse.Type.SendtSøknadMottatt
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Egenmeldingsdag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitEgenmeldingsdag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 1
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tEgenmeldingsdag"

    override fun dagType() = JsonDagType.EGENMELDINGSDAG

    override fun nøkkel(): Nøkkel =
        when (hendelse.hendelsetype()) {
            SendtSøknadMottatt -> Nøkkel.SRD_A
            InntektsmeldingMottatt -> Nøkkel.SRD_IM
            else -> throw RuntimeException("Hendelse er ikke støttet")
        }
}
