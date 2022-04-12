package no.nav.helse.person


import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*
import no.nav.helse.hendelser.Periode
import no.nav.helse.hendelser.til
import no.nav.helse.person.etterlevelse.SubsumsjonObserver
import no.nav.helse.person.etterlevelse.SubsumsjonObserver.Companion.subsumsjonsformat
import no.nav.helse.person.filter.Utbetalingsfilter
import no.nav.helse.økonomi.Inntekt
import no.nav.helse.økonomi.Inntekt.Companion.summer
import no.nav.helse.person.infotrygdhistorikk.Inntektsopplysning as InfotrygdhistorikkInntektsopplysning

internal class Inntektshistorikk {

    private val historikk = mutableListOf<Innslag>()

    internal companion object {
        internal val NULLUUID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }

    internal fun accept(visitor: InntekthistorikkVisitor) {
        visitor.preVisitInntekthistorikk(this)
        historikk.forEach { it.accept(visitor) }
        visitor.postVisitInntekthistorikk(this)
    }

    internal fun nyttInnslag(): Innslag {
        val innslag = nyesteInnslag()?.clone() ?: Innslag(UUID.randomUUID())
        historikk.add(0, innslag)
        return innslag
    }

    internal fun nyesteInnslag() = historikk.firstOrNull()

    internal fun nyesteId() = Innslag.nyesteId(this)

    internal fun isNotEmpty() = historikk.isNotEmpty()

