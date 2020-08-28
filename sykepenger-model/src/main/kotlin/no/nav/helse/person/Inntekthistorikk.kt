package no.nav.helse.person

import no.nav.helse.person.Inntekthistorikk.Inntektsendring.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Inntekthistorikk {

    //Hvordan løser vi as-is/as-of
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

    internal fun add(
        dato: LocalDate,
        meldingsreferanseId: UUID,
        inntekt: Inntekt,
        kilde: Kilde,
        tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        inntekter.add(Inntektsendring(dato, meldingsreferanseId, inntekt, kilde, tidsstempel))
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
        inntekter.add(
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
        inntekter.add(Saksbehandler(dato, meldingsreferanseId, inntekt, kilde, begrunnelse, tidsstempel))
    }

    internal fun sykepengegrunnlag(dato: LocalDate) =
        GrunnlagForSykepengegrunnlagVisitor(dato)
            .also(this::accept)
            .sykepengegrunnlag()

    internal fun sammenligningsgrunnlag(dato: LocalDate) =
        Inntektsendring.sammenligningsgrunnlag(dato, inntekter)

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
            visitor.visitInntekt(this, hendelseId, kilde, fom)
        }

        companion object {
            internal fun sammenligningsgrunnlag(dato: LocalDate, inntekter: List<Inntektsendring>): Inntekt {
                return inntekter
                    .filter { it.kilde == Kilde.SKATT_SAMMENLIGNINSGRUNNLAG }
                    .takeLatestBy { it.tidsstempel }
                    .filter { YearMonth.from(it.fom) in YearMonth.from(dato).let { it.minusMonths(12)..it.minusMonths(1) } }
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
                visitor.visitInntektSkatt(this, hendelseId, kilde, fom)
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
                visitor.visitInntektSaksbehandler(this, hendelseId, kilde, fom)
            }
        }
    }
}
