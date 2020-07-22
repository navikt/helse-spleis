package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Inntekthistorikk.Inntekt.Kilde
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import java.time.LocalDate
import java.util.*
import kotlin.math.min

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

    internal fun add(fom: LocalDate, hendelseId: UUID, beløp: Number, kilde: Kilde) {
        val nyInntekt = Inntekt(fom, hendelseId, beløp, kilde)
        inntekter.removeAll { it.erRedundantMed(nyInntekt) }
        inntekter.add(nyInntekt)
        inntekter.sort()
    }

    internal fun inntekt(dato: LocalDate) = Inntekt.inntekt(inntekter, dato)

    internal fun sykepengegrunnlag(dato: LocalDate) = Inntekt.sykepengegrunnlag(inntekter, dato)

    internal fun dekningsgrunnlag(dato: LocalDate, regler: ArbeidsgiverRegler) =
        aktuellDagsinntekt(dato) * regler.dekningsgrad()

    internal fun aktuellDagsinntekt(dato: LocalDate) =
        inntekt(dato)
            ?.toDouble()
            ?.times(12.0)
            ?.div(260.0)
            ?: 0.0

    internal class Inntekt(
        private val fom: LocalDate,
        private val hendelseId: UUID,
        beløp: Number,
        private val kilde: Kilde
    ) : Comparable<Inntekt> {
        private val beløp = beløp.toDouble()

        companion object {
            internal fun inntekt(inntekter: List<Inntekt>, dato: LocalDate) =
                (inntekter.lastOrNull { it.fom <= dato } ?: inntekter.firstOrNull())?.beløp

            internal fun sykepengegrunnlag(inntekter: List<Inntekt>, dato: LocalDate): Double? =
                inntekt(inntekter, dato)?.toDouble()?.let {
                    min(it * 12, Grunnbeløp.`6G`.beløp(dato))
                }
        }

        fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntekt(this, hendelseId)
        }

        override fun compareTo(other: Inntekt) =
            this.fom.compareTo(other.fom).let {
                if (it == 0) this.kilde.compareTo(other.kilde)
                else it
            }

        internal fun erRedundantMed(annenInntekt: Inntekt) =
            annenInntekt.fom == fom && annenInntekt.kilde == kilde

        //Order is significant, compare is used to prioritize records from various sources
        internal enum class Kilde : Comparable<Kilde> {
            SKATT, INFOTRYGD, INNTEKTSMELDING
        }
    }

}
