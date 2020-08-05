package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Inntekthistorikk.Inntektsendring.Kilde
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Inntekthistorikk {
    private val inntekter = mutableListOf<Inntektsendring>()

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

    internal fun add(fom: LocalDate, hendelseId: UUID, beløp: Inntekt, kilde: Kilde, tidsstempel: LocalDateTime = LocalDateTime.now()) {
        val nyInntekt = Inntektsendring(fom, hendelseId, beløp, kilde, tidsstempel)
        inntekter.removeAll { it.erRedundantMed(nyInntekt) }
        inntekter.add(nyInntekt)
        inntekter.sort()
    }

    internal fun inntekt(dato: LocalDate) = Inntektsendring.inntekt(inntekter, dato)

    internal fun sykepengegrunnlag(dato: LocalDate) = Inntektsendring.sykepengegrunnlag(inntekter, dato)

    internal fun dekningsgrunnlag(dato: LocalDate, regler: ArbeidsgiverRegler): Inntekt =
        inntekt(dato)?.times(regler.dekningsgrad()) ?: Inntekt.INGEN

    internal class Inntektsendring(
        private val fom: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val kilde: Kilde,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Comparable<Inntektsendring> {

        companion object {
            internal fun inntekt(inntekter: List<Inntektsendring>, dato: LocalDate) =
                (inntekter.lastOrNull { it.fom <= dato } ?: inntekter.firstOrNull())?.beløp

            internal fun sykepengegrunnlag(inntekter: List<Inntektsendring>, dato: LocalDate): Double? =
                inntekt(inntekter, dato)?.let {
                    listOf(it, Grunnbeløp.`6G`.beløp(dato)).min()?.tilÅrligDouble()
                }
        }

        fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntekt(this, hendelseId)
        }

        override fun compareTo(other: Inntektsendring) =
            this.fom.compareTo(other.fom).let {
                if (it == 0) this.kilde.compareTo(other.kilde)
                else it
            }

        internal fun erRedundantMed(annenInntektsendring: Inntektsendring) =
            annenInntektsendring.fom == fom && annenInntektsendring.kilde == kilde

        //Order is significant, compare is used to prioritize records from various sources
        internal enum class Kilde : Comparable<Kilde> {
            SKATT, INFOTRYGD, INNTEKTSMELDING
        }
    }

}
