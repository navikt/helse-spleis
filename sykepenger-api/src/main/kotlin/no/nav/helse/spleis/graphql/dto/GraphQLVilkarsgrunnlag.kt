package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.time.LocalDate
import java.util.*

internal fun SchemaBuilder.vilkarsgrunnlagTypes() {
    enum<GraphQLVilkarsgrunnlagtype>()

    type<GraphQLVilkarsgrunnlag>()
    type<GraphQLSpleisVilkarsgrunnlag>()
    type<GraphQLInfotrygdVilkarsgrunnlag>()
    type<GraphQLVilkarsgrunnlaghistorikk>()
}

enum class GraphQLVilkarsgrunnlagtype {
    Infotrygd,
    Spleis,
    Ukjent
}

interface GraphQLVilkarsgrunnlag {
    val skjaeringstidspunkt: LocalDate
    val omregnetArsinntekt: Double
    val sammenligningsgrunnlag: Double?
    val sykepengegrunnlag: Double
    val inntekter: List<GraphQLArbeidsgiverinntekt>
    val vilkarsgrunnlagtype: GraphQLVilkarsgrunnlagtype
}

data class GraphQLSpleisVilkarsgrunnlag(
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<GraphQLArbeidsgiverinntekt>,
    val avviksprosent: Double?,
    val grunnbelop: Int,
    val antallOpptjeningsdagerErMinst: Int,
    val opptjeningFra: LocalDate,
    val oppfyllerKravOmMinstelonn: Boolean,
    val oppfyllerKravOmOpptjening: Boolean,
    val oppfyllerKravOmMedlemskap: Boolean?
) : GraphQLVilkarsgrunnlag {
    override val vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.Spleis
}

data class GraphQLInfotrygdVilkarsgrunnlag(
    override val skjaeringstidspunkt: LocalDate,
    override val omregnetArsinntekt: Double,
    override val sammenligningsgrunnlag: Double?,
    override val sykepengegrunnlag: Double,
    override val inntekter: List<GraphQLArbeidsgiverinntekt>
) : GraphQLVilkarsgrunnlag {
    override val vilkarsgrunnlagtype = GraphQLVilkarsgrunnlagtype.Infotrygd
}

data class GraphQLVilkarsgrunnlaghistorikk(
    val id: UUID,
    val grunnlag: List<GraphQLVilkarsgrunnlag>
)
