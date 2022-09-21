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

    internal fun sykefraværstilfeller(person: JsonNode): Set<Periode> {
        val aktiveVedtaksperioder = person.path("arbeidsgivere")
            .flatMap { it.path("vedtaksperioder") }
            .filterNot { it.tilstand == "AVSLUTTET_UTEN_UTBETALING" }

        return sykefraværstilfeller(aktiveVedtaksperioder.map { Vedtaksperiode(it.skjæringstidspunkt, it.fom til it.tom) })
    }

    internal fun sykefraværstilfeller(vedtaksperioder: List<Vedtaksperiode>): Set<Periode>{
        val sammenhengendePerioder = vedtaksperioder.map { it.periode }.grupperSammenhengendePerioderMedHensynTilHelg()
        return sammenhengendePerioder.map { sammenhengendePeriode ->
            val tidligsteSkjæringstidspunkt = vedtaksperioder.tidligsteSkjæringstidspunktFor(sammenhengendePeriode)
            tidligsteSkjæringstidspunkt til sammenhengendePeriode.endInclusive
        }.toSet()
    }

    private fun List<Vedtaksperiode>.tidligsteSkjæringstidspunktFor(periode: Periode) =
        filter { periode.overlapperMed(it.periode) }.minOf { it.skjæringstidspunkt }

    internal class Vedtaksperiode(val skjæringstidspunkt: LocalDate, val periode: Periode)
}