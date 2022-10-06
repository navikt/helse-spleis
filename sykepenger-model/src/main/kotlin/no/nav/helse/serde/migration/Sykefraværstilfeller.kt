package no.nav.helse.serde.migration

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.Periode.Companion.grupperSammenhengendePerioderMedHensynTilHelg
import no.nav.helse.hendelser.til
import no.nav.helse.serde.migration.Sykefraværstilfeller.Vedtaksperiode.Companion.aktiveSkjæringstidspunkter
import no.nav.helse.serde.migration.Sykefraværstilfeller.Vedtaksperiode.Companion.kunForkastetEllerAvsluttetUtenUtbetaling
import no.nav.helse.serde.migration.Sykefraværstilfeller.Vedtaksperiode.Companion.overlappendeVedtaksperioder
import no.nav.helse.serde.migration.Sykefraværstilfeller.Vedtaksperiode.Companion.skjæringstidspunkter

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
            .map { AktivVedtaksperiode(it.skjæringstidspunkt, it.fom til it.tom, it.tilstand) }

        val forkastedeVedtaksperioder = person.path("arbeidsgivere")
            .flatMap { it.path("forkastede") }
            .map { it.path("vedtaksperiode") }
            .map { ForkastetVedtaksperiode(it.skjæringstidspunkt, it.fom til it.tom, it.tilstand) }

        return aktiveVedtaksperioder + forkastedeVedtaksperioder
    }

    internal fun sykefraværstilfeller(vedtaksperioder: List<Vedtaksperiode>): Set<Sykefraværstilfelle> {
        val sammenhengendePerioder = vedtaksperioder
            .map { it.periode }
            .grupperSammenhengendePerioderMedHensynTilHelg()

        val aktivePerioderForPerson = vedtaksperioder.filterIsInstance<AktivVedtaksperiode>()
        val aktiveSkjæringstidspunkterForPerson = aktivePerioderForPerson.aktiveSkjæringstidspunkter()

        val dagerISpleisForPerson = aktivePerioderForPerson
            .map { it.periode }
            .grupperSammenhengendePerioderMedHensynTilHelg()
            .flatten()
            .sorted()

        return sammenhengendePerioder
            .mapNotNull { sammenhengendePeriode ->
                val overlappendeVedtaksperioder = vedtaksperioder.overlappendeVedtaksperioder(sammenhengendePeriode)
                val skjæringstidspunkter = overlappendeVedtaksperioder.skjæringstidspunkter()
                val aktiveSkjæringstidspunkter = skjæringstidspunkter.filter { it in aktiveSkjæringstidspunkterForPerson }.toSet()
                val sisteDagISpleis = dagerISpleisForPerson.lastOrNull { it in sammenhengendePeriode }

                if (overlappendeVedtaksperioder.kunForkastetEllerAvsluttetUtenUtbetaling()) null // Om AUU har samme skjæringstidspunkt som en annen aktiv vedtaksperiode
                else if (aktiveSkjæringstidspunkter.isEmpty() || sisteDagISpleis == null) null // Uansett ikke aktuell å lete frem vilkårsgrunnlag for
                else {
                    Sykefraværstilfelle(
                        tidligsteSkjæringstidspunkt = skjæringstidspunkter.min(),
                        aktiveSkjæringstidspunkter = aktiveSkjæringstidspunkter,
                        førsteDag = sammenhengendePeriode.start,
                        sisteDag = sammenhengendePeriode.endInclusive,
                        sisteDagISpleis = sisteDagISpleis
                    )
                }
            }.toSet()
    }

    internal sealed class Vedtaksperiode(val skjæringstidspunkt: LocalDate, val periode: Periode, protected val tilstand: String) {
        open fun tilstand() = tilstand
        override fun toString() = "$periode med skjæringstidspunkt $skjæringstidspunkt"
        protected fun erAvsluttetUtenUtbetaling() = tilstand() == "AVSLUTTET_UTEN_UTBETALING"

        internal companion object {
            internal fun List<AktivVedtaksperiode>.aktiveSkjæringstidspunkter() =
                fjernAvsluttetUtenUtbetaling().skjæringstidspunkter()
            internal fun List<Vedtaksperiode>.overlappendeVedtaksperioder(periode: Periode) =
                filter { periode.overlapperMed(it.periode) }
            internal fun List<Vedtaksperiode>.kunForkastetEllerAvsluttetUtenUtbetaling() =
                all { it is ForkastetVedtaksperiode || it.erAvsluttetUtenUtbetaling() }
            internal fun List<Vedtaksperiode>.skjæringstidspunkter() =
                map { it.skjæringstidspunkt }.toSet()
            internal fun List<Vedtaksperiode>.fjernAvsluttetUtenUtbetaling() =
                filterNot { it.erAvsluttetUtenUtbetaling() }

        }
    }
    internal class AktivVedtaksperiode(skjæringstidspunkt: LocalDate, periode: Periode, tilstand: String) : Vedtaksperiode(skjæringstidspunkt, periode, tilstand)
    internal class ForkastetVedtaksperiode(skjæringstidspunkt: LocalDate, periode: Periode, tilstand: String) : Vedtaksperiode(skjæringstidspunkt, periode, tilstand) {
        override fun tilstand() = "$tilstand (Forkastet)"
    }

    internal data class Sykefraværstilfelle(
        val tidligsteSkjæringstidspunkt: LocalDate,
        val aktiveSkjæringstidspunkter: Set<LocalDate>,
        private val førsteDag: LocalDate,
        private val sisteDag: LocalDate,
        private val sisteDagISpleis: LocalDate) {
        private val fom = minOf(tidligsteSkjæringstidspunkt, førsteDag)
        internal val periode = fom til sisteDag
        internal val periodeFremTilSisteDagISpleis = fom til sisteDagISpleis
    }
}