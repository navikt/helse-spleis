package no.nav.helse.person.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.inntekt.Sykepengegrunnlag
import no.nav.helse.utbetalingslinjer.UtbetalingVedtakFattetBuilder
import no.nav.helse.økonomi.Inntekt

internal class VedtakFattetBuilder(
    private val fødselsnummer: String,
    private val aktørId: String,
    private val organisasjonsnummer: String,
    private val vedtaksperiodeId: UUID,
    private val periode: Periode,
    private val hendelseIder: Set<UUID>,
    private val skjæringstidspunkt: LocalDate
): UtbetalingVedtakFattetBuilder {
    private var sykepengegrunnlag =  Inntekt.INGEN
    private var beregningsgrunnlag = Inntekt.INGEN
    private var begrensning: Sykepengegrunnlag.Begrensning? = null
    private var omregnetÅrsinntektPerArbeidsgiver = emptyMap<String, Inntekt>()
    private var vedtakFattetTidspunkt = LocalDateTime.now()
    private var utbetalingId: UUID? = null

    override fun utbetalingId(id: UUID) = apply { this.utbetalingId = id }
    override fun utbetalingVurdert(tidspunkt: LocalDateTime) = apply { this.vedtakFattetTidspunkt = tidspunkt }
    internal fun sykepengegrunnlag(sykepengegrunnlag: Inntekt) = apply { this.sykepengegrunnlag = sykepengegrunnlag }
    internal fun beregningsgrunnlag(beregningsgrunnlag: Inntekt) = apply { this.beregningsgrunnlag = beregningsgrunnlag }
    internal fun begrensning(begrensning: Sykepengegrunnlag.Begrensning) = apply { this.begrensning = begrensning }
    internal fun omregnetÅrsinntektPerArbeidsgiver(omregnetÅrsinntektPerArbeidsgiver: Map<String, Inntekt>) = apply { this.omregnetÅrsinntektPerArbeidsgiver = omregnetÅrsinntektPerArbeidsgiver }

    internal fun result(): PersonObserver.VedtakFattetEvent {
        return PersonObserver.VedtakFattetEvent(
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            organisasjonsnummer = organisasjonsnummer,
            vedtaksperiodeId = vedtaksperiodeId,
            periode = periode,
            hendelseIder = hendelseIder,
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag.månedlig,
            beregningsgrunnlag = beregningsgrunnlag.månedlig,
            omregnetÅrsinntektPerArbeidsgiver = omregnetÅrsinntektPerArbeidsgiver.mapValues { (_, inntekt) -> inntekt.månedlig },
            inntekt = beregningsgrunnlag.månedlig,
            utbetalingId = utbetalingId,
            sykepengegrunnlagsbegrensning = when (begrensning) {
                Sykepengegrunnlag.Begrensning.ER_6G_BEGRENSET -> "ER_6G_BEGRENSET"
                Sykepengegrunnlag.Begrensning.ER_IKKE_6G_BEGRENSET -> "ER_IKKE_6G_BEGRENSET"
                Sykepengegrunnlag.Begrensning.VURDERT_I_INFOTRYGD -> "VURDERT_I_INFOTRYGD"
                else -> "VET_IKKE"
            },
            vedtakFattetTidspunkt = vedtakFattetTidspunkt
        )
    }
}
