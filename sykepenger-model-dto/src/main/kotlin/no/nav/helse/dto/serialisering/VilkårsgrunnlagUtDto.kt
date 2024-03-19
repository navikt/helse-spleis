package no.nav.helse.dto.serialisering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.MedlemskapsvurderingDto

sealed class VilkårsgrunnlagUtDto {
    abstract val vilkårsgrunnlagId: UUID
    abstract val skjæringstidspunkt: LocalDate
    abstract val sykepengegrunnlag: SykepengegrunnlagUtDto

    data class Spleis(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagUtDto,
        val opptjening: OpptjeningUtDto,
        val medlemskapstatus: MedlemskapsvurderingDto,
        val vurdertOk: Boolean,
        val meldingsreferanseId: UUID?
    ) : VilkårsgrunnlagUtDto()
    data class Infotrygd(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagUtDto
    ) : VilkårsgrunnlagUtDto()
}