package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.util.*

internal fun SchemaBuilder.arbeidsgiverTypes() {
    type<GraphQLGenerasjon> {
        property<List<GraphQLTidslinjeperiode>>("perioderSlice") {
            resolver { generasjon: GraphQLGenerasjon, first: Int?, from: Int? ->
                generasjon.perioder.safeSlice(first, from)
            }
        }
        property<GraphQLTidslinjeperiode?>("periode") {
            resolver { generasjon: GraphQLGenerasjon, index: Int ->
                generasjon.perioder.getOrNull(index)
            }
        }
        property<GraphQLTidslinjeperiode?>("sistePeriode") {
            resolver { generasjon: GraphQLGenerasjon ->
                generasjon.perioder.firstOrNull()
            }
        }
    }

    type<GraphQLArbeidsgiver> {
        property<List<GraphQLGenerasjon>>("generasjonerSlice") {
            resolver { arbeidsgiver: GraphQLArbeidsgiver, first: Int?, from: Int? ->
                arbeidsgiver.generasjoner.safeSlice(first, from)
            }
        }
        property<GraphQLGenerasjon?>("generasjon") {
            resolver { arbeidsgiver: GraphQLArbeidsgiver, index: Int ->
                arbeidsgiver.generasjoner.getOrNull(index)
            }
        }
        property<GraphQLGenerasjon?>("sisteGenerasjon") {
            resolver { arbeidsgiver: GraphQLArbeidsgiver ->
                arbeidsgiver.generasjoner.firstOrNull()
            }
        }
    }
}

data class GraphQLGenerasjon(
    val id: UUID,
    val perioder: List<GraphQLTidslinjeperiode>
)

data class GraphQLArbeidsgiver(
    val organisasjonsnummer: String,
    val id: UUID,
    val generasjoner: List<GraphQLGenerasjon>
)

internal fun <T> List<T>.safeSlice(first: Int?, from: Int?): List<T> {
    if (from != null && from >= size || first == 0) return emptyList()
    val start = (from ?: 0)
    val end = ((start + (first ?: size) - 1)).coerceAtLeast(0).coerceAtMost(size - 1)
    return slice(start..end)
}
