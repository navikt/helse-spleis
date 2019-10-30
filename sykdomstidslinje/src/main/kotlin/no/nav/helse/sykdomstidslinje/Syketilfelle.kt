package no.nav.helse.sykdomstidslinje

import no.nav.helse.sykdomstidslinje.dag.Dag
import no.nav.helse.sykdomstidslinje.dag.Sykedag
import java.math.BigDecimal

data class Syketilfelle(
    val arbeidsgiverperiode: Sykdomstidslinje?,
    val dagerEtterArbeidsgiverperiode: Sykdomstidslinje?
) {
    init {
        assert(arbeidsgiverperiode != null || dagerEtterArbeidsgiverperiode != null)
    }

    val tidslinje
        get() = when {
            dagerEtterArbeidsgiverperiode != null -> arbeidsgiverperiode?.plus(dagerEtterArbeidsgiverperiode)
                ?: dagerEtterArbeidsgiverperiode
            arbeidsgiverperiode != null -> arbeidsgiverperiode
            else -> throw RuntimeException("Arbeidsgiverperiode og dager etter arbeidsgiverperiode er begge null. Syketilfellet inneholder ingen data")
        }
}

