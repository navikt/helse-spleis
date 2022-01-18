package no.nav.helse.person

import no.nav.helse.hendelser.Arbeidsforhold
import no.nav.helse.hendelser.Arbeidsforhold.Companion.harArbeidsforholdSomErNyereEnnTreMåneder
import java.time.LocalDate
import java.util.*

internal class Arbeidsforholdhistorikk private constructor(
    private val historikk: MutableList<Innslag>
) {

    internal constructor() : this(mutableListOf())

    internal fun lagre(arbeidsforhold: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        val erDuplikat = sisteRelevanteInnslag(skjæringstidspunkt)?.erDuplikat(arbeidsforhold, skjæringstidspunkt) ?: false
        if (!erDuplikat) {
            historikk.add(Innslag(UUID.randomUUID(), arbeidsforhold, skjæringstidspunkt))
        }
    }

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.preVisitArbeidsforholdhistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsforholdhistorikk(this)
    }

    internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) = sisteRelevanteInnslag(skjæringstidspunkt) != null

    internal fun harArbeidsforholdNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) =
        sisteRelevanteInnslag(skjæringstidspunkt)?.harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt) ?: false

    fun gjørArbeidsforholdInaktivt(skjæringstidspunkt: LocalDate) {
    }

    fun harInaktivtArbeidsforhold(skjæringstidspunkt: LocalDate) = true

    private fun sisteRelevanteInnslag(skjæringstidspunkt: LocalDate) = historikk.lastOrNull { it.harRelevantArbeidsforhold(skjæringstidspunkt) }

    internal class Innslag(private val id: UUID, private val arbeidsforhold: List<Arbeidsforhold>, private val skjæringstidspunkt: LocalDate) {
        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.preVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
        }

        internal fun erDuplikat(other: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) =
            skjæringstidspunkt == this.skjæringstidspunkt && arbeidsforhold.size == other.size && arbeidsforhold.containsAll(other)

        internal fun harRelevantArbeidsforhold(skjæringstidspunkt: LocalDate) =
            this.skjæringstidspunkt == skjæringstidspunkt

        internal fun harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) =
            arbeidsforhold.harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt)
    }
}
