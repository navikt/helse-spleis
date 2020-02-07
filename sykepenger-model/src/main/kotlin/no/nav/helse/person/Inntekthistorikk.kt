package no.nav.helse.person

import no.nav.helse.hendelser.ModelInntektsmelding
import no.nav.helse.hendelser.ModelYtelser
import java.math.BigDecimal
import java.time.LocalDate

internal class Inntekthistorikk {
    internal class Inntekt(
        private val fom: LocalDate,
        internal val hendelse: ArbeidstakerHendelse,
        val beløp: BigDecimal
    ) : Comparable<Inntekt> {
        companion object {
            internal fun inntekt(inntekter: List<Inntekt>, dato: LocalDate) =
                inntekter.lastOrNull { it.fom <= dato }?.beløp
        }

        fun accept(visitor: ArbeidsgiverVisitor) {
            visitor.visitInntekt(this)
        }

        override fun compareTo(other: Inntekt) = this.fom.compareTo(other.fom)

        override fun hashCode() = fom.hashCode() * 37 + beløp.hashCode()
    }

    private val inntekter = mutableListOf<Inntekt>()

    internal fun clone(): Inntekthistorikk {
        return Inntekthistorikk().also {
            it.inntekter.addAll(this.inntekter)
        }
    }

    internal fun accept(visitor: ArbeidsgiverVisitor) {
        visitor.preVisitInntekthistorikk(this)

        visitor.preVisitInntekter()
        inntekter.forEach { it.accept(visitor) }
        visitor.postVisitInntekter()

        visitor.postVisitInntekthistorikk(this)
    }

    internal fun add(fom: LocalDate, hendelse: ArbeidstakerHendelse, beløp: BigDecimal) {
        inntekter.add(Inntekt(fom, hendelse, beløp))
        inntekter.sort()
    }

    internal fun inntekt(dato: LocalDate) = Inntekt.inntekt(inntekter, dato)
}
