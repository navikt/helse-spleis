package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.time.YearMonth

internal fun SchemaBuilder.inntektsgrunnlagTypes() {
    enum<GraphQLInntektskilde>()

    type<GraphQLInntekterFraAOrdningen>()
    type<GraphQLOmregnetArsinntekt>()
    type<GraphQLSammenligningsgrunnlag>()
    type<GraphQLArbeidsgiverinntekt>()
}

enum class GraphQLInntektskilde {
    Saksbehandler,
    Inntektsmelding,
    Infotrygd,
    AOrdningen,
    IkkeRapportert
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
    val sammenligningsgrunnlag: GraphQLSammenligningsgrunnlag?,
    val deaktivert: Boolean? = null,
)
