package no.nav.helse.spleis.graphql.dto

import java.time.LocalDate
import java.util.*

data class GraphQLGenerasjon(
    val id: UUID,
    val perioder: List<GraphQLTidslinjeperiode>
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

data class GraphQLArbeidsgiver(
    val organisasjonsnummer: String,
    val id: UUID,
    val generasjoner: List<GraphQLGenerasjon>,
    val ghostPerioder: List<GraphQLGhostPeriode>
)

internal fun <T> List<T>.safeSlice(first: Int?, from: Int?): List<T> {
    if (from != null && from >= size || first == 0) return emptyList()
    val start = (from ?: 0)
    val end = ((start + (first ?: size) - 1)).coerceAtLeast(0).coerceAtMost(size - 1)
    return slice(start..end)
}
