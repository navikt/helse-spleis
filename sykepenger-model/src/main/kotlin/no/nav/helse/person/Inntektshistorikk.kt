package no.nav.helse.person

import no.nav.helse.Grunnbeløp
import no.nav.helse.person.Inntektshistorikk.Inntektsendring.Kilde
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class Inntektshistorikk(private val inntekter: MutableList<Inntektsendring> = mutableListOf()) {

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

    internal fun inntekt(skjæringstidspunkt: LocalDate) = Inntektsendring.inntekt(inntekter, skjæringstidspunkt)

    internal fun dekningsgrunnlag(skjæringstidspunkt: LocalDate, regler: ArbeidsgiverRegler): Inntekt =
        inntekt(skjæringstidspunkt)?.times(regler.dekningsgrad()) ?: Inntekt.INGEN

    internal class Inntektsendring(
        private val fom: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val kilde: Kilde,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Comparable<Inntektsendring> {

        companion object {
            private fun inntektendring(inntekter: List<Inntektsendring>, skjæringstidspunkt: LocalDate) =
                (inntekter.lastOrNull { it.fom <= skjæringstidspunkt } ?: inntekter.firstOrNull())

            internal fun inntekt(inntekter: List<Inntektsendring>, skjæringstidspunkt: LocalDate) =
                inntektendring(inntekter, skjæringstidspunkt)?.beløp

            internal fun sykepengegrunnlag(inntekter: List<Inntektsendring>, skjæringstidspunkt: LocalDate, virkningFra: LocalDate = LocalDate.now()): Inntekt? =
                inntekt(inntekter, skjæringstidspunkt)?.let {
                    listOf(it, Grunnbeløp.`6G`.beløp(skjæringstidspunkt, virkningFra)).minOrNull()
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
