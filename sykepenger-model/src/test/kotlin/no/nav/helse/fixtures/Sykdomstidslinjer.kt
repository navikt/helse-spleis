package no.nav.helse.fixtures

import no.nav.helse.hendelser.Testhendelse
import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import java.time.LocalDate

private val frøDato = LocalDate.of(2018, 1, 1)
private var dagensDato = frøDato

internal fun resetSeed() {
    dagensDato = frøDato
}

internal val sendtSykmelding =
    Testhendelse(3.mandag.atStartOfDay())

internal val Int.S
    get() = ConcreteSykdomstidslinje.sykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = ConcreteSykdomstidslinje.ferie(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = ConcreteSykdomstidslinje.ikkeSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.I
    get() = ConcreteSykdomstidslinje.implisittdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.E
    get() = ConcreteSykdomstidslinje.egenmeldingsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = ConcreteSykdomstidslinje.permisjonsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.EDU
    get() = ConcreteSykdomstidslinje.studiedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UT
    get() = ConcreteSykdomstidslinje.utenlandsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = ConcreteSykdomstidslinje.ubestemtdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        sendtSykmelding
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }
