package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.util.*

internal fun SchemaBuilder.arbeidsgiverTypes() {
    type<GraphQLGenerasjon> {
        property<List<GraphQLTidslinjeperiode>>("perioder") {
            resolver { generasjon: GraphQLGenerasjon, from: Int?, to: Int? ->
                if (from == null || to == null || from >= to) {
                    generasjon.perioder
                } else {
                    generasjon.perioder.subList(from.coerceAtLeast(0), to.coerceAtMost(generasjon.perioder.size))
                }
            }
        }
    }

    type<GraphQLArbeidsgiver> {
        property<GraphQLGenerasjon?>("generasjon") {
            resolver { arbeidsgiver: GraphQLArbeidsgiver, index: Int ->
                arbeidsgiver.generasjoner.getOrNull(index)
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
