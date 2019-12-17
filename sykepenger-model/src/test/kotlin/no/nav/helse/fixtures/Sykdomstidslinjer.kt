package no.nav.helse.fixtures

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import java.time.LocalDate

private val frøDato = LocalDate.of(2018, 1, 1)
private var dagensDato = frøDato
private val nesteDato get() = dagensDato.also { dagensDato = dagensDato.plusDays(1) }

internal fun resetSeed() {
    dagensDato = frøDato
}

internal val sendtSykmelding =
    Testhendelse(3.mandag.atStartOfDay())

internal val Int.S
    get() = ConcreteSykdomstidslinje.sykedager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.F
    get() = ConcreteSykdomstidslinje.ferie(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.A
    get() = ConcreteSykdomstidslinje.ikkeSykedager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.I
    get() = ConcreteSykdomstidslinje.implisittdager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.E
    get() = ConcreteSykdomstidslinje.egenmeldingsdager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.P
    get() = ConcreteSykdomstidslinje.permisjonsdager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.EDU
    get() = ConcreteSykdomstidslinje.studiedager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.UT
    get() = ConcreteSykdomstidslinje.utenlandsdager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )

internal val Int.U
    get() = ConcreteSykdomstidslinje.ubestemtdager(
        nesteDato, nesteDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    )
