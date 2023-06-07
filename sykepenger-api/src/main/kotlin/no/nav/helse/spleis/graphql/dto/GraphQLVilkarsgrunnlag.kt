package no.nav.helse.spleis.graphql.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID

enum class GraphQLVilkarsgrunnlagtype {
    Infotrygd,
    Spleis,
    Ukjent
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename")
interface GraphQLVilkarsgrunnlag {
    val id: UUID
    val skjaeringstidspunkt: LocalDate
    val omregnetArsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<GraphQLArbeidsgiverinntekt>
    val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>
    val vilkarsgrunnlagtype: GraphQLVilkarsgrunnlagtype
}

data class GraphQLSpleisVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double, // TODO: det heter 'beregningsgrunnlag'. Omregnet årsinntekt er noe annet etter skjønnsmessig fastsetting
    override val sammenligningsgrunnlag: Double?,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<GraphQLArbeidsgiverinntekt>,
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    val avviksprosent: Double?,
    val grunnbelop: Int,
    val sykepengegrunnlagsgrense: GraphQLSykepengegrunnlagsgrense,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : GraphQLVilkarsgrunnlag {
    override val vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.Spleis
}

data class GraphQLInfotrygdVilkarsgrunnlag(
    override val id: UUID,
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val sykepengegrunnlag: Double,
    override val arbeidsgiverrefusjoner: List<GraphQLArbeidsgiverrefusjon>,
    override val inntekter: List<GraphQLArbeidsgiverinntekt>
) : GraphQLVilkarsgrunnlag {
    override val vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.Infotrygd
}

data class GraphQLVilkarsgrunnlaghistorikk(
    val id: UUID,
    val grunnlag: List<GraphQLVilkarsgrunnlag>
)

data class GraphQLSykepengegrunnlagsgrense(
    val grunnbelop: Int,
    val grense: Int,
    val virkningstidspunkt: LocalDate,
)
