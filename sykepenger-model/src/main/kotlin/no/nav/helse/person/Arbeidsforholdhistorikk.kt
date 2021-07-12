package no.nav.helse.person

import no.nav.helse.hendelser.Arbeidsforhold
import java.time.LocalDate
import java.util.*

internal class Arbeidsforholdhistorikk private constructor(
    private val historikk: MutableList<Innslag>
)  {

    internal constructor() : this(mutableListOf())

    internal fun lagre(arbeidsforhold: List<Arbeidsforhold>) {
        if(historikk.isEmpty() || !historikk.last().erDuplikat(arbeidsforhold)) {
            historikk.add(Innslag(UUID.randomUUID(), arbeidsforhold))
        }
    }

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.preVisitArbeidsforholdhistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsforholdhistorikk(this)
    }

    internal fun harAktivtArbeidsforhold(skjæringstidspunkt: LocalDate) = historikk.last().harAktivtArbeidsforhold(skjæringstidspunkt)

    internal class Innslag(private val id: UUID, private val arbeidsforhold: List<Arbeidsforhold>) {
        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.preVisitArbeidsforholdinnslag(this, id)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsforholdinnslag(this, id)
        }

        internal fun erDuplikat(other: List<Arbeidsforhold>) = arbeidsforhold.size == other.size && arbeidsforhold.containsAll(other)

        internal fun harAktivtArbeidsforhold(skjæringstidspunkt: LocalDate) = arbeidsforhold.any { it.gjelderPeriode(skjæringstidspunkt) }
    }
}
