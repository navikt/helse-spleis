package no.nav.helse.hendelser

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal data class BitAvArbeidsgiverperiode(
    val metadata: HendelseMetadata,
    val sykdomstidslinje: Sykdomstidslinje,
    val dagerNavOvertarAnsvar: List<Periode>
)
