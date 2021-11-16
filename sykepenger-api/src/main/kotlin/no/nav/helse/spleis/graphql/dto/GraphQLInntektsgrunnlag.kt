package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.time.LocalDate
import java.time.YearMonth

internal fun SchemaBuilder.inntektsgrunnlagTypes() {
    enum<GraphQLInntektskilde>()

    type<GraphQLInntekterFraAOrdningen>()
    type<GraphQLOmregnetArsinntekt>()
    type<GraphQLSammenligningsgrunnlag>()
    type<GraphQLArbeidsgiverinntekt>()
    type<GraphQLInntektsgrunnlag>()
}

enum class GraphQLInntektskilde {
    Saksbehandler,
    Inntektsmelding,
    Infotrygd,
    AOrdningen
}

data class GraphQLInntekterFraAOrdningen(
    val maned: YearMonth,
    val sum: Double
)

data class GraphQLOmregnetArsinntekt(
    val kilde: GraphQLInntektskilde,
    val belop: Double,
    val manedsbelop: Double,
    val inntekterFraAOrdningen: List<GraphQLInntekterFraAOrdningen>?
)

data class GraphQLSammenligningsgrunnlag(
    val belop: Double,
    val inntekterFraAOrdningen: List<GraphQLInntekterFraAOrdningen>
)

data class GraphQLArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: GraphQLOmregnetArsinntekt?,
    val sammenligningsgrunnlag: GraphQLSammenligningsgrunnlag?
)

data class GraphQLInntektsgrunnlag(
    val skjaeringstidspunkt: LocalDate,
    val sykepengegrunnlag: Double?,
    val omregnetArsinntekt: Double?,
    val sammenligningsgrunnlag: Double?,
    val avviksprosent: Double?,
    val maksUtbetalingPerDag: Double?,
    val inntekter: List<GraphQLArbeidsgiverinntekt>,
    val oppfyllerKravOmMinstelonn: Boolean?,
    val grunnbelop: Int
) {
}
