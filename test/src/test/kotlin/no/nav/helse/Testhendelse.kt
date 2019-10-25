package no.nav.helse

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.SykdomstidslinjeHendelse
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDateTime
import java.util.*

class Testhendelse(
    private val rapportertdato: LocalDateTime = LocalDateTime.of(2019, 9, 16, 10, 45),
    private val hendelsetype: Dag.NøkkelHendelseType = Dag.NøkkelHendelseType.Søknad
) :
    SykdomstidslinjeHendelse(UUID.randomUUID().toString()) {
    override fun nøkkelHendelseType(): Dag.NøkkelHendelseType = hendelsetype

    override fun sykdomstidslinje(): Sykdomstidslinje {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun rapportertdato(): LocalDateTime {
        return rapportertdato
    }

    override fun compareTo(other: SykdomstidslinjeHendelse): Int {
        return this.rapportertdato().compareTo(other.rapportertdato())
    }


    override fun equals(other: Any?): Boolean = this === other
}
