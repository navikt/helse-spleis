package no.nav.helse.person

import no.nav.helse.hendelser.Arbeidsforhold
import java.time.LocalDate
import java.util.*

internal class Arbeidsforholdhistorikk private constructor(
    private val historikk: MutableList<Innslag>
) {

    internal constructor() : this(mutableListOf())

    internal fun lagre(arbeidsforhold: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) {
        if (historikk.isEmpty() || !historikk.last().erDuplikat(arbeidsforhold, skjæringstidspunkt)) {
            historikk.add(Innslag(UUID.randomUUID(), arbeidsforhold, skjæringstidspunkt))
        }
    }

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.preVisitArbeidsforholdhistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsforholdhistorikk(this)
    }

    internal fun harAktivtArbeidsforhold(skjæringstidspunkt: LocalDate): Boolean {
        if (historikk.isEmpty()) {
            return false
        }
        return historikk.last().harAktivtArbeidsforhold(skjæringstidspunkt)
    }

    internal class Innslag(private val id: UUID, private val arbeidsforhold: List<Arbeidsforhold>, private val skjæringstidspunkt: LocalDate) {
        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.preVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsforholdinnslag(this, id, skjæringstidspunkt)
        }

        internal fun erDuplikat(other: List<Arbeidsforhold>, skjæringstidspunkt: LocalDate) =
            skjæringstidspunkt == this.skjæringstidspunkt && arbeidsforhold.size == other.size && arbeidsforhold.containsAll(other)

        internal fun harAktivtArbeidsforhold(skjæringstidspunkt: LocalDate) =
            this.skjæringstidspunkt == skjæringstidspunkt && arbeidsforhold.any { it.gjelderPeriode(skjæringstidspunkt) }
    }
}
