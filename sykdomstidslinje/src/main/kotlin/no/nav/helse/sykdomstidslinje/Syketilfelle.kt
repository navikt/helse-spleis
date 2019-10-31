package no.nav.helse.sykdomstidslinje

data class Syketilfelle(
    val arbeidsgiverperiode: Sykdomstidslinje,
    val dagerEtterArbeidsgiverperiode: Sykdomstidslinje?
) {
    val tidslinje
        get() = when {
            dagerEtterArbeidsgiverperiode != null -> arbeidsgiverperiode.plus(dagerEtterArbeidsgiverperiode)
            else -> arbeidsgiverperiode
        }
}

