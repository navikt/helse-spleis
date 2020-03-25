package no.nav.helse.testhelpers

import no.nav.helse.hendelser.Søknad
import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import java.time.LocalDate

private var dagensDato = LocalDate.of(2018, 1, 1)

internal fun resetSeed(frøDato: LocalDate = LocalDate.of(2018, 1, 1)) {
    dagensDato = frøDato
}

internal val Int.S
    get() = Sykdomstidslinje.sykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        100.0,
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.F
    get() = Sykdomstidslinje.ferie(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.A
    get() = Sykdomstidslinje.ikkeSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.I
    get() = Sykdomstidslinje.implisitteDager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.E
    get() = Sykdomstidslinje.egenmeldingsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.P
    get() = Sykdomstidslinje.permisjonsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.EDU
    get() = Sykdomstidslinje.studiedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.UT
    get() = Sykdomstidslinje.utenlandsdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.U
    get() = Sykdomstidslinje.ubestemtdager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }

internal val Int.FO
    get() = Sykdomstidslinje.kunArbeidsgiverSykedager(
        dagensDato, dagensDato.plusDays(this.toLong() - 1),
        100.0,
        Søknad.SøknadDagFactory
    ).also { dagensDato = dagensDato.plusDays(this.toLong()) }
