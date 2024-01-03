package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

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

data class GraphQLSkjonnsmessigFastsatt(
    val belop: Double,
    val manedsbelop: Double,
)

data class GraphQLOmregnetArsinntekt(
    val kilde: GraphQLInntektskilde,
    val belop: Double,
    val manedsbelop: Double,
    val inntekterFraAOrdningen: List<GraphQLInntekterFraAOrdningen>?
)

data class GraphQLArbeidsgiverinntekt(
    val arbeidsgiver: String,
    val omregnetArsinntekt: GraphQLOmregnetArsinntekt,
    val skjonnsmessigFastsatt: GraphQLSkjonnsmessigFastsatt?, // TODO: speil bruker kun <beløp>, og verdien kunne altså vært foreklet til en <Double?>
    val skjonnsmessigFastsattAarlig: Double?,
    val deaktivert: Boolean? = null,
)

data class GraphQLArbeidsgiverrefusjon(
    val arbeidsgiver: String,
    val refusjonsopplysninger: List<GraphQLRefusjonselement>
)

data class GraphQLRefusjonselement(
    val fom: LocalDate,
    val tom: LocalDate?,
    val belop: Double,
    val meldingsreferanseId: UUID
)
