package no.nav.helse.person

import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

internal class Inntekthistorikk {
    private val inntekter = mutableListOf<Inntekt>()

    internal fun clone(): Inntekthistorikk {
        return Inntekthistorikk().also {
            it.inntekter.addAll(this.inntekter)
        }
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        inntekter.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun add(fom: LocalDate, hendelseId: UUID, beløp: BigDecimal) {
        inntekter.removeAll { it.fom() == fom }
        inntekter.add(Inntekt(fom, hendelseId, beløp))
        inntekter.sort()
    }

    internal fun inntekt(dato: LocalDate) = Inntekt.inntekt(inntekter, dato)

    internal class Inntekt(
        private val fom: LocalDate,
        internal val hendelseId: UUID,
        val beløp: BigDecimal
    ) : Comparable<Inntekt> {
        companion object {
            internal fun inntekt(inntekter: List<Inntekt>, dato: LocalDate) =
                inntekter.lastOrNull { it.fom <= dato }?.beløp
        }

        internal fun fom() = fom

        fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntekt(this)
        }

        override fun compareTo(other: Inntekt) = this.fom.compareTo(other.fom)

        override fun hashCode() = fom.hashCode() * 37 + beløp.hashCode()
    }

}
