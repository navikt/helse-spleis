package no.nav.helse.hendelser

import no.nav.helse.person.ArbeidsforholdhistorikkVisitor
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

class Arbeidsforhold(
    internal val orgnummer: String,
    private val fom: LocalDate,
    private val tom: LocalDate? = null
) {
    fun erSøppel() =
        tom != null && tom < fom

    private fun erGyldig(skjæringstidspunkt: LocalDate) =
        fom < skjæringstidspunkt && !erSøppel()

    private fun periode(skjæringstidspunkt: LocalDate): Periode? {
        if (!erGyldig(skjæringstidspunkt)) return null
        return fom til (tom ?: skjæringstidspunkt)
    }

    internal fun gjelder(skjæringstidspunkt: LocalDate) = fom < skjæringstidspunkt && (tom == null || tom >= skjæringstidspunkt)

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.visitArbeidsforhold(orgnummer, fom, tom)
    }

    override fun equals(other: Any?) = other is Arbeidsforhold
        && orgnummer == other.orgnummer
        && fom == other.fom
        && tom == other.tom

    internal fun erRelevant(arbeidsgiver: Arbeidsgiver) = orgnummer == arbeidsgiver.organisasjonsnummer()

    internal fun gjelderPeriode(skjæringstidspunkt: LocalDate) = fom < skjæringstidspunkt && (tom == null || tom > skjæringstidspunkt)

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

        internal fun List<Arbeidsforhold>.grupperArbeidsforholdPerOrgnummer() = groupBy { it.orgnummer }
    }
}
