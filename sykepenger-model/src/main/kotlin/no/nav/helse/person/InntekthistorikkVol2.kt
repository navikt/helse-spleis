package no.nav.helse.person


import no.nav.helse.person.InntekthistorikkVol2.Inntektsendring.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntekthistorikkVol2 {

    private val endringer = mutableListOf<InntektshistorikkEndring>()

    private val endring get() =
        if(erIendring) endringer.first()
        else (endringer.firstOrNull()?.clone() ?: InntektshistorikkEndring())
            .also { endringer.add(0, it) }


    private var erIendring = false

    internal fun endring(block: InntekthistorikkVol2.() -> Unit) {
        require(!erIendring)
        endring
        erIendring = true
        block()
        erIendring = false
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikkVol2(this)
        endringer.firstOrNull()?.accept(visitor)
        visitor.postVisitInntekthistorikkVol2(this)
    }

    internal fun sykepengegrunnlag(dato: LocalDate) =
        GrunnlagForSykepengegrunnlagVisitor(dato)
            .also(this::accept)
            .sykepengegrunnlag()

    internal fun sammenligningsgrunnlag(dato: LocalDate) =
        endringer.first().sammenligningsgrunnlag(dato)

    internal fun add(
        dato: LocalDate,
        meldingsreferanseId: UUID,
        inntekt: Inntekt,
        kilde: Kilde,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        endring.add(Inntektsendring(dato, meldingsreferanseId, inntekt, kilde, tidsstempel))
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

    internal fun clone() = InntekthistorikkVol2().also {
        it.endringer.addAll(this.endringer.map(InntektshistorikkEndring::clone))
    }

    private class InntektshistorikkEndring {

        private val inntekter = mutableListOf<Inntektsendring>()

        fun accept(visitor: InntekthistorikkVisitor) {
            inntekter.forEach { it.accept(visitor) }
        }

        fun clone() = InntektshistorikkEndring().also {
            it.inntekter.addAll(this.inntekter)
        }

        fun add(inntektsendring: Inntektsendring) {
            inntekter.removeIf { it.skalErstattesAv(inntektsendring) }
            inntekter.add(inntektsendring)
        }

        fun sammenligningsgrunnlag(dato: LocalDate) =
            Inntektsendring.sammenligningsgrunnlag(dato, inntekter)

    }

    internal open class Inntektsendring(
        protected val fom: LocalDate,
        protected val hendelseId: UUID,
        private val beløp: Inntekt,
        protected val kilde: Kilde,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        internal fun inntekt() = beløp
        internal fun isBefore(other: Inntektsendring) = this.tidsstempel.isBefore(other.tidsstempel)
        internal fun isAfter(other: Inntektsendring) = this.tidsstempel.isAfter(other.tidsstempel)

        open fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektVol2(this, hendelseId, kilde, fom)
        }

        internal fun skalErstattesAv(other: Inntektsendring) = this.fom == other.fom && this.kilde == other.kilde

        companion object {
            internal fun sammenligningsgrunnlag(dato: LocalDate, inntekter: List<Inntektsendring>): Inntekt {
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
        ) : Inntektsendring(
            fom,
            hendelseId,
            beløp,
            kilde,
            tidsstempel
        ) {
            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitInntektSkattVol2(this, hendelseId, kilde, fom)
            }
        }

        internal class Saksbehandler(
            fom: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            kilde: Kilde,
            private val begrunnelse: String,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Inntektsendring(
            fom,
            hendelseId,
            beløp,
            kilde,
            tidsstempel
        ) {
            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitInntektSaksbehandlerVol2(this, hendelseId, kilde, fom)
            }
        }
    }
}
