package no.nav.helse.person.inntekt

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import no.nav.helse.isWithinRangeOf
import no.nav.helse.person.InntekthistorikkVisitor
import no.nav.helse.økonomi.Inntekt

internal sealed class Skatt(
    dato: LocalDate,
    prioritet: Int,
    protected val hendelseId: UUID,
    protected val beløp: Inntekt,
    protected val måned: YearMonth,
    protected val type: Inntekttype,
    protected val fordel: String,
    protected val beskrivelse: String,
    protected val tidsstempel: LocalDateTime = LocalDateTime.now()
) : Inntektsopplysning(dato, prioritet) {
    internal enum class Inntekttype {
        LØNNSINNTEKT,
        NÆRINGSINNTEKT,
        PENSJON_ELLER_TRYGD,
        YTELSE_FRA_OFFENTLIGE
    }

    internal fun erRelevant(måneder: Long) = måned.isWithinRangeOf(dato, måneder)

    protected fun skalErstattesAv(other: Skatt) =
        this.dato == other.dato && other.måned == this.måned

    protected fun erSammeSkatteinntekt(other: Skatt) =
        this.dato == other.dato && this.beløp == other.beløp && this.måned == other.måned
                && this.type == other.type && this.fordel == other.fordel && this.beskrivelse == other.beskrivelse

    internal class Sykepengegrunnlag(
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Skatt(
        dato,
        40,
        hendelseId,
        beløp,
        måned,
        type,
        fordel,
        beskrivelse,
        tidsstempel
    ) {
        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitSkattSykepengegrunnlag(this, dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
        }

        override fun omregnetÅrsinntekt(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            takeIf { this.dato == skjæringstidspunkt && måned.isWithinRangeOf(skjæringstidspunkt, 3) }

        override fun omregnetÅrsinntekt(): Inntekt = beløp

        override fun rapportertInntekt(): Inntekt = error("Sykepengegrunnlag har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Sykepengegrunnlag && skalErstattesAv(other)

        override fun erSamme(other: Inntektsopplysning): Boolean {
            return other is Sykepengegrunnlag && erSammeSkatteinntekt(other)
        }
    }

    internal class RapportertInntekt(
        dato: LocalDate,
        hendelseId: UUID,
        beløp: Inntekt,
        måned: YearMonth,
        type: Inntekttype,
        fordel: String,
        beskrivelse: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) :
        Skatt(dato, 20, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel) {

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitSkattRapportertInntekt(
                this,
                dato,
                hendelseId,
                beløp,
                måned,
                type,
                fordel,
                beskrivelse,
                tidsstempel
            )
        }

        override fun rapportertInntekt(dato: LocalDate) =
            takeIf { this.dato == dato && måned.isWithinRangeOf(dato, 12) }?.let { listOf(this) }

        override fun rapportertInntekt(): Inntekt = beløp

        override fun omregnetÅrsinntekt(): Inntekt = error("Sammenligningsgrunnlag har ikke grunnlag for sykepengegrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is RapportertInntekt && skalErstattesAv(other)

        override fun erSamme(other: Inntektsopplysning) =
            other is RapportertInntekt && erSammeSkatteinntekt(other)
    }
}