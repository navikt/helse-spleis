package no.nav.helse.serde.api.builders

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.helse.hendelser.Periode
import no.nav.helse.person.Arbeidsgiver
import no.nav.helse.person.Dokumentsporing
import no.nav.helse.person.ForkastetVedtaksperiode
import no.nav.helse.person.ForlengelseFraInfotrygd
import no.nav.helse.person.Inntektskilde
import no.nav.helse.person.InntektsmeldingInfo
import no.nav.helse.person.Periodetype
import no.nav.helse.person.Vedtaksperiode
import no.nav.helse.person.VilkårsgrunnlagHistorikk
import no.nav.helse.serde.api.dto.UtbetalingshistorikkElementDTO
import no.nav.helse.serde.api.v2.HendelseDTO

internal class VedtaksperioderBuilder(
    private val arbeidsgiver: Arbeidsgiver,
    private val fødselsnummer: String,
    private val vilkårsgrunnlagInntektBuilder: VilkårsgrunnlagInntektBuilder,
    private val gruppeIder: MutableMap<Vedtaksperiode, UUID>,
    private val vilkårsgrunnlagHistorikk: VilkårsgrunnlagHistorikk,
    private val byggerForkastedePerioder: Boolean = false
) : BuilderState() {
    private val perioder = mutableListOf<VedtaksperiodeBuilder>()

    fun build(hendelser: List<HendelseDTO>, utbetalinger: List<UtbetalingshistorikkElementDTO>) =
        perioder.map { it.build(hendelser, utbetalinger) }

    private fun gruppeId(vedtaksperiode: Vedtaksperiode): UUID {
        val gruppeId = arbeidsgiver.finnVedtaksperiodeRettFør(vedtaksperiode)?.let(gruppeIder::getValue) ?: UUID.randomUUID()
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
        periodetype: () -> Periodetype,
        skjæringstidspunkt: () -> LocalDate,
        skjæringstidspunktFraInfotrygd: LocalDate?,
        forlengelseFraInfotrygd: ForlengelseFraInfotrygd,
        hendelseIder: Set<Dokumentsporing>,
        inntektsmeldingInfo: InntektsmeldingInfo?,
        inntektskilde: Inntektskilde
    ) {
        val vilkårsgrunnlag = vilkårsgrunnlagHistorikk.vilkårsgrunnlagFor(skjæringstidspunkt()) as? VilkårsgrunnlagHistorikk.Grunnlagsdata
        val sykepengegrunnlag = vilkårsgrunnlag?.sykepengegrunnlag()

        val vedtaksperiodeBuilder = VedtaksperiodeBuilder(
            vedtaksperiode = vedtaksperiode,
            id = id,
            periode = periode,
            skjæringstidspunkt = skjæringstidspunkt(),
            periodetype = periodetype(),
            forlengelseFraInfotrygd = forlengelseFraInfotrygd,
            tilstand = tilstand,
            inntektskilde = inntektskilde,
            sykepengegrunnlag = sykepengegrunnlag?.sykepengegrunnlag,
            gruppeId = gruppeId(vedtaksperiode),
            fødselsnummer = fødselsnummer,
            hendelseIder = hendelseIder,
            vilkårsgrunnlagInntektBuilder = vilkårsgrunnlagInntektBuilder,
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
