package no.nav.helse.person

import no.nav.helse.hendelser.ModelInntektsmelding
import java.math.BigDecimal
import java.time.LocalDate

internal class Inntekthistorikk {
    internal class Inntekt(
        private val fom: LocalDate,
        internal val hendelse: ModelInntektsmelding,
        val beløp: BigDecimal
    ) {
        companion object {
            internal fun inntekt(inntekter: List<Inntekt>, dato: LocalDate) =
                inntekter.lastOrNull { it.fom <= dato }?.beløp
        }

        fun accept(visitor: ArbeidsgiverVisitor) {
            visitor.visitInntekt(this)
        }
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

    internal fun add(fom: LocalDate, hendelse: ModelInntektsmelding, beløp: BigDecimal) {
        inntekter.add(Inntekt(fom, hendelse, beløp))
    }

    internal fun inntekt(dato: LocalDate) = Inntekt.inntekt(inntekter, dato)
}
