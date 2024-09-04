package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate
import java.util.UUID

data class GraphQLGenerasjon(
    val id: UUID,
    val perioder: List<GraphQLTidslinjeperiode>,
    val kildeTilGenerasjon: UUID
)

data class GraphQLGhostPeriode(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val skjaeringstidspunkt: LocalDate,
    val vilkarsgrunnlagId: UUID,
    val deaktivert: Boolean,
    val organisasjonsnummer: String
)

data class GraphQLNyttInntektsforholdPeriode(
    val id: UUID,
    val organisasjonsnummer: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val vilkarsgrunnlagId: UUID
)

data class GraphQLArbeidsgiver(
    val organisasjonsnummer: String,
    val id: UUID,
    val generasjoner: List<GraphQLGenerasjon>,
    val ghostPerioder: List<GraphQLGhostPeriode>,
    val nyeInntektsforholdPerioder: List<GraphQLNyttInntektsforholdPeriode>
)
