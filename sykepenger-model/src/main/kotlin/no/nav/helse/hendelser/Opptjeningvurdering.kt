package no.nav.helse.hendelser

import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

class Opptjeningvurdering(
    private val arbeidsforhold: List<Arbeidsforhold>
) {
    private companion object {
        private const val TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER = 28
    }

    internal var antallOpptjeningsdager: Int = 0
        private set

    internal fun harOpptjening() =
        antallOpptjeningsdager >= TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER

    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): IAktivitetslogg {
        antallOpptjeningsdager = Arbeidsforhold.opptjeningsdager(arbeidsforhold, aktivitetslogg, skjæringstidspunkt)
        if (harOpptjening()) aktivitetslogg.info(
            "Har minst %d dager opptjening",
            TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER
        )
        else aktivitetslogg.error("Har mindre enn %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
        return aktivitetslogg
    }

    class Arbeidsforhold(
        private val orgnummer: String,
        private val fom: LocalDate,
        private val tom: LocalDate? = null
    ) {
        internal companion object {
            private fun LocalDate.datesUntilReversed(tom: LocalDate) = tom.toEpochDay().downTo(this.toEpochDay())
                .asSequence()
                .map(LocalDate::ofEpochDay)

            fun opptjeningsdager(
                arbeidsforhold: List<Arbeidsforhold>,
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate
            ) : Int {
                val ranges = arbeidsforhold
                    .filter { it.fom < skjæringstidspunkt }
                    .map { it.fom.til(it.tom ?: skjæringstidspunkt) }

                if (ranges.none { skjæringstidspunkt in it }) {
                    aktivitetslogg.error("Personen er ikke i arbeid ved skjæringstidspunktet")
                    return 0
                }

                val min = ranges.minOf { it.start }
                return min.datesUntilReversed(skjæringstidspunkt.minusDays(1))
                    .takeWhile { cursor -> ranges.any { periode -> cursor in periode } }
                    .count()
            }
        }
    }
}
