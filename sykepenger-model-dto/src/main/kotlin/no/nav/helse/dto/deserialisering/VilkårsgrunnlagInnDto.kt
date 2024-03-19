package no.nav.helse.dto.deserialisering

import java.time.LocalDate
import java.util.UUID
import no.nav.helse.dto.MedlemskapsvurderingDto
import no.nav.helse.dto.serialisering.OpptjeningUtDto

sealed class VilkårsgrunnlagInnDto {
    abstract val vilkårsgrunnlagId: UUID
    abstract val skjæringstidspunkt: LocalDate
    abstract val sykepengegrunnlag: SykepengegrunnlagInnDto

    data class Spleis(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagInnDto,
        val opptjening: OpptjeningInnDto,
        val medlemskapstatus: MedlemskapsvurderingDto,
        val vurdertOk: Boolean,
        val meldingsreferanseId: UUID?
    ) : VilkårsgrunnlagInnDto()
    data class Infotrygd(
        override val vilkårsgrunnlagId: UUID,
        override val skjæringstidspunkt: LocalDate,
        override val sykepengegrunnlag: SykepengegrunnlagInnDto
    ) : VilkårsgrunnlagInnDto()
}