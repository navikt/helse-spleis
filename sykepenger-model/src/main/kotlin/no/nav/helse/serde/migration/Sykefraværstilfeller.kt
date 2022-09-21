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

        return aktiveVedtaksperioder.map { Vedtaksperiode(it.skjæringstidspunkt, it.fom til it.tom, it.tilstand) }
    }

    internal fun sykefraværstilfeller(vedtaksperioder: List<Vedtaksperiode>): Set<Sykefraværstilfelle>{
        val sammenhengendePerioder = vedtaksperioder.map { it.periode }.grupperSammenhengendePerioderMedHensynTilHelg()
        return sammenhengendePerioder.map { sammenhengendePeriode ->
            val skjæringstidspunkter = vedtaksperioder.skjæringstidspunkterFor(sammenhengendePeriode)
            val tidligsteSkjæringstidspunkt = skjæringstidspunkter.min()
            val periode = tidligsteSkjæringstidspunkt til sammenhengendePeriode.endInclusive
            Sykefraværstilfelle(skjæringstidspunkter, periode)
        }.toSet()
    }

    private fun List<Vedtaksperiode>.skjæringstidspunkterFor(periode: Periode) =
        filter { periode.overlapperMed(it.periode) }.map { it.skjæringstidspunkt }.toSet()

    internal data class Vedtaksperiode(val skjæringstidspunkt: LocalDate, val periode: Periode, val tilstand: String)
    internal data class Sykefraværstilfelle(val skjæringstidspunkter: Set<LocalDate>, val periode: Periode) {
        internal val tidligsteSkjæringstidspunkt = skjæringstidspunkter.min()
    }
}