    internal fun harInntektsmelding(førsteFraværsdag: LocalDate) = historikk.any {
        it.harInntektsmelding(førsteFraværsdag)
    }

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, dato: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? =
        grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) ?: skjæringstidspunkt
            .takeIf { it <= dato }
            ?.let { nyesteInnslag()?.grunnlagForSykepengegrunnlagFraInfotrygd(it til dato) }

    internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? =
        nyesteInnslag()?.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag)

    internal fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Inntektsopplysning? =
        nyesteInnslag()?.grunnlagForSammenligningsgrunnlag(dato)

    internal fun sykepengegrunnlagKommerFraSkatt(skjæringstidspunkt: LocalDate) =
        grunnlagForSykepengegrunnlag(skjæringstidspunkt, skjæringstidspunkt).let { it == null || it is SkattComposite }

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

        internal fun harInntektsmelding(førsteFraværsdag: LocalDate) = inntekter
            .sorted()
            .any { it.harInntektsmelding(førsteFraværsdag) }

        internal fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            inntekter
                .sorted()
                .mapNotNull { it.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) }
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

        internal fun build(filter: Utbetalingsfilter.Builder, inntektsmeldingId: UUID) {
            inntekter.forEach { it.build(filter, inntektsmeldingId) }
        }

        internal fun erDuplikat(inntektsopplysning: InfotrygdhistorikkInntektsopplysning) =
            inntekter.filterIsInstance<Infotrygd>().any { it.erDuplikat(inntektsopplysning) }

        internal companion object {
            internal fun nyesteId(inntektshistorikk: Inntektshistorikk) = inntektshistorikk.nyesteInnslag()!!.id
        }
    }

    internal interface Inntektsopplysning : Comparable<Inntektsopplysning> {
        val dato: LocalDate
        val prioritet: Int
        fun accept(visitor: InntekthistorikkVisitor)
        fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?): Inntektsopplysning? = null
        fun grunnlagForSykepengegrunnlag(): Inntekt
        fun grunnlagForSammenligningsgrunnlag(dato: LocalDate): Inntektsopplysning? = null
        fun grunnlagForSammenligningsgrunnlag(): Inntekt
        fun skalErstattesAv(other: Inntektsopplysning): Boolean
        fun subsumsjon(subsumsjonObserver: SubsumsjonObserver, skjæringstidspunkt: LocalDate, organisasjonsnummer: String) = run {}
        override fun compareTo(other: Inntektsopplysning) =
            (-this.dato.compareTo(other.dato)).takeUnless { it == 0 } ?: -this.prioritet.compareTo(other.prioritet)

        fun kanLagres(other: Inntektsopplysning) = true
        fun harInntektsmelding(førsteFraværsdag: LocalDate) = false
        fun build(filter: Utbetalingsfilter.Builder, inntektsmeldingId: UUID) {}
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
            visitor.visitSaksbehandler(this, id, dato, hendelseId, beløp, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) = takeIf { it.dato == skjæringstidspunkt }
        override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Saksbehandler har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Saksbehandler && this.dato == other.dato

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
            visitor.visitInfotrygd(this, id, dato, hendelseId, beløp, tidsstempel)
        }

        // TODO: egen test for å bruke førstefraværsdag her: https://trello.com/c/QFYSoFOs
        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) = takeIf { it.dato == skjæringstidspunkt }
        override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

        internal fun grunnlagForSykepengegrunnlag(periode: Periode) = takeIf { it.dato in periode }

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Infotrygd har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Infotrygd && this.dato == other.dato

        internal fun erDuplikat(other: InfotrygdhistorikkInntektsopplysning) = other.erDuplikat(dato, beløp)

        // Vi overrider equals for å kunne sjekke om et vilkårsgrunnlag for infotrygd er et duplikat
        override fun equals(other: Any?): Boolean {
            if (other !is Infotrygd) return false
            return id == other.id
                    && dato == other.dato
                    && hendelseId == other.hendelseId
                    && beløp == other.beløp
                    && prioritet == other.prioritet
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + dato.hashCode()
            result = 31 * result + hendelseId.hashCode()
            result = 31 * result + beløp.hashCode()
            result = 31 * result + prioritet
            return result
        }
    }

    internal class Inntektsmelding(
        private val id: UUID,
        override val dato: LocalDate,
        private val hendelseId: UUID,
        private val beløp: Inntekt,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {
        override val prioritet = 60

        override fun build(filter: Utbetalingsfilter.Builder, inntektsmeldingId: UUID) {
            if (hendelseId != inntektsmeldingId) return
            filter.inntektsmeldingtidsstempel(tidsstempel)
        }

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitInntektsmelding(this, id, dato, hendelseId, beløp, tidsstempel)
        }

        override fun harInntektsmelding(førsteFraværsdag: LocalDate) =
            førsteFraværsdag == dato

        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            takeIf { førsteFraværsdag != null && YearMonth.from(skjæringstidspunkt) == YearMonth.from(førsteFraværsdag) && it.dato == førsteFraværsdag }

        override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Inntektsmelding har ikke grunnlag for sammenligningsgrunnlag")

        override fun skalErstattesAv(other: Inntektsopplysning) =
            other is Inntektsmelding && this.dato == other.dato

        override fun kanLagres(other: Inntektsopplysning) =
            other !is Inntektsmelding || this.dato != other.dato
    }

    internal class SkattComposite(
        private val id: UUID,
        private val inntektsopplysninger: List<Skatt>
    ) : Inntektsopplysning {

        override val dato = inntektsopplysninger.first().dato
        override val prioritet = inntektsopplysninger.first().prioritet

        private val inntekterSisteTreMåneder = inntektsopplysninger.filter { it.erRelevant(3) }

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.preVisitSkatt(this, id, dato)
            inntektsopplysninger.forEach { it.accept(visitor) }
            visitor.postVisitSkatt(this, id, dato)
        }

        override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
            takeIf {
                inntektsopplysninger.any {
                    it.grunnlagForSykepengegrunnlag(skjæringstidspunkt, førsteFraværsdag) != null
                        && it.erRelevant(Arbeidsforholdhistorikk.Arbeidsforhold.MAKS_INNTEKT_GAP)
                }
            }

        override fun grunnlagForSykepengegrunnlag(): Inntekt {
            return inntekterSisteTreMåneder
                .map(Skatt::grunnlagForSykepengegrunnlag)
                .summer()
                .div(3)
        }

        override fun grunnlagForSammenligningsgrunnlag(dato: LocalDate) =
            takeIf { inntektsopplysninger.any { it.grunnlagForSammenligningsgrunnlag(dato) != null } }

        override fun grunnlagForSammenligningsgrunnlag(): Inntekt =
            inntektsopplysninger
                .filter { it.erRelevant(12) }
                .map(Skatt::grunnlagForSammenligningsgrunnlag)
                .summer()
                .div(12)

        internal fun sammenligningsgrunnlag() = grunnlagForSammenligningsgrunnlag(dato)?.grunnlagForSammenligningsgrunnlag()

        override fun subsumsjon(subsumsjonObserver: SubsumsjonObserver, skjæringstidspunkt: LocalDate, organisasjonsnummer: String) {
            subsumsjonObserver.`§ 8-28 ledd 3 bokstav a`(
                organisasjonsnummer = organisasjonsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                inntekterSisteTreMåneder = inntekterSisteTreMåneder.subsumsjonsformat(),
                grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag()
            )
            subsumsjonObserver.`§ 8-29`(skjæringstidspunkt, grunnlagForSykepengegrunnlag(), inntektsopplysninger.subsumsjonsformat(), organisasjonsnummer)
        }

        override fun skalErstattesAv(other: Inntektsopplysning): Boolean =
            this.inntektsopplysninger.any { it.skalErstattesAv(other) }
                || (other is SkattComposite && other.inntektsopplysninger.any { this.skalErstattesAv(it) })
    }

    internal class IkkeRapportert(
        private val id: UUID,
        override val dato: LocalDate,
        private val tidsstempel: LocalDateTime = LocalDateTime.now()
    ) : Inntektsopplysning {

        override val prioritet = 10

        override fun accept(visitor: InntekthistorikkVisitor) {
            visitor.visitIkkeRapportert(id, dato, tidsstempel)
        }

        override fun grunnlagForSykepengegrunnlag() = Inntekt.INGEN

        override fun grunnlagForSammenligningsgrunnlag() = Inntekt.INGEN

        override fun skalErstattesAv(other: Inntektsopplysning) = other is IkkeRapportert && this.dato == other.dato
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

        internal fun erRelevant(måneder: Long) = måned.isWithinRangeOf(dato, måneder)

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
                visitor.visitSkattSykepengegrunnlag(this, dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel)
            }

            override fun grunnlagForSykepengegrunnlag(skjæringstidspunkt: LocalDate, førsteFraværsdag: LocalDate?) =
                takeIf { this.dato == skjæringstidspunkt && måned.isWithinRangeOf(skjæringstidspunkt, 3) }

            override fun grunnlagForSykepengegrunnlag(): Inntekt = beløp

            override fun grunnlagForSammenligningsgrunnlag(): Inntekt = error("Sykepengegrunnlag har ikke grunnlag for sammenligningsgrunnlag")

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
                takeIf { this.dato == dato && måned.isWithinRangeOf(dato, 12) }

            override fun grunnlagForSammenligningsgrunnlag(): Inntekt = beløp

            override fun grunnlagForSykepengegrunnlag(): Inntekt = error("Sammenligningsgrunnlag har ikke grunnlag for sykepengegrunnlag")

            override fun skalErstattesAv(other: Inntektsopplysning) =
                other is Sammenligningsgrunnlag && this.dato == other.dato
        }
    }

    internal fun append(block: AppendMode.() -> Unit) {
        AppendMode(nyttInnslag()).append(block)
    }

    internal class AppendMode(private val innslag: Innslag) {
        internal fun append(appender: AppendMode.() -> Unit) {
            apply(appender)
            skatt.takeIf { it.isNotEmpty() }?.also { add(SkattComposite(UUID.randomUUID(), it)) }
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
            skatt.add(Skatt.Sykepengegrunnlag(dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel))

        internal fun addSkattSammenligningsgrunnlag(
            dato: LocalDate,
            hendelseId: UUID,
            beløp: Inntekt,
            måned: YearMonth,
            type: Skatt.Inntekttype,
            fordel: String,
            beskrivelse: String
        ) =
            skatt.add(Skatt.Sammenligningsgrunnlag(dato, hendelseId, beløp, måned, type, fordel, beskrivelse, tidsstempel))

        private fun add(opplysning: Inntektsopplysning) {
            innslag.add(opplysning)
        }
    }

    internal fun restore(block: RestoreJsonMode.() -> Unit) {
        RestoreJsonMode(this).apply(block)
    }

    internal fun build(filter: Utbetalingsfilter.Builder, inntektsmeldingId: UUID) {
        nyesteInnslag()?.build(filter, inntektsmeldingId)
    }

    internal fun filtrerBortKjenteInntekter(inntektsopplysninger: List<InfotrygdhistorikkInntektsopplysning>): List<InfotrygdhistorikkInntektsopplysning> {
        val nyesteInnslag = nyesteInnslag() ?: return inntektsopplysninger
        return inntektsopplysninger.filter { !nyesteInnslag.erDuplikat(it) }
    }

    internal class RestoreJsonMode(private val inntektshistorikk: Inntektshistorikk) {
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
