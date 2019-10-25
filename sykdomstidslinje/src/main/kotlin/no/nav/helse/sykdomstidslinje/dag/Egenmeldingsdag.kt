package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
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
        when (hendelse.nøkkelHendelseType()) {
            Søknad -> Nøkkel.SRD_A
            Inntektsmelding -> Nøkkel.SRD_IM
            else -> throw RuntimeException("Hendelse ${hendelse.nøkkelHendelseType()} er ikke støttet")
        }
}
