package no.nav.helse.testhelpers

import no.nav.helse.sykdomstidslinje.ConcreteSykdomstidslinje
import no.nav.helse.sykdomstidslinje.dag.Dag
import java.time.LocalDate

private var dagensDato = LocalDate.of(2018, 1, 1)

internal fun resetSeed(frøDato: LocalDate = LocalDate.of(2018, 1, 1)) {
    dagensDato = frøDato
}

internal val Int.S
    get() = ConcreteSykdomstidslinje.sykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = ConcreteSykdomstidslinje.ferie(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = ConcreteSykdomstidslinje.ikkeSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.I
    get() = ConcreteSykdomstidslinje.implisittdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.E
    get() = ConcreteSykdomstidslinje.egenmeldingsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = ConcreteSykdomstidslinje.permisjonsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.EDU
    get() = ConcreteSykdomstidslinje.studiedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UT
    get() = ConcreteSykdomstidslinje.utenlandsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = ConcreteSykdomstidslinje.ubestemtdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Dag.Kildehendelse.Søknad
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }
