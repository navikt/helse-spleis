package no.nav.helse.hendelser

import no.nav.helse.person.Arbeidsforholdhistorikk
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.IAktivitetslogg
import java.time.LocalDate

class Arbeidsforhold(
    internal val orgnummer: String,
    private val ansattFom: LocalDate,
    private val ansattTom: LocalDate? = null
) {
    internal fun tilDomeneobjekt() = Arbeidsforholdhistorikk.Arbeidsforhold(
        ansattFom = ansattFom,
        ansattTom = ansattTom,
        erAktivt = true
    )

    fun erSøppel() =
        ansattTom != null && ansattTom < ansattFom

    private fun erGyldig(skjæringstidspunkt: LocalDate) =
        ansattFom < skjæringstidspunkt && !erSøppel()

    private fun periode(skjæringstidspunkt: LocalDate): Periode? {
        if (!erGyldig(skjæringstidspunkt)) return null
        return ansattFom til (ansattTom ?: skjæringstidspunkt)
    }

    internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

    internal fun erRelevant(arbeidsgiver: Arbeidsgiver) = orgnummer == arbeidsgiver.organisasjonsnummer()

    internal fun harArbeidetMindreEnnTreMåneder(skjæringstidspunkt: LocalDate) = ansattFom > skjæringstidspunkt.withDayOfMonth(1).minusMonths(3)

    internal companion object {
        internal fun List<Arbeidsforhold>.toEtterlevelseMap() = map {
            mapOf(
                "orgnummer" to it.orgnummer,
                "fom" to it.ansattFom,
                "tom" to it.ansattTom
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
