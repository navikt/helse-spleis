package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til

/*
* Av uante årsaker hender det at vi finner ulike skjæringstidspunkt på vedtaksperiodene i samme sykefravøærstilfellet.
* Vi må ta hensyn til det tidligste registrerte skjæringstidspunktet når vi skal finne det reelle sykefraværstilfellet
* for å ikke snevre inn søket for mye.
* */
internal object Sykefraværstilfeller {

    private val JsonNode.dato get() = LocalDate.parse(asText())
    private val JsonNode.skjæringstidspunkt get() = path("skjæringstidspunkt").dato
    private val JsonNode.fom get() = path("fom").dato
    private val JsonNode.tom get() = path("tom").dato
    private val JsonNode.tilstand get() = path("tilstand").asText()

    internal fun vedtaksperioder(person: JsonNode): List<Vedtaksperiode> {
        val aktiveVedtaksperioder = person.path("arbeidsgivere")
            .flatMap { it.path("vedtaksperioder") }
            .filterNot { it.tilstand == "AVSLUTTET_UTEN_UTBETALING" }
            .map { AktivVedtaksperiode(it.skjæringstidspunkt, it.fom til it.tom, it.tilstand) }

        val forkastedeVedtaksperioder = person.path("arbeidsgivere")
            .flatMap { it.path("forkastede") }
            .map { it.path("vedtaksperiode") }
            .map { ForkastetVedtaksperiode(it.skjæringstidspunkt, it.fom til it.tom, it.tilstand) }

        return aktiveVedtaksperioder + forkastedeVedtaksperioder
    }

    internal fun sykefraværstilfeller(vedtaksperioder: List<Vedtaksperiode>): Set<Sykefraværstilfelle>{
        val sammenhengendePerioder = vedtaksperioder
            .map { it.periode }
            .grupperSammenhengendePerioderMedHensynTilHelg()

        val aktivePerioder = vedtaksperioder.filterIsInstance<AktivVedtaksperiode>()
        val aktiveSkjæringstidspunkter = aktivePerioder.map { it.skjæringstidspunkt }.toSet()

        val dagerISpleis = aktivePerioder
            .map { it.periode }
            .grupperSammenhengendePerioderMedHensynTilHelg()
            .flatten()
            .sorted()

        return sammenhengendePerioder
            .mapNotNull { sammenhengendePeriode ->
                when (val sisteDagISpleis = dagerISpleis.lastOrNull { it in sammenhengendePeriode }) {
                    null -> null
                    else -> {
                        val skjæringstidspunkter = vedtaksperioder.skjæringstidspunkterFor(sammenhengendePeriode)
                        val tidligsteSkjæringstidspunkt = skjæringstidspunkter.min()
                        Sykefraværstilfelle(
                            tidligsteSkjæringstidspunkt = tidligsteSkjæringstidspunkt,
                            aktiveSkjæringstidspunkter = skjæringstidspunkter.filter { it in aktiveSkjæringstidspunkter }.toSet(),
                            sisteDag = sammenhengendePeriode.endInclusive,
                            sisteDagISpleis = sisteDagISpleis
                        )
                    }
                }
            }.toSet()
    }

    private fun List<Vedtaksperiode>.skjæringstidspunkterFor(periode: Periode) =
        filter { periode.overlapperMed(it.periode) }.map { it.skjæringstidspunkt }.toSet()

    internal sealed class Vedtaksperiode(val skjæringstidspunkt: LocalDate, val periode: Periode, protected val tilstand: String) {
        open fun tilstand() = tilstand
        override fun toString() = "$periode med skjæringstidspunkt $skjæringstidspunkt"
    }
    internal class AktivVedtaksperiode(skjæringstidspunkt: LocalDate, periode: Periode, tilstand: String) : Vedtaksperiode(skjæringstidspunkt, periode, tilstand)
    internal class ForkastetVedtaksperiode(skjæringstidspunkt: LocalDate, periode: Periode, tilstand: String) : Vedtaksperiode(skjæringstidspunkt, periode, tilstand) {
        override fun tilstand() = "$tilstand (Forkastet)"
    }

    internal data class Sykefraværstilfelle(
        val tidligsteSkjæringstidspunkt: LocalDate,
        val aktiveSkjæringstidspunkter: Set<LocalDate>,
        private val sisteDag: LocalDate,
        private val sisteDagISpleis: LocalDate) {
        internal val periode = tidligsteSkjæringstidspunkt til sisteDag
        internal val periodeFremTilSisteDagISpleis = tidligsteSkjæringstidspunkt til sisteDagISpleis
    }
}