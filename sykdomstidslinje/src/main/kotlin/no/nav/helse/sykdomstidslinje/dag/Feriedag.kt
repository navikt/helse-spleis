package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.DokumentMottattHendelse
import no.nav.helse.hendelse.DokumentMottattHendelse.Type.InntektsmeldingMottatt
import no.nav.helse.hendelse.DokumentMottattHendelse.Type.SendtSøknadMottatt
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Feriedag internal constructor(gjelder: LocalDate, hendelse: DokumentMottattHendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitFeriedag(this)
    }

    override fun antallSykedagerHvorViTellerMedHelg() = 0
    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tFerie"

    override fun dagType(): JsonDagType = JsonDagType.FERIEDAG

    override fun nøkkel(): Nøkkel =
        when (hendelse.hendelsetype()) {
            SendtSøknadMottatt -> Nøkkel.V_A
            InntektsmeldingMottatt -> Nøkkel.V_IM
            else -> throw RuntimeException("Hendelse er ikke støttet")
        }
}
