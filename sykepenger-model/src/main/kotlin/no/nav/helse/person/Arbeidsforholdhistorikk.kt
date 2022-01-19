package no.nav.helse.person

import no.nav.helse.person.Arbeidsforholdhistorikk.Arbeidsforhold.Companion.harArbeidsforholdSomErNyereEnnTreMåneder
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

    internal fun gjørArbeidsforholdInaktivt(skjæringstidspunkt: LocalDate) {
    }

    internal fun harInaktivtArbeidsforhold(skjæringstidspunkt: LocalDate) = true

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

    /**
     * Noen begreper:
     *
     * Relevant -> Et relevant arbeidsforhold er et arbeidsforhold som er gjeldende for skjæringstidspunkt
     * Aktivt -> Et aktivt arbeidsforhold er et relevant arbeidsforhold som skal taes med i beregning for skjæringstidspunkt
     * Inaktiv -> Et inaktivt arbeidsforhold blir markert av en saksbehandler som at det ikke skal være med i beregning for skjæringstidspunkt
     */
    internal class Arbeidsforhold(
        private val ansattFom: LocalDate,
        private val ansattTom: LocalDate?,
        private val erAktivt: Boolean
    ) {
        internal fun gjelder(skjæringstidspunkt: LocalDate) = ansattFom <= skjæringstidspunkt && (ansattTom == null || ansattTom >= skjæringstidspunkt)

        override fun equals(other: Any?) = other is Arbeidsforhold
            && ansattFom == other.ansattFom
            && ansattTom == other.ansattTom
            && erAktivt == other.erAktivt

        internal fun harArbeidetMindreEnnTreMåneder(skjæringstidspunkt: LocalDate) = ansattFom > skjæringstidspunkt.withDayOfMonth(1).minusMonths(3)

        override fun hashCode(): Int {
            var result = ansattFom.hashCode()
            result = 31 * result + (ansattTom?.hashCode() ?: 0)
            result = 31 * result + erAktivt.hashCode()
            return result
        }

        internal fun accept(visitor: ArbeidsforholdhistorikkVisitor) {
            visitor.visitArbeidsforhold(ansattFom = ansattFom, ansattTom = ansattTom, erAktivt = erAktivt)
        }

        companion object {
            internal fun List<Arbeidsforhold>.harArbeidsforholdSomErNyereEnnTreMåneder(skjæringstidspunkt: LocalDate) =
                any { it.harArbeidetMindreEnnTreMåneder(skjæringstidspunkt) }
        }

    }
}
