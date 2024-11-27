package no.nav.helse.dto.serialisering

import no.nav.helse.dto.MedlemskapsvurderingDto
import java.time.LocalDate
import java.util.UUID

sealed class VilkårsgrunnlagUtDto {
    abstract val vilkårsgrunnlagId: UUID
    abstract val skjæringstidspunkt: LocalDate
    abstract val inntektsgrunnlag: InntektsgrunnlagUtDto

    data class Spleis(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val inntektsgrunnlag: InntektsgrunnlagUtDto,
        val opptjening: OpptjeningUtDto,
        val medlemskapstatus: MedlemskapsvurderingDto,
        val vurdertOk: Boolean,
        val meldingsreferanseId: UUID?,
    ) : VilkårsgrunnlagUtDto()

    data class Infotrygd(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val inntektsgrunnlag: InntektsgrunnlagUtDto,
    ) : VilkårsgrunnlagUtDto()
}
