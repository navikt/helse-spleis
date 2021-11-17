package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.util.*

internal fun SchemaBuilder.arbeidsgiverTypes() {
    type<GraphQLGenerasjon> {
        property<List<GraphQLTidslinjeperiode>>("perioder") {
            resolver { generasjon: GraphQLGenerasjon, first: Int?, after: Int? ->
                val max = generasjon.perioder.size
                val start = (after ?: 0).coerceAtMost(max)
                val end = (first ?: max).coerceAtLeast(0).coerceAtMost(max)
                generasjon.perioder.subList(start, end)
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
