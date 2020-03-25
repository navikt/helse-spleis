package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.NySykdomstidslinje
import java.time.LocalDate

private var dagensDato = LocalDate.of(2018, 1, 1)

internal fun resetSeed(frøDato: LocalDate = LocalDate.of(2018, 1, 1)) {
    dagensDato = frøDato
}

internal val Int.S
    get() = NySykdomstidslinje.sykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        100.0,
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = NySykdomstidslinje.ferie(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = NySykdomstidslinje.ikkeSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.I
    get() = NySykdomstidslinje.implisitteDager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.E
    get() = NySykdomstidslinje.egenmeldingsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = NySykdomstidslinje.permisjonsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.EDU
    get() = NySykdomstidslinje.studiedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UT
    get() = NySykdomstidslinje.utenlandsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = NySykdomstidslinje.ubestemtdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.FO
    get() = NySykdomstidslinje.kunArbeidsgiverSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        100.0,
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }
