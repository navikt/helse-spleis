package no.nav.helse.sykdomstidslinje.dag

import no.nav.helse.hendelse.Sykdomshendelse
import no.nav.helse.hendelse.Sykdomshendelse.Type.Inntektsmelding
import no.nav.helse.hendelse.Sykdomshendelse.Type.SendtSykepengesøknad
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeVisitor
import java.time.LocalDate

class Arbeidsdag internal constructor(gjelder: LocalDate, hendelse: Sykdomshendelse) : Dag(gjelder, hendelse) {
    override fun accept(visitor: SykdomstidslinjeVisitor) {
        visitor.visitArbeidsdag(this)
    }

    override fun antallSykedagerHvorViIkkeTellerMedHelg() = 0

    override fun antallSykedagerHvorViTellerMedHelg() = 0

    override fun toString() = formatter.format(dagen) + "\tArbeidsdag"

    override fun dagType(): JsonDagType = JsonDagType.ARBEIDSDAG

    override fun nøkkel(): Nøkkel =
        when (hendelse.hendelsetype()) {
            SendtSykepengesøknad -> Nøkkel.WD_A
            Inntektsmelding -> Nøkkel.WD_IM
            else -> throw RuntimeException("Hendelse ${hendelse.hendelsetype()} er ikke støttet")
        }
}
