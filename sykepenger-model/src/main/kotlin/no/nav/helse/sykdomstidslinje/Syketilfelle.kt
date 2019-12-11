package no.nav.helse.sykdomstidslinje

internal data class Syketilfelle(
    val arbeidsgiverperiode: ConcreteSykdomstidslinje,
    val dagerEtterArbeidsgiverperiode: ConcreteSykdomstidslinje?
) {
    val tidslinje
        get() = when {
            dagerEtterArbeidsgiverperiode != null -> arbeidsgiverperiode.plus(dagerEtterArbeidsgiverperiode)
            else -> arbeidsgiverperiode
        }
}

