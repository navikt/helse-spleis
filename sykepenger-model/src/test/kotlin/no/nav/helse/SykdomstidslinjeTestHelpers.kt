package no.nav.helse

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje
import no.nav.helse.sykdomstidslinje.merge
import no.nav.helse.tournament.Dagturnering

internal fun perioder(
    periode1: Sykdomstidslinje,
    periode2: Sykdomstidslinje,
    test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje) -> Unit
) {
    listOf(periode1, periode2).merge(Dagturnering.TURNERING::beste).test(periode1, periode2)
}

internal fun perioder(
    periode1: Sykdomstidslinje,
    periode2: Sykdomstidslinje,
    periode3: Sykdomstidslinje,
    test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje) -> Unit
) {
    listOf(periode1, periode2, periode3).merge(Dagturnering.TURNERING::beste).test(periode1, periode2, periode3)
}

internal fun perioder(
    periode1: Sykdomstidslinje,
    periode2: Sykdomstidslinje,
    periode3: Sykdomstidslinje,
    periode4: Sykdomstidslinje,
    test: Sykdomstidslinje.(Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje, Sykdomstidslinje) -> Unit
) {
    listOf(periode1, periode2, periode3, periode4).merge(Dagturnering.TURNERING::beste)
        .test(periode1, periode2, periode3, periode4)
}
