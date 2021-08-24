package no.nav.helse.person


import no.nav.helse.Appender
import no.nav.helse.AppenderFeature
import no.nav.helse.appender
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.Inntektshistorikk.Innslag.Companion.nyesteId
import no.nav.helse.serde.reflection.Inntektsopplysningskilde
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

internal class Inntektshistorikk {

    private val historikk = mutableListOf<Innslag>()

    private val innslag
        get() = (historikk.firstOrNull()?.clone() ?: Innslag(UUID.randomUUID()))
            .also { historikk.add(0, it) }

    internal operator fun invoke(block: AppendMode.() -> Unit) {
        appender(AppendMode, block)
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun nyesteId() = historikk.nyesteId()

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, dato: LocalDate): Inntekt? =
        grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt, dato)?.second

    internal fun grunnlagForSykepengegrunnlag(dato: LocalDate): Inntekt? =
        grunnlagForSykepengegrunnlagMedMetadata(dato)?.second

    internal fun grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt: LocalDate, dato: LocalDate): Pair<Inntektsopplysning, Inntekt>? =
        grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt) ?: skjæringstidspunkt
            .takeIf { it <= dato }
            ?.let { grunnlagForSykepengegrunnlagFraInfotrygdMedMetadata(it til dato) }

    private fun grunnlagForSykepengegrunnlagMedMetadata(dato: LocalDate): Pair<Inntektsopplysning, Inntekt>? =
        historikk.firstOrNull()?.grunnlagForSykepengegrunnlag(dato)

    private fun grunnlagForSykepengegrunnlagFraInfotrygdMedMetadata(periode: Periode): Pair<Inntektsopplysning, Inntekt>? =
        historikk.firstOrNull()?.grunnlagForSykepengegrunnlagFraInfotrygd(periode)

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Inntekt? =
        grunnlagForSammenligningsgrunnlagMedMetadata(dato)?.second

    internal fun grunnlagForSammenligningsgrunnlagMedMetadata(dato: LocalDate): Pair<Inntektsopplysning, Inntekt>? =
        historikk.firstOrNull()?.grunnlagForSammenligningsgrunnlag(dato)

    internal fun sykepengegrunnlagKommerFraSkatt(skjæringstidspunkt: LocalDate) = grunnlagForSykepengegrunnlagMedMetadata(skjæringstidspunkt)?.first.let { it == null || it is SkattComposite }
    internal fun harGrunnlagForSykepengegrunnlag(dato: LocalDate) =  grunnlagForSykepengegrunnlag(dato) != null
    private fun harGrunnlagForSammenligningsgrunnlag(dato: LocalDate) = grunnlagForSammenligningsgrunnlag(dato) != null
    internal fun harGrunnlagForSykepengegrunnlagEllerSammenligningsgrunnlag(dato: LocalDate) = harGrunnlagForSykepengegrunnlag(dato) || harGrunnlagForSammenligningsgrunnlag(dato)

    internal class Innslag(private val id: UUID) {
        private val inntekter = mutableListOf<Inntektsopplysning>()

        internal fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitInnslag(this, id)
            inntekter.forEach { it.accept(visitor) }
            visitor.postVisitInnslag(this, id)
        }

        internal fun clone() = Innslag(UUID.randomUUID()).also {
            it.inntekter.addAll(this.inntekter)
        }

        internal fun add(inntektsopplysning: Inntektsopplysning) {
            if (inntekter.all { it.kanLagres(inntektsopplysning) }) {
                inntekter.removeIf { it.skalErstattesAv(inntektsopplysning) }
                inntekter.add(inntektsopplysning)
            }
        }

        internal fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSykepengegrunnlag(dato) }
                .firstOrNull()

        internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSammenligningsgrunnlag(dato) }
                .firstOrNull()

        internal fun grunnlagForSykepengegrunnlagFraInfotrygd(periode: Periode) =
            inntekter
                .filterIsInstance<Infotrygd>()
                .sorted()
                .mapNotNull { it.grunnlagForSykepengegrunnlag(periode) }
                .firstOrNull()

        internal companion object {
            internal fun List<Innslag>.nyesteId() = this.first().id
        }
    }

    internal interface Inntektsopplysning : Comparable<Inntektsopplysning> {
        val dato: LocalDate
        val prioritet: Int
        fun accept(visitor: InntekthistorikkVisitor)
        fun grunnlagForSykepengegrunnlag(dato: LocalDate): Pair<Inntektsopplysning, Inntekt>? = null
        fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Pair<Inntektsopplysning, Inntekt>? = null
        fun skalErstattesAv(other: Inntektsopplysning): Boolean
        override fun compareTo(other: Inntektsopplysning) =
            (-this.dato.compareTo(other.dato)).takeUnless { it == 0 } ?: -this.prioritet.compareTo(other.prioritet)

        fun kanLagres(other: Inntektsopplysning) = true
    }

    internal class Saksbehandler(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 100

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitSaksbehandler(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) = takeIf { it.dato == dato }?.let { it to it.beløp }

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Saksbehandler && this.dato == other.dato

        internal fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to Inntektsopplysningskilde.SAKSBEHANDLER,
            "tidsstempel" to tidsstempel
        )
    }

    internal class Infotrygd(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 80

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInfotrygd(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) = takeIf { it.dato == dato }?.let { it to it.beløp }
        internal fun grunnlagForSykepengegrunnlag(periode: Periode) = takeIf { it.dato in periode }?.let { it to it.beløp }

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Infotrygd && this.dato == other.dato

        internal fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to Inntektsopplysningskilde.INFOTRYGD,
            "tidsstempel" to tidsstempel
        )
    }

    internal class Inntektsmelding(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 60

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektsmelding(this, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) = takeIf { it.dato == dato }?.let { it to it.beløp }

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Inntektsmelding && this.dato == other.dato

        override fun kanLagres(other: Inntektsopplysning) =
            other !is Inntektsmelding || this.dato != other.dato

        internal fun toMap(): Map<String, Any?> = mapOf(
            "id" to id,
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to  Inntektsopplysningskilde.INNTEKTSMELDING,
            "tidsstempel" to tidsstempel
        )
    }

    internal class SkattComposite(
        private val id: UUID,
        private val inntektsopplysninger: List<Skatt>
    ) : Inntektsopplysning {

        override val dato = inntektsopplysninger.first().dato
        override val prioritet = inntektsopplysninger.first().prioritet

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitSkatt(this, id)
            inntektsopplysninger.forEach { it.accept(visitor) }
            visitor.postVisitSkatt(this, id)
        }

        override fun grunnlagForSykepengegrunnlag(dato: LocalDate) =
            inntektsopplysninger
                .mapNotNull { it.grunnlagForSykepengegrunnlag(dato) }
                .takeIf { it.isNotEmpty() }
                ?.map { (_, sum) -> sum }
                ?.summer()
                ?.div(3)
                ?.let { this to it }

        override fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            inntektsopplysninger
                .mapNotNull { it.grunnlagForSammenligningsgrunnlag(dato) }
                .takeIf { it.isNotEmpty() }
                ?.map { (_, sum) -> sum }
                ?.summer()
                ?.div(12)
                ?.let { this to it }

        override fun skalErstattesAv(other: Inntektsopplysning): Boolean =
            this.inntektsopplysninger.any { it.skalErstattesAv(other) }
                || (other is SkattComposite && other.inntektsopplysninger.any { this.skalErstattesAv(it) })
    }

    internal sealed class Skatt(
        override val dato: LocalDate,
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
                takeIf { this.dato == dato && måned.isWithinRangeOf(dato, 3) }?.let { it to it.beløp }

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
                takeIf { this.dato == dato && måned.isWithinRangeOf(dato, 12) }?.let { it to it.beløp }

            override fun skalErstattesAv(other: Inntektsopplysning) =
                other is Sammenligningsgrunnlag && this.dato == other.dato
        }
        internal fun toMap(kilde: Inntektsopplysningskilde): Map<String, Any?> =mapOf(
            "dato" to dato,
            "hendelseId" to hendelseId,
            "beløp" to beløp.reflection { _, månedlig, _, _ -> månedlig },
            "kilde" to kilde,
            "tidsstempel" to tidsstempel,

            "måned" to måned,
            "type" to type,
            "fordel" to fordel,
            "beskrivelse" to beskrivelse
        )
    }

    internal class AppendMode private constructor(private val innslag: Innslag) : Appender {
        companion object : AppenderFeature<Inntektshistorikk, AppendMode> {
            override fun append(a: Inntektshistorikk, appender: AppendMode.() -> Unit) {
                AppendMode(a.innslag).apply(appender).apply {
                    skatt.takeIf { it.isNotEmpty() }?.also { add(SkattComposite(UUID.randomUUID(), it)) }
                }
            }
        }

        private val tidsstempel = LocalDateTime.now()
        private val skatt = mutableListOf<Skatt>()

        internal fun addSaksbehandler(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Saksbehandler(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

        internal fun addInntektsmelding(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Inntektsmelding(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

        internal fun addInfotrygd(dato: LocalDate, hendelseId: UUID, beløp: Inntekt) =
            add(Infotrygd(UUID.randomUUID(), dato, hendelseId, beløp, tidsstempel))

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

    internal class RestoreJsonMode private constructor(private val inntektshistorikk: Inntektshistorikk) :
        Appender {
        companion object : AppenderFeature<Inntektshistorikk, RestoreJsonMode> {
            override fun append(a: Inntektshistorikk, appender: RestoreJsonMode.() -> Unit) {
                RestoreJsonMode(a).apply(appender)
            }
        }

        internal fun innslag(innslagId: UUID, block: InnslagAppender.() -> Unit) {
            Innslag(innslagId).also { InnslagAppender(it).apply(block) }.also { inntektshistorikk.historikk.add(0, it) }
        }

        internal class InnslagAppender(private val innslag: Innslag) {
            internal fun add(opplysning: Inntektsopplysning) = innslag.add(opplysning)
        }
    }
}

private fun YearMonth.isWithinRangeOf(dato: LocalDate, måneder: Long) =
    this in YearMonth.from(dato).let { it.minusMonths(måneder)..it.minusMonths(1) }
