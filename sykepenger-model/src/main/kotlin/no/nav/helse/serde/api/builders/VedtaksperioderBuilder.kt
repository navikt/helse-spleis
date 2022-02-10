package no.nav.helse.serde.api.builders

import no.nav.helse.hendelser.Periode
import no.nav.helse.person.*
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.api.v2.HendelseDTO
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

internal class VedtaksperioderBuilder(
    private val arbeidsgiver: Arbeidsgiver,
    private val fødselsnummer: String,
    private val inntektshistorikkBuilder: InntektshistorikkBuilder,
    private val gruppeIder: MutableMap<Vedtaksperiode, UUID>,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val byggerForkastedePerioder: Boolean = false
) : BuilderState() {
    private val perioder = mutableListOf<VedtaksperiodeBuilder>()

    fun build(hendelser: List<HendelseDTO>, utbetalinger: List<UtbetalingshistorikkElementDTO>) =
        perioder.map { it.build(hendelser, utbetalinger) }

    private fun gruppeId(vedtaksperiode: Vedtaksperiode): UUID {
        val gruppeId = arbeidsgiver.finnSykeperiodeRettFør(vedtaksperiode)?.let(gruppeIder::getValue) ?: UUID.randomUUID()
        gruppeIder[vedtaksperiode] = gruppeId
        return gruppeId
    }

    override fun preVisitVedtaksperiode(
        vedtaksperiode: Vedtaksperiode,
        id: UUID,
        tilstand: Vedtaksperiode.Vedtaksperiodetilstand,
        opprettet: LocalDateTime,
        oppdatert: LocalDateTime,
        periode: Periode,
        opprinneligPeriode: Periode,
        skjæringstidspunkt: LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        periodetype: Periodetype,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<UUID>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        val vilkårsgrunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt) as? VilkårsgrunnlagHistorikk.Grunnlagsdata
        val sykepengegrunnlag = vilkårsgrunnlag?.sykepengegrunnlag()

        val vedtaksperiodeBuilder = VedtaksperiodeBuilder(
            vedtaksperiode = vedtaksperiode,
            id = id,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt,
            periodetype = periodetype,
            forlengelseFraInfotrygd = forlengelseFraInfotrygd,
            tilstand = tilstand,
            inntektskilde = inntektskilde,
            sykepengegrunnlag = sykepengegrunnlag?.sykepengegrunnlag,
            gruppeId = gruppeId(vedtaksperiode),
            fødselsnummer = fødselsnummer,
            hendelseIder = hendelseIder,
            inntektshistorikkBuilder = inntektshistorikkBuilder,
            dataForVilkårsvurdering = vilkårsgrunnlag,
            forkastet = byggerForkastedePerioder
        )
        perioder.add(vedtaksperiodeBuilder)
        pushState(vedtaksperiodeBuilder)
    }

    override fun postVisitPerioder(vedtaksperioder: List<Vedtaksperiode>) {
        popState()
    }

    override fun postVisitForkastedePerioder(vedtaksperioder: List<ForkastetVedtaksperiode>) {
        popState()
    }
}
