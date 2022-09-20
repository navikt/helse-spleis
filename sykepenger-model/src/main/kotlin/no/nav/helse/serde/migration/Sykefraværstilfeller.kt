package no.nav.helse.serde.migration

import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioder

/*
* Av uante årsaker hender det at vi finner ulike skjæringstidspunkt på vedtaksperiodene i samme sykefravøærstilfellet.
* Vi må ta hensyn til det tidligste registrerte skjæringstidspunktet når vi skal finne det reelle sykefraværstilfellet
* for å ikke snevre inn søket for mye.
* */
internal object Sykefraværstilfeller {
    internal fun sykefraværstilfeller(vedtaksperioder: List<Vedtaksperiode>): Map<LocalDate, Periode>{
        val sammenhengendePerioder = vedtaksperioder.map { it.periode }.grupperSammenhengendePerioder()
        return sammenhengendePerioder.associateBy { vedtaksperioder.tidlisgteSkjæringstidspunktFor(it) }
    }

    private fun List<Vedtaksperiode>.tidlisgteSkjæringstidspunktFor(periode: Periode) =
        filter { periode.overlapperMed(it.periode) }.minOf { it.skjæringstidspunkt }

    internal class Vedtaksperiode(val skjæringstidspunkt: LocalDate, val periode: Periode)
}