package no.nav.helse.person


import no.nav.helse.Appender
import no.nav.helse.AppenderFeature
import no.nav.helse.appender
import no.nav.helse.utbetalingstidslinje.ArbeidsgiverRegler
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.INGEN
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class InntektshistorikkVol2 {

    private val historikk = mutableListOf<Innslag>()

    private val innslag
        get() = (historikk.firstOrNull()?.clone() ?: Innslag())
            .also { historikk.add(0, it) }

    internal operator fun invoke(block: AppendMode.() -> Unit) {
        appender(AppendMode, block)
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikkVol2(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikkVol2(this)
    }

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
        historikk.first().grunnlagForSykepengegrunnlag(dato) ?: INGEN

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
        historikk.first().grunnlagForSammenligningsgrunnlag(dato) ?: INGEN

    internal fun dekningsgrunnlag(dato: LocalDate, regler: ArbeidsgiverRegler) =
        grunnlagForSykepengegrunnlag(dato).times(regler.dekningsgrad())

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

        fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSykepengegrunnlag(dato) }
                .firstOrNull()

        fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSammenligningsgrunnlag(dato) }
                .firstOrNull()

    }

    internal interface Inntektsopplysning : Comparable<Inntektsopplysning> {
        val prioritet: Int
        fun accept(visitor: InntekthistorikkVisitor)
        fun grunnlagForSykepengegrunnlag(dato: LocalDate): Inntekt? = null
        fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Inntekt? = null
        fun skalErstattesAv(other: Inntektsopplysning): Boolean
        override fun compareTo(other: Inntektsopplysning) = -this.prioritet.compareTo(other.prioritet)
    }

    internal class Saksbehandler(
        private val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 100

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitSaksbehandler(this)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) = if (dato == this.dato) beløp else null

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Saksbehandler && this.dato == other.dato
    }


    internal class Inntektsmelding(
        private val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 80

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektsmelding(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) = if (dato == this.dato) beløp else null

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Inntektsmelding && this.dato == other.dato
    }

    internal class Infotrygd(
        private val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 60

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInfotrygd(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) = if (dato == this.dato) beløp else null

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Infotrygd && this.dato == other.dato
    }

    internal class SkattComposite(
        private val inntektsopplysninger: List<Skatt> = listOf()
    ) : Inntektsopplysning {

        override val prioritet = inntektsopplysninger.map { it.prioritet }.max() ?: 0

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitSkatt(this)
            inntektsopplysninger.forEach { it.accept(visitor) }
            visitor.postVisitSkatt(this)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
            inntektsopplysninger
                .mapNotNull { it.grunnlagForSykepengegrunnlag(dato) }
                .takeIf { it.isNotEmpty() }
                ?.summer()
                ?.div(3)

        override fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            inntektsopplysninger
                .mapNotNull { it.grunnlagForSammenligningsgrunnlag(dato) }
                .takeIf { it.isNotEmpty() }
                ?.summer()
                ?.div(12)

        override fun skalErstattesAv(other: Inntektsopplysning): Boolean =
            this.inntektsopplysninger.any { it.skalErstattesAv(other) }
                || (other is SkattComposite && other.inntektsopplysninger.any { this.skalErstattesAv(it) })
    }

    internal sealed class Skatt(
        protected val dato: LocalDate,
        protected val hendelseId: UUID,
        protected val beløp: Inntekt,
        protected val måned: YearMonth,
        protected val type: Inntekttype,
        protected val fordel: String,
        protected val beskrivelse: String,
        protected val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        internal enum class Inntekttype {
            LØNNSINNTEKT,
            NÆRINGSINNTEKT,
            PENSJON_ELLER_TRYGD,
            YTELSE_FRA_OFFENTLIGE
        }

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
            hendelseId,
            beløp,
            måned,
            type,
            fordel,
            beskrivelse,
            tidsstempel
        ) {
            override val prioritet = 40

            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitSkattSykepengegrunnlag(
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

            override fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
                if (this.dato == dato && måned.isWithinRangeOf(dato, 3)) beløp else null

            override fun skalErstattesAv(other: Inntektsopplysning) =
                other is Sykepengegrunnlag && this.dato == other.dato && this.tidsstempel != other.tidsstempel
        }

        internal class Sammenligningsgrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Inntekttype,
            fordel: String,
            beskrivelse: String,
            tidsstempel: LocalDateTime = LocalDateTime.now()
        ) :
            Skatt(dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel) {
            override val prioritet = 20

            override fun accept(visitor: InntekthistorikkVisitor) {
                visitor.visitSkattSammenligningsgrunnlag(
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

            override fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
                if (this.dato == dato && måned.isWithinRangeOf(dato, 12)) beløp else null

            override fun skalErstattesAv(other: Inntektsopplysning) =
                other is Sammenligningsgrunnlag && this.dato == other.dato
        }
    }

    internal class AppendMode private constructor(private val innslag: Innslag) : Appender {
        companion object : AppenderFeature<InntektshistorikkVol2, AppendMode> {
            override fun append(a: InntektshistorikkVol2, appender: AppendMode.() -> Unit) {
                AppendMode(a.innslag).apply(appender).apply {
                    skatt.takeIf { it.isNotEmpty() }?.also { add(SkattComposite(it)) }
                }
            }
        }

        private val tidsstempel = LocalDateTime.now()
        private val skatt = mutableListOf<Skatt>()

        internal fun addSaksbehandler(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Saksbehandler(dato, hendelseId, beløp, tidsstempel))

        internal fun addInntektsmelding(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Inntektsmelding(dato, hendelseId, beløp, tidsstempel))

        internal fun addInfotrygd(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Infotrygd(dato, hendelseId, beløp, tidsstempel))

        internal fun addSkattSykepengegrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String
        ) =
            skatt.add(
                Skatt.Sykepengegrunnlag(
                    dato,
                    hendelseId,
                    beløp,
                    måned,
                    type,
                    fordel,
                    beskrivelse,
                    tidsstempel
                )
            )

        internal fun addSkattSammenligningsgrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String
        ) =
            skatt.add(
                Skatt.Sammenligningsgrunnlag(
                    dato,
                    hendelseId,
                    beløp,
                    måned,
                    type,
                    fordel,
                    beskrivelse,
                    tidsstempel
                )
            )

        private fun add(opplysning: Inntektsopplysning) {
            innslag.add(opplysning)
        }
    }

    internal class RestoreJsonMode private constructor(private val inntektshistorikkVol2: InntektshistorikkVol2) :
        Appender {
        companion object : AppenderFeature<InntektshistorikkVol2, RestoreJsonMode> {
            override fun append(a: InntektshistorikkVol2, appender: RestoreJsonMode.() -> Unit) {
                RestoreJsonMode(a).apply(appender)
            }
        }

        internal fun innslag(block: InnslagAppender.() -> Unit) {
            Innslag().also { InnslagAppender(it).apply(block) }.also { inntektshistorikkVol2.historikk.add(it) }
        }

        internal class InnslagAppender(private val innslag: Innslag) {
            internal fun add(opplysning: Inntektsopplysning) = innslag.add(opplysning)

            internal fun skatt(block: SkattAppender.() -> Unit) {
                SkattAppender(this).apply(block).finalize()
            }

            internal class SkattAppender(private val innslagAppender: InnslagAppender) {
                private val skatt = mutableListOf<Skatt>()
                internal fun finalize() =
                    skatt.takeIf { it.isNotEmpty() }?.apply { innslagAppender.innslag.add(SkattComposite(this)) }

                internal fun add(skatt: Skatt) = this.skatt.add(skatt)
            }
        }
    }
}

private fun YearMonth.isWithinRangeOf(dato: LocalDate, måneder: Long) =
    this in YearMonth.from(dato).let { it.minusMonths(måneder)..it.minusMonths(1) }
