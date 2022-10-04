package no.nav.helse.person

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.hendelser.Periode.Companion.sammenhengende
import no.nav.helse.hendelser.til
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.ansattVedSkjæringstidspunkt
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.create
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.erDeaktivert
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.harArbeidsforholdNyereEnn
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.harDeaktivertArbeidsforholdSomErNyereEnn
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.harIkkeDeaktivertArbeidsforholdSomErNyereEnn
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.ikkeDeaktiverteArbeidsforhold
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.opptjeningsperiode

internal class Arbeidsforholdhistorikk private constructor(
    private val historikk: MutableList<Innslag>
) {

    internal constructor() : this(mutableListOf())

    internal fun lagre(arbeidsforhold: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        val erDuplikat = sisteInnslag(skjæringstidspunkt)?.erDuplikat(arbeidsforhold, skjæringstidspunkt) ?: false
        if (!erDuplikat) {
            historikk.add(Innslag(UUID.randomUUID(), arbeidsforhold, skjæringstidspunkt))
        }
    }

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.preVisitArbeidsforholdhistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsforholdhistorikk(this)
    }

    internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = sisteInnslag(skjæringstidspunkt)?.arbeidsforhold
        ?.ikkeDeaktiverteArbeidsforhold()
        ?.ansattVedSkjæringstidspunkt(skjæringstidspunkt)
        ?: false

    internal fun harIkkeDeaktivertArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
        sisteInnslag(skjæringstidspunkt)?.harIkkeDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt, antallMåneder) ?: false

    internal fun harDeaktivertArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
        sisteInnslag(skjæringstidspunkt)?.harDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt, antallMåneder) ?: false

    internal fun harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
        sisteInnslag(skjæringstidspunkt)?.harArbeidsforholdNyereEnn(skjæringstidspunkt, antallMåneder) ?: false

    internal fun aktiverArbeidsforhold(skjæringstidspunkt: LocalDate) {
        val nåværendeInnslag = requireNotNull(sisteInnslag(skjæringstidspunkt))
        lagre(nåværendeInnslag.aktiverArbeidsforhold(), skjæringstidspunkt)
    }

    internal fun deaktiverArbeidsforhold(skjæringstidspunkt: LocalDate) {
        val nåværendeInnslag = requireNotNull(sisteInnslag(skjæringstidspunkt))
        lagre(nåværendeInnslag.deaktiverArbeidsforhold(), skjæringstidspunkt)
    }

    internal fun harDeaktivertArbeidsforhold(skjæringstidspunkt: LocalDate): Boolean {
        val innslag = sisteInnslag(skjæringstidspunkt) ?: return false
        return innslag.erDeaktivert()
    }

    internal fun arbeidsforhold(skjæringstidspunkt: LocalDate) = sisteInnslag(skjæringstidspunkt)?.arbeidsforhold ?: emptyList()

    private fun sisteInnslag(skjæringstidspunkt: LocalDate) = historikk.lastOrNull { it.gjelder(skjæringstidspunkt) }
    internal fun <T> sisteArbeidsforhold(skjæringstidspunkt: LocalDate, creator: (LocalDate, LocalDate?, Boolean) -> T) =
        sisteInnslag(skjæringstidspunkt)?.arbeidsforhold?.create(creator) ?: emptyList()

    internal fun startdatoFor(skjæringstidspunkt: LocalDate): LocalDate? =
        sisteInnslag(skjæringstidspunkt)?.arbeidsforhold?.opptjeningsperiode(skjæringstidspunkt)?.start


    internal class Innslag(private val id: UUID, val arbeidsforhold: List<Arbeidsforhold>, private val skjæringstidspunkt: LocalDate) {
        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.preVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
        }

        internal fun erDuplikat(other: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) =
            skjæringstidspunkt == this.skjæringstidspunkt && arbeidsforhold.size == other.size && arbeidsforhold.containsAll(other)

        internal fun gjelder(skjæringstidspunkt: LocalDate) =
            this.skjæringstidspunkt == skjæringstidspunkt

        internal fun harIkkeDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
            arbeidsforhold.harIkkeDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt, antallMåneder)

        internal fun harDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
            arbeidsforhold.harDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt, antallMåneder)

        internal fun harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
            arbeidsforhold.harArbeidsforholdNyereEnn(skjæringstidspunkt, antallMåneder)

        internal fun deaktiverArbeidsforhold() = arbeidsforhold.map { it.deaktiver() }

        internal fun aktiverArbeidsforhold() = arbeidsforhold.map { it.aktiver() }

        internal fun erDeaktivert() = arbeidsforhold.erDeaktivert()

    }

    internal class Arbeidsforhold(
        private val ansattFom: LocalDate,
        private val ansattTom: LocalDate?,
        private val deaktivert: Boolean
    ) {
        internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

        override fun equals(other: Any?) = other is Arbeidsforhold
            && ansattFom == other.ansattFom
            && ansattTom == other.ansattTom
            && deaktivert == other.deaktivert

        internal fun harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) = ansattFom >= skjæringstidspunkt.withDayOfMonth(1).minusMonths(antallMåneder)

        override fun hashCode(): Int {
            var result = ansattFom.hashCode()
            result = 31 * result + (ansattTom?.hashCode() ?: 0)
            result = 31 * result + deaktivert.hashCode()
            return result
        }

        internal fun accept(visitor: ArbeidsforholdVisitor) {
            visitor.visitArbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = deaktivert)
        }

        internal fun deaktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = true)

        internal fun aktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = false)

        companion object {
            const val MAKS_INNTEKT_GAP = 2L

            private fun List<Arbeidsforhold>.harArbeidetMindreEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) = this
                .filter { it.harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder) }
                .filter { it.gjelder(skjæringstidspunkt) }

            internal fun List<Arbeidsforhold>.harArbeidsforholdNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
                harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder).isNotEmpty()

            internal fun List<Arbeidsforhold>.harIkkeDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
                harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder).any { !it.deaktivert }

            internal fun List<Arbeidsforhold>.harDeaktivertArbeidsforholdSomErNyereEnn(skjæringstidspunkt: LocalDate, antallMåneder: Long) =
                harArbeidetMindreEnn(skjæringstidspunkt, antallMåneder).any { it.deaktivert }

            internal fun Collection<Arbeidsforhold>.erDeaktivert() = any { it.deaktivert }
            internal fun Collection<Arbeidsforhold>.opptjeningsperiode(skjæringstidspunkt: LocalDate) = filter { !it.deaktivert }
                .map { it.ansattFom til (it.ansattTom ?: skjæringstidspunkt) }
                .sammenhengende(skjæringstidspunkt)


            internal fun <T> List<Arbeidsforhold>.create(creator: (LocalDate, LocalDate?, Boolean) -> T) =
                map { creator(it.ansattFom, it.ansattTom, it.deaktivert) }

            internal fun Collection<Arbeidsforhold>.ikkeDeaktiverteArbeidsforhold() = filter { !it.deaktivert }

            internal fun Collection<Arbeidsforhold>.ansattVedSkjæringstidspunkt(skjæringstidspunkt: LocalDate) = any { it.gjelder(skjæringstidspunkt) }

            internal fun Iterable<Arbeidsforhold>.toEtterlevelseMap(orgnummer: String) = map {
                mapOf(
                    "orgnummer" to orgnummer,
                    "fom" to it.ansattFom,
                    "tom" to it.ansattTom
                )
            }
        }

    }

    internal companion object {
        internal fun ferdigArbeidsforholdhistorikk(historikk: List<Innslag>): Arbeidsforholdhistorikk =
            Arbeidsforholdhistorikk(historikk.map{it}.toMutableList())
    }
}
