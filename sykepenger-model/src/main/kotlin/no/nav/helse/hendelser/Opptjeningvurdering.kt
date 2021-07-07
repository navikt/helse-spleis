package no.nav.helse.hendelser

import no.nav.helse.hendelser.Opptjeningvurdering.Arbeidsforhold.Companion.toEtterlevelseMap
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

    internal fun valider(aktivitetslogg: IAktivitetslogg, skjæringstidspunkt: LocalDate): Boolean {
        antallOpptjeningsdager = Arbeidsforhold.opptjeningsdager(arbeidsforhold, aktivitetslogg, skjæringstidspunkt)
        val harOpptjening = harOpptjening()
        aktivitetslogg.etterlevelse.`§8-2 ledd 1`(harOpptjening, skjæringstidspunkt, TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER, arbeidsforhold.toEtterlevelseMap())
        if (harOpptjening) aktivitetslogg.info("Har minst %d dager opptjening", TILSTREKKELIG_ANTALL_OPPTJENINGSDAGER)
        else aktivitetslogg.warn("Perioden er avslått på grunn av manglende opptjening")
        return harOpptjening
    }

    class Arbeidsforhold(
        private val orgnummer: String,
        private val fom: LocalDate,
        private val tom: LocalDate? = null
    ) {
        private fun erSøppel() =
            tom != null && tom < fom

        private fun erGyldig(skjæringstidspunkt: LocalDate) =
            fom < skjæringstidspunkt && !erSøppel()

        private fun periode(skjæringstidspunkt: LocalDate): Periode? {
            if (!erGyldig(skjæringstidspunkt)) return null
            return fom til (tom ?: skjæringstidspunkt)
        }

        internal companion object {
            internal fun List<Arbeidsforhold>.toEtterlevelseMap() = map {
                mapOf(
                    "orgnummer" to it.orgnummer,
                    "fom" to it.fom,
                    "tom" to it.tom
                )
            }

            private fun LocalDate.datesUntilReversed(tom: LocalDate) = tom.toEpochDay().downTo(this.toEpochDay())
                .asSequence()
                .map(LocalDate::ofEpochDay)

            fun opptjeningsdager(
                arbeidsforhold: List<Arbeidsforhold>,
                aktivitetslogg: IAktivitetslogg,
                skjæringstidspunkt: LocalDate
            ): Int {
                if (arbeidsforhold.any(Arbeidsforhold::erSøppel))
                    aktivitetslogg.warn("Opptjeningsvurdering må gjøres manuelt fordi opplysningene fra AA-registeret er ufullstendige")

                val ranges = arbeidsforhold.mapNotNull { it.periode(skjæringstidspunkt) }
                if (ranges.none { skjæringstidspunkt in it }) {
                    aktivitetslogg.info("Personen er ikke i arbeid ved skjæringstidspunktet")
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
