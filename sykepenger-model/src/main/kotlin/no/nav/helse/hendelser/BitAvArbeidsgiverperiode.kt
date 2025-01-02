package no.nav.helse.hendelser

import no.nav.helse.sykdomstidslinje.Sykdomstidslinje

internal class BitAvArbeidsgiverperiode(val metadata: HendelseMetadata, private val sykdomstidslinje: Sykdomstidslinje) : SykdomshistorikkHendelse {
    override fun oppdaterFom(other: Periode) =
        other.oppdaterFom(sykdomstidslinje().periode() ?: other)

    override fun sykdomstidslinje() = sykdomstidslinje
}
