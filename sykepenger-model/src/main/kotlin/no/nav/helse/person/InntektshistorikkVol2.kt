package no.nav.helse.person


import no.nav.helse.person.InntektshistorikkVol2.Inntektsopplysning.*
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkVol2 {

    private val historikk = mutableListOf<Innslag>()

    private val innslag
        get() =
            (historikk.firstOrNull()?.clone() ?: Innslag())
                .also { historikk.add(0, it) }


    internal operator fun invoke(block: InnslagBuilder.() -> Unit) {
        InnslagBuilder(innslag).apply(block)
    }

    internal class InnslagBuilder(private val innslag: Innslag) {
        private val tidsstempel = LocalDateTime.now()

        internal fun add(
            dato: LocalDate,
            meldingsreferanseId: UUID,
            inntekt: Inntekt,
            kilde: Kilde,
            tidsstempel: LocalDateTime? = null
        ) {
            innslag.add(Inntektsopplysning(dato, meldingsreferanseId, inntekt, kilde, tidsstempel ?: this.tidsstempel))
        }

        internal fun add(
            dato: LocalDate,
            meldingsreferanseId: UUID,
            inntekt: Inntekt,
            kilde: Kilde,
            måned: YearMonth,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String,
            tilleggsinformasjon: String?,
            tidsstempel: LocalDateTime? = null
        ) {
            innslag.add(
                Skatt(
                    dato,
                    meldingsreferanseId,
                    inntekt,
                    kilde,
                    måned,
                    type,
                    fordel,
                    beskrivelse,
                    tilleggsinformasjon,
                    tidsstempel ?: this.tidsstempel
                )
            )
        }

        internal fun add(
            dato: LocalDate,
            meldingsreferanseId: UUID,
            inntekt: Inntekt,
            kilde: Kilde,
            begrunnelse: String,
            tidsstempel: LocalDateTime? = null
        ) {
            innslag.add(
                Saksbehandler(
                    dato,
                    meldingsreferanseId,
                    inntekt,
                    kilde,
                    begrunnelse,
                    tidsstempel ?: this.tidsstempel
                )
            )
        }
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikkVol2(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikkVol2(this)
    }

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
        GrunnlagForSykepengegrunnlagVisitor(dato)
            .also { historikk.firstOrNull()?.accept(it) }
            .sykepengegrunnlag()

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
        historikk.first().sammenligningsgrunnlag(dato)

    internal fun clone() = InntektshistorikkVol2().also {
        it.historikk.addAll(this.historikk.map(Innslag::clone))
    }

    internal class Innslag {

        private val inntekter = mutableListOf<Inntektsopplysning>()

        fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitInnslag(this)
            inntekter.forEach { it.accept(visitor) }
            visitor.postVisitInnslag(this)
        }

        fun clone() = Innslag().also {
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
        protected val dato: LocalDate,
        protected val hendelseId: UUID,
        private val beløp: Inntekt,
        protected val kilde: Kilde,
        protected val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) {
        internal fun inntekt() = beløp
        internal fun isBefore(other: Inntektsopplysning) = this.tidsstempel.isBefore(other.tidsstempel)
        internal fun isAfter(other: Inntektsopplysning) = this.tidsstempel.isAfter(other.tidsstempel)

        open fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektVol2(this, hendelseId, kilde, dato, tidsstempel)
        }

        internal fun skalErstattesAv(other: Inntektsopplysning) =
            this.dato == other.dato && this.kilde == other.kilde && this.tidsstempel != other.tidsstempel

        companion object {
            internal fun sammenligningsgrunnlag(dato: LocalDate, inntekter: List<Inntektsopplysning>): Inntekt {
                return inntekter
                    .filter { it.kilde == Kilde.SKATT_SAMMENLIGNINSGRUNNLAG }
                    .takeLatestBy { it.tidsstempel }
                    .filter {
                        YearMonth.from(it.dato) in YearMonth.from(dato).let { it.minusMonths(12)..it.minusMonths(1) }
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
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            kilde: Kilde,
            private val måned: YearMonth,
            private val type: Inntekttype,
            private val fordel: String,
            private val beskrivelse: String,
            private val tilleggsinformasjon: String?,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Inntektsopplysning(
            dato,
            hendelseId,
            beløp,
            kilde,
            tidsstempel
        ) {
            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitInntektSkattVol2(this, hendelseId, kilde, dato, måned, tidsstempel)
            }
        }

        internal class Saksbehandler(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            kilde: Kilde,
            private val begrunnelse: String,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) : Inntektsopplysning(
            dato,
            hendelseId,
            beløp,
            kilde,
            tidsstempel
        ) {
            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitInntektSaksbehandlerVol2(this, hendelseId, kilde, dato, tidsstempel)
            }
        }
    }
}
