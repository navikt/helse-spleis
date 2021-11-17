package no.nav.helse.spleis.graphql.dto

import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import java.time.LocalDate
import java.util.*

internal fun SchemaBuilder.personTypes() {
    type<GraphQLPerson> {
        property<GraphQLArbeidsgiver?>("arbeidsgiver") {
            resolver { person: GraphQLPerson, organisasjonsnummer: String ->
                person.arbeidsgivere.find { it.organisasjonsnummer == organisasjonsnummer }
            }
        }
        property<List<GraphQLVilkarsgrunnlag>?>("vilkarsgrunnlag") {
            resolver { person: GraphQLPerson, id: String ->
                person.vilkarsgrunnlaghistorikk.find { it.id === UUID.fromString(id) }?.grunnlag
            }
        }
    }
}

data class GraphQLPerson(
    val aktorId: String,
    val fodselsnummer: String,
    val arbeidsgivere: List<GraphQLArbeidsgiver>,
    val inntektsgrunnlag: List<GraphQLInntektsgrunnlag>,
    val vilkarsgrunnlaghistorikk: List<GraphQLVilkarsgrunnlaghistorikk>,
    val dodsdato: LocalDate?,
    val versjon: Int
)
