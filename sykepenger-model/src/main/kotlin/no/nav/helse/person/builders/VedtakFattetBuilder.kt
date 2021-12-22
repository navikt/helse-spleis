package no.nav.helse.person.builders

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.PersonObserver
import no.nav.helse.person.Sykepengegrunnlag
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.økonomi.Inntekt
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtakFattetBuilder(
    private val periode: Periode,
    private val hendelseIder: Set<UUID>,
    private val skjæringstidspunkt: LocalDate,
    vilkårsgrunnlag: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement?
) {
    private val sykepengegrunnlag = vilkårsgrunnlag?.sykepengegrunnlag() ?: Inntekt.INGEN
    private val grunnlagForSykepengegrunnlag = vilkårsgrunnlag?.grunnlagForSykepengegrunnlag() ?: Inntekt.INGEN
    private val begrensning = vilkårsgrunnlag?.grunnlagsBegrensning()

    private val grunnlagForSykepengegrunnlagPerArbeidsgiver =  vilkårsgrunnlag?.inntektsopplysningPerArbeidsgiver()?.mapValues { (_, inntektsopplysning) ->
        inntektsopplysning.grunnlagForSykepengegrunnlag()
    } ?: emptyMap()

    private var vedtakFattetTidspunkt = LocalDateTime.now()
    private var utbetalingId: UUID? = null

    internal fun utbetalingId(id: UUID) = apply { this.utbetalingId = id }
    internal fun utbetalingVurdert(tidspunkt: LocalDateTime) = apply { this.vedtakFattetTidspunkt = tidspunkt }

    internal fun result(): PersonObserver.VedtakFattetEvent {
        return PersonObserver.VedtakFattetEvent(
            periode = periode,
            hendelseIder = hendelseIder,
            skjæringstidspunkt = skjæringstidspunkt,
            sykepengegrunnlag = sykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
            grunnlagForSykepengegrunnlag = grunnlagForSykepengegrunnlag.reflection { årlig, _, _, _ -> årlig },
            grunnlagForSykepengegrunnlagPerArbeidsgiver = grunnlagForSykepengegrunnlagPerArbeidsgiver.mapValues { (_, inntekt) -> inntekt.reflection { årlig, _, _, _ -> årlig} },
            inntekt = grunnlagForSykepengegrunnlag.reflection { _, månedlig, _, _ -> månedlig },
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
