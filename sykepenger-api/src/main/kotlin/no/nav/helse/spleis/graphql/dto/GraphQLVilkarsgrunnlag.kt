package no.nav.helse.spleis.graphql.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename")
interface GraphQLVilkarsgrunnlag {
    val id: UUID
    val skjaeringstidspunkt: LocalDate
    val omregnetArsinntekt: Double
    val sykepengegrunnlag: Double
    val inntekter: List<GraphQLArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>
}

data class GraphQLSpleisVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<GraphQLArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    val skjonnsmessigFastsattAarlig: Double?,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: GraphQLSykepengegrunnlagsgrense,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : GraphQLVilkarsgrunnlag

data class GraphQLInfotrygdVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    override val inntekter: List<GraphQLArbeidsgiverinntekt>
) : GraphQLVilkarsgrunnlag

data class GraphQLVilkarsgrunnlaghistorikk(
    val id: UUID,
    val grunnlag: List<GraphQLVilkarsgrunnlag>
)

data class GraphQLSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
)
