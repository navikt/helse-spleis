package no.nav.helse.person

import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
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

    internal fun dagsats(dato: LocalDate, regler: ArbeidsgiverRegler) =
        inntekt(dato)
        ?.multiply(regler.dekningsgrad().toBigDecimal())
        ?.multiply(12.toBigDecimal())
        ?.divide(260.toBigDecimal(), MathContext.DECIMAL128)
        ?.setScale(0, RoundingMode.HALF_UP)
        ?.toInt()
        ?: 0

    internal class Inntekt(
        private val fom: LocalDate,
        private val hendelseId: UUID,
        private val beløp: BigDecimal
    ) : Comparable<Inntekt> {
        companion object {
            internal fun inntekt(inntekter: List<Inntekt>, dato: LocalDate) =
                (inntekter.lastOrNull { it.fom <= dato } ?: inntekter.firstOrNull())?.beløp
        }

        internal fun fom() = fom

        fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntekt(this, hendelseId)
        }

        override fun compareTo(other: Inntekt) = this.fom.compareTo(other.fom)

        override fun hashCode() = fom.hashCode() * 37 + beløp.hashCode()
        override fun equals(other: Any?) =
            other is Inntekt
                && other.fom == this.fom
                && other.beløp == this.beløp
    }

}
