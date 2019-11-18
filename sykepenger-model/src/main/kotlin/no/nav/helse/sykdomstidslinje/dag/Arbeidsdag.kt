package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Inntektsmelding
import no.nav.helse.sykdomstidslinje.dag.Dag.NøkkelHendelseType.Søknad
import java.time.LocalDate

internal class Arbeidsdag internal constructor(gjelder: LocalDate, hendelse: SykdomstidslinjeHendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitArbeidsdag(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"

    override fun dagType(): JsonDagType = JsonDagType.ARBEIDSDAG

    override fun nøkkel(): Nøkkel =
        when (hendelse.nøkkelHendelseType()) {
             Søknad -> Nøkkel.WD_A
             Inntektsmelding -> Nøkkel.WD_IM
            else -> throw RuntimeException("Hendelse ${hendelse.nøkkelHendelseType()} er ikke støttet")
        }
}
