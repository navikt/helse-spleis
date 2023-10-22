package no.nav.helse.person.builders

import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattEtterHovedregel
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattEtterSkjønn
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.FastsattIInfotrygd
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.Sykepengegrunnlagsfakta
import no.nav.helse.person.PersonObserver.VedtakFattetEvent.Tag
import no.nav.helse.person.inntekt.Sykepengegrunnlag.Begrensning
import no.nav.helse.person.inntekt.Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET
import no.nav.helse.økonomi.Avviksprosent
import no.nav.helse.økonomi.Inntekt

internal class VedtakFattetBuilder(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periode: Periode,
    private val hendelseIder: Set<UUID>,
    private val skjæringstidspunkt: LocalDate,
    private val tags: MutableSet<Tag> = mutableSetOf()
) {
    private var sykepengegrunnlag =  Inntekt.INGEN
    private var beregningsgrunnlag = Inntekt.INGEN
    private var begrensning: Begrensning? = null
    private var omregnetÅrsinntektPerArbeidsgiverÅrlig = emptyMap<String, Double>()
    private var vedtakFattetTidspunkt = LocalDateTime.now()
    private var utbetalingId: UUID? = null

    internal fun utbetalingId(id: UUID) = apply { this.utbetalingId = id }
    internal fun utbetalingVurdert(tidspunkt: LocalDateTime) = apply { this.vedtakFattetTidspunkt = tidspunkt }
    internal fun sykepengegrunnlag(sykepengegrunnlag: Inntekt) = apply { this.sykepengegrunnlag = sykepengegrunnlag }
    internal fun beregningsgrunnlag(beregningsgrunnlag: Inntekt) = apply { this.beregningsgrunnlag = beregningsgrunnlag }
    internal fun begrensning(begrensning: Begrensning) = apply { this.begrensning = begrensning }
    internal fun omregnetÅrsinntektPerArbeidsgiver(omregnetÅrsinntektPerArbeidsgiverÅrlig: Map<String, Double>) = apply { this.omregnetÅrsinntektPerArbeidsgiverÅrlig = omregnetÅrsinntektPerArbeidsgiverÅrlig }
    internal fun ingenNyArbeidsgiverperiode() = apply { tags.add(Tag.IngenNyArbeidsgiverperiode) }

    private var sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta? = null
    internal fun sykepengegrunnlagsfakta(sykepengegrunnlagsfakta: Sykepengegrunnlagsfakta) = apply { this.sykepengegrunnlagsfakta = sykepengegrunnlagsfakta }

    internal fun result(): PersonObserver.VedtakFattetEvent {
        check((utbetalingId == null) || (sykepengegrunnlagsfakta != null)) { "Må ha sykepengegrunnlagsfakta dersom utbetalingsId er satt" }
        return PersonObserver.VedtakFattetEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periode = periode,
            hendelseIder = hendelseIder,
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
            beregningsgrunnlag = beregningsgrunnlag.reflection { årlig, _, _, _ -> årlig },
            omregnetÅrsinntektPerArbeidsgiver = omregnetÅrsinntektPerArbeidsgiverÅrlig,
            inntekt = beregningsgrunnlag.reflection { _, månedlig, _, _ -> månedlig },
            utbetalingId = utbetalingId,
            sykepengegrunnlagsbegrensning = when (begrensning) {
                ER_6G_BEGRENSET -> "ER_6G_BEGRENSET"
                Begrensning.ER_IKKE_6G_BEGRENSET -> "ER_IKKE_6G_BEGRENSET"
                Begrensning.VURDERT_I_INFOTRYGD -> "VURDERT_I_INFOTRYGD"
                else -> "VET_IKKE"
            },
            vedtakFattetTidspunkt = vedtakFattetTidspunkt,
            sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
            tags = tags
        )
    }

    sealed class SykepengegrunnlagsfaktaBuilder {
        protected val Inntekt.årlig get() = reflection { årlig, _, _, _ -> årlig }.toDesimaler
        protected val Double.toDesimaler get() = toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
        abstract fun build(): Sykepengegrunnlagsfakta
    }
    internal class FastsattIInfotrygdBuilder(private val omregnetÅrsinntekt: Inntekt) : SykepengegrunnlagsfaktaBuilder() {
        override fun build() = FastsattIInfotrygd(omregnetÅrsinntekt.årlig)
    }
    sealed class FastsattISpleisBuilder(
        protected val omregnetÅrsinntekt: Inntekt,
        protected val avviksprosent: Avviksprosent,
        protected val `6G`: Inntekt,
        begrensning: Begrensning
    ) : SykepengegrunnlagsfaktaBuilder() {
        protected val tags: Set<Tag> = begrensning.takeIf { it == ER_6G_BEGRENSET }?.let { setOf(Tag.`6GBegrenset`) } ?: emptySet()
        protected lateinit var innrapportertÅrsinntekt: Inntekt
        internal fun innrapportertÅrsinntekt(innrapportertÅrsinntekt: Inntekt) = apply { this.innrapportertÅrsinntekt = innrapportertÅrsinntekt }
        protected val Avviksprosent.avrundet get() = rundTilToDesimaler()
    }
    internal class FastsattEtterHovedregelBuilder(omregnetÅrsinntekt: Inntekt, avviksprosent: Avviksprosent, `6G`: Inntekt, begrensning: Begrensning) : FastsattISpleisBuilder(omregnetÅrsinntekt, avviksprosent, `6G`, begrensning) {
        private val arbeidsgivere = mutableListOf<FastsattEtterHovedregel.Arbeidsgiver>()
        internal fun arbeidsgiver(arbeidsgiver: String, omregnetÅrsinntekt: Inntekt) = apply { arbeidsgivere.add(FastsattEtterHovedregel.Arbeidsgiver(arbeidsgiver, omregnetÅrsinntekt.årlig)) }
        override fun build() = FastsattEtterHovedregel(
            omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
            innrapportertÅrsinntekt = innrapportertÅrsinntekt.årlig,
            avviksprosent = avviksprosent.avrundet,
            `6G`= `6G`.årlig,
            tags = tags,
            arbeidsgivere = arbeidsgivere.toList()
        )
    }
    internal class FastsattEtterSkjønnBuilder(omregnetÅrsinntekt: Inntekt, avviksprosent: Avviksprosent, `6G`: Inntekt, begrensning: Begrensning, private val skjønnsfastsatt: Inntekt) : FastsattISpleisBuilder(omregnetÅrsinntekt, avviksprosent, `6G`, begrensning) {
        private val arbeidsgivere = mutableListOf<FastsattEtterSkjønn.Arbeidsgiver>()
        internal fun arbeidsgiver(arbeidsgiver: String, omregnetÅrsinntekt: Inntekt, skjønnsfastsatt: Inntekt) = apply { arbeidsgivere.add(FastsattEtterSkjønn.Arbeidsgiver(arbeidsgiver, omregnetÅrsinntekt.årlig, skjønnsfastsatt.årlig)) }
        override fun build() = FastsattEtterSkjønn(
            omregnetÅrsinntekt = omregnetÅrsinntekt.årlig,
            skjønnsfastsatt = skjønnsfastsatt.årlig,
            innrapportertÅrsinntekt = innrapportertÅrsinntekt.årlig,
            avviksprosent = avviksprosent.avrundet,
            `6G`= `6G`.årlig,
            tags = tags,
            arbeidsgivere = arbeidsgivere.toList()
        )
    }
}
