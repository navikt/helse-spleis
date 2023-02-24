package no.nav.helse.inspectors

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VedtaksperiodeVisitor
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.utbetalingslinjer.Utbetaling

internal val Vedtaksperiode.inspektør get() = VedtaksperiodeInspektør(this)

internal class VedtaksperiodeInspektør(vedtaksperiode: Vedtaksperiode) : VedtaksperiodeVisitor {

    init {
        vedtaksperiode.accept(this)
    }

    internal lateinit var id: UUID
        private set
    internal lateinit var periode: Periode
        private set
    internal lateinit var skjæringstidspunkt: LocalDate
    internal lateinit var utbetalingIdTilVilkårsgrunnlagId: Pair<UUID, UUID?>

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: () -> Inntektskilde
    ) {
        this.id = id
        this.periode = periode
        this.skjæringstidspunkt = skjæringstidspunkt()
    }

    override fun preVisitVedtaksperiodeUtbetaling(
        grunnlagsdata: VilkårsgrunnlagHistorikk.VilkårsgrunnlagElement,
        utbetaling: Utbetaling
    ) {
        val vilkårsgrunnlagId = grunnlagsdata?.inspektør?.vilkårsgrunnlagId
        val utbetalingId = utbetaling.inspektør.utbetalingId
        utbetalingIdTilVilkårsgrunnlagId = utbetalingId to vilkårsgrunnlagId
    }
}
