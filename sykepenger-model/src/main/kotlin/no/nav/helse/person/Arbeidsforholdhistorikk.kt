package no.nav.helse.person

import no.nav.helse.hendelser.Arbeidsforhold
import java.util.*

internal class Arbeidsforholdhistorikk private constructor(
    private val historikk: MutableList<Innslag>
)  {

    internal constructor() : this(mutableListOf())

    internal fun lagre(arbeidsforhold: List<Arbeidsforhold>) {
        historikk.add(Innslag(UUID.randomUUID(), arbeidsforhold))
    }

    internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
        visitor.preVisitArbeidsforholdhistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitArbeidsforholdhistorikk(this)
    }

    internal class Innslag(private val id: UUID, private val arbeidsforhold: List<Arbeidsforhold>) {
        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.preVisitArbeidsforholdinnslag(this, id)
            arbeidsforhold.forEach { it.accept(visitor) }
            visitor.postVisitArbeidsforholdinnslag(this, id)
        }
    }
}
