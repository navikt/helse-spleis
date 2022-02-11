package no.nav.helse.person

import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.create
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.erDeaktivert
import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.harArbeidsforholdSomErNyereEnnTreMåneder
import java.time.LocalDate
import java.util.*

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

    internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = sisteInnslag(skjæringstidspunkt)?.erDeaktivert()?.not() ?: false

    internal fun harArbeidsforholdNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) =
        sisteInnslag(skjæringstidspunkt)?.harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt) ?: false

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

    private fun sisteInnslag(skjæringstidspunkt: LocalDate) = historikk.lastOrNull { it.gjelder(skjæringstidspunkt) }
    internal fun <T> sisteArbeidsforhold(skjæringstidspunkt: LocalDate, creator: (LocalDate, LocalDate?, Boolean) -> T) =
        sisteInnslag(skjæringstidspunkt)?.arbeidsforhold?.create(creator) ?: emptyList()

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

        internal fun harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) =
            arbeidsforhold.harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt)

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

        internal fun harArbeidetMindreEnnTreMåneder(skjæringstidspunkt: LocalDate) = ansattFom > skjæringstidspunkt.withDayOfMonth(1).minusMonths(3)

        override fun hashCode(): Int {
            var result = ansattFom.hashCode()
            result = 31 * result + (ansattTom?.hashCode() ?: 0)
            result = 31 * result + deaktivert.hashCode()
            return result
        }

        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.visitArbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = deaktivert)
        }

        internal fun deaktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = true)

        internal fun aktiver() = Arbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, deaktivert = false)

        companion object {
            internal fun List<Arbeidsforhold>.harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) =
                any { it.harArbeidetMindreEnnTreMåneder(skjæringstidspunkt) && !it.deaktivert }

            internal fun Collection<Arbeidsforhold>.erDeaktivert() = any { it.deaktivert }

            internal fun <T> List<Arbeidsforhold>.create(creator: (LocalDate, LocalDate?, Boolean) -> T) =
                map { creator(it.ansattFom, it.ansattTom, it.deaktivert) }
        }

    }
}
