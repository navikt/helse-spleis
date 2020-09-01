package no.nav.helse.person


import no.nav.helse.person.InntektshistorikkVol2.Inntektsopplysning.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkVol2 {

    private val endringer = mutableListOf<InntektshistorikkEndring>()

    private val endring get() =
        if(erIendring) endringer.first()
        else (endringer.firstOrNull()?.clone() ?: InntektshistorikkEndring())
            .also { endringer.add(0, it) }


    private var erIendring = false

    internal fun endring(block: InntektshistorikkVol2.() -> Unit) {
        require(!erIendring)
        endring
        erIendring = true
        block()
        erIendring = false
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikkVol2(this)
        endringer.forEach{ it.accept(visitor) }
        visitor.postVisitInntekthistorikkVol2(this)
    }

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
        GrunnlagForSykepengegrunnlagVisitor(dato)
            .also(this::accept)
            .sykepengegrunnlag()

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
        endringer.first().sammenligningsgrunnlag(dato)

    internal fun add(
        dato: LocalDate,
        meldingsreferanseId: UUID,
        inntekt: Inntekt,
        kilde: Kilde,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        endring.add(Inntektsopplysning(dato, meldingsreferanseId, inntekt, kilde, tidsstempel))
    }

    internal fun add(
        dato: LocalDate,
        meldingsreferanseId: UUID,
        inntekt: Inntekt,
        kilde: Kilde,
        type: Inntekttype,
        fordel: String,
        beskrivelse: String,
        tilleggsinformasjon: String?,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        endring.add(
            Skatt(
                dato,
                meldingsreferanseId,
                inntekt,
                kilde,
                type,
                fordel,
                beskrivelse,
                tilleggsinformasjon,
                tidsstempel
            )
        )
    }

    internal fun add(
        dato: LocalDate,
        meldingsreferanseId: UUID,
        inntekt: Inntekt,
        kilde: Kilde,
        begrunnelse: String,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        endring.add(Saksbehandler(dato, meldingsreferanseId, inntekt, kilde, begrunnelse, tidsstempel))
    }

    internal fun clone() = InntektshistorikkVol2().also {
        it.endringer.addAll(this.endringer.map(InntektshistorikkEndring::clone))
    }

    internal class InntektshistorikkEndring {

        private val inntekter = mutableListOf<Inntektsopplysning>()

        fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitInntekthistorikkEndringVol2(this)
            inntekter.forEach { it.accept(visitor) }
            visitor.postVisitInntekthistorikkEndringVol2(this)
        }

        fun clone() = InntektshistorikkEndring().also {
            it.inntekter.addAll(this.inntekter)
        }

        fun add(inntektsopplysning: Inntektsopplysning) {
            inntekter.removeIf { it.skalErstattesAv(inntektsopplysning) }
            inntekter.add(inntektsopplysning)
        }

        fun sammenligningsgrunnlag(dato: LocalDate) =
            Inntektsopplysning.sammenligningsgrunnlag(dato, inntekter)

    }

    internal open class Inntektsopplysning(
        protected val fom: LocalDate,
        protected val hendelseId: UUID,
        private val beløp: Inntekt,
        protected val kilde: Kilde,
        protected val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        internal fun inntekt() = beløp
        internal fun isBefore(other: Inntektsopplysning) = this.tidsstempel.isBefore(other.tidsstempel)
        internal fun isAfter(other: Inntektsopplysning) = this.tidsstempel.isAfter(other.tidsstempel)

        open fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektVol2(this, hendelseId, kilde, fom, tidsstempel)
        }

        internal fun skalErstattesAv(other: Inntektsopplysning) = this.fom == other.fom && this.kilde == other.kilde

        companion object {
            internal fun sammenligningsgrunnlag(dato: LocalDate, inntekter: List<Inntektsopplysning>): Inntekt {
                return inntekter
                    .filter { it.kilde == Kilde.SKATT_SAMMENLIGNINSGRUNNLAG }
                    .takeLatestBy { it.tidsstempel }
                    .filter {
                        YearMonth.from(it.fom) in YearMonth.from(dato).let { it.minusMonths(12)..it.minusMonths(1) }
                    }
                    .map { it.inntekt() }
                    .summer() / 12
            }

            private fun <T, R : Comparable<R>> List<T>.takeLatestBy(selector: (T) -> R): List<T> {
                val first = maxBy(selector) ?: return this
                return this.filter { selector(first) == selector(it) }
            }
        }

        internal enum class Kilde {
            SKATT_SAMMENLIGNINSGRUNNLAG, SKATT_SYKEPENGEGRUNNLAG, INFOTRYGD, INNTEKTSMELDING, SAKSBEHANDLER
        }

        internal enum class Inntekttype {
            LØNNSINNTEKT,
            NÆRINGSINNTEKT,
            PENSJON_ELLER_TRYGD,
            YTELSE_FRA_OFFENTLIGE
        }

        internal class Skatt(
            fom: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            kilde: Kilde,
            private val type: Inntekttype,
            private val fordel: String,
            private val beskrivelse: String,
            private val tilleggsinformasjon: String?,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Inntektsopplysning(
            fom,
            hendelseId,
            beløp,
            kilde,
            tidsstempel
        ) {
            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitInntektSkattVol2(this, hendelseId, kilde, fom, tidsstempel)
            }
        }

        internal class Saksbehandler(
            fom: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            kilde: Kilde,
            private val begrunnelse: String,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Inntektsopplysning(
            fom,
            hendelseId,
            beløp,
            kilde,
            tidsstempel
        ) {
            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitInntektSaksbehandlerVol2(this, hendelseId, kilde, fom, tidsstempel)
            }
        }
    }
}